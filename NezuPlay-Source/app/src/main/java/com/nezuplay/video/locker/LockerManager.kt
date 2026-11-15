package com.nezuplay.video.locker

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.nezuplay.video.data.database.NezuPlayDatabase
import com.nezuplay.video.data.database.PrivateVideoEntity
import com.nezuplay.video.data.database.UserPreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class LockerAuthState {
    SETUP_REQUIRED,
    LOCKED,
    UNLOCKED
}

class LockerManager(private val context: Context) {
    private val database = NezuPlayDatabase.getInstance(context)
    private val privateVideoDao = database.privateVideoDao()
    private val preferencesDao = database.userPreferencesDao()
    private val encryptionManager = EncryptionManager()

    private val _authState = MutableStateFlow(LockerAuthState.LOCKED)
    val authState: StateFlow<LockerAuthState> = _authState.asStateFlow()

    private val _privateVideos = MutableStateFlow<List<PrivateVideoEntity>>(emptyList())
    val privateVideos: StateFlow<List<PrivateVideoEntity>> = _privateVideos.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutUntilMs = MutableStateFlow(0L)
    val lockoutUntilMs: StateFlow<Long> = _lockoutUntilMs.asStateFlow()

    private val vaultDir: File
        get() = File(context.filesDir, "locker_vault").apply { if (!exists()) mkdirs() }

    private val ephemeralDir: File
        get() = File(context.cacheDir, "ephemeral_vault").apply { if (!exists()) mkdirs() }

    init {
        // Ephemeral directory must be clean on initialization
        clearEphemeralFiles()
    }

    suspend fun checkInitialState() {
        withContext(Dispatchers.IO) {
            val pinHash = preferencesDao.getValue(KEY_PIN_HASH)
            if (pinHash.isNullOrEmpty()) {
                _authState.value = LockerAuthState.SETUP_REQUIRED
            } else {
                _authState.value = LockerAuthState.LOCKED
            }
        }
    }

    suspend fun setupPin(pin: String, enableBiometric: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            if (pin.length < 4) return@withContext false
            val salt = encryptionManager.generateSalt()
            val hash = encryptionManager.hashPin(pin, salt)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

            preferencesDao.setValue(UserPreferenceEntity(KEY_PIN_HASH, hashBase64))
            preferencesDao.setValue(UserPreferenceEntity(KEY_PIN_SALT, saltBase64))
            preferencesDao.setValue(UserPreferenceEntity(KEY_BIOMETRIC_ENABLED, enableBiometric.toString()))

            _authState.value = LockerAuthState.UNLOCKED
            _failedAttempts.value = 0
            loadPrivateVideos()
            true
        }
    }

    suspend fun unlockWithPin(pin: String): Boolean {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (now < _lockoutUntilMs.value) {
                return@withContext false
            }

            val storedHash = preferencesDao.getValue(KEY_PIN_HASH) ?: return@withContext false
            val storedSalt = preferencesDao.getValue(KEY_PIN_SALT) ?: return@withContext false

            val isValid = encryptionManager.verifyPin(pin, storedHash, storedSalt)
            if (isValid) {
                _authState.value = LockerAuthState.UNLOCKED
                _failedAttempts.value = 0
                loadPrivateVideos()
                true
            } else {
                val attempts = _failedAttempts.value + 1
                _failedAttempts.value = attempts
                if (attempts >= 5) {
                    _lockoutUntilMs.value = System.currentTimeMillis() + (30 * 1000)
                }
                false
            }
        }
    }

    suspend fun unlockWithBiometric(): Boolean {
        return withContext(Dispatchers.IO) {
            _authState.value = LockerAuthState.UNLOCKED
            _failedAttempts.value = 0
            loadPrivateVideos()
            true
        }
    }

    suspend fun isBiometricEnabled(): Boolean {
        return withContext(Dispatchers.IO) {
            preferencesDao.getValue(KEY_BIOMETRIC_ENABLED) == "true"
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesDao.setValue(UserPreferenceEntity(KEY_BIOMETRIC_ENABLED, enabled.toString()))
        }
    }

    fun lock() {
        _authState.value = LockerAuthState.LOCKED
        clearEphemeralFiles()
    }

    suspend fun loadPrivateVideos() {
        withContext(Dispatchers.IO) {
            privateVideoDao.getAllPrivateVideosFlow().collect { list ->
                _privateVideos.value = list
            }
        }
    }

    suspend fun importVideo(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            _isImporting.value = true
            _importProgress.value = 0f
            try {
                // Determine file name and size
                var displayName = "Video_${System.currentTimeMillis()}"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                // Extract duration if possible
                var durationMs = 0L
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = durStr?.toLongOrNull() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val encFileName = "enc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.bin"
                val encFile = File(vaultDir, encFileName)

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext false

                val totalExpectedSize = if (fileSize > 0) fileSize else 1L
                val ivBase64 = inputStream.use { stream ->
                    encryptionManager.encryptStreamToFile(stream, encFile) { bytesWritten ->
                        _importProgress.value = (bytesWritten.toFloat() / totalExpectedSize.toFloat()).coerceIn(0f, 0.99f)
                    }
                }

                // Verify file exists and has size
                if (!encFile.exists() || encFile.length() <= 12) {
                    encFile.delete()
                    return@withContext false
                }

                val entity = PrivateVideoEntity(
                    originalFileName = displayName,
                    title = displayName.substringBeforeLast('.'),
                    durationMs = durationMs,
                    sizeBytes = encFile.length(),
                    encryptedFileName = encFileName,
                    ivBase64 = ivBase64,
                    saltBase64 = "",
                    importTimestamp = System.currentTimeMillis(),
                    mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                )

                privateVideoDao.insertPrivateVideo(entity)
                _importProgress.value = 1f
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _isImporting.value = false
            }
        }
    }

    suspend fun prepareEphemeralPlayback(privateVideo: PrivateVideoEntity): File? {
        return withContext(Dispatchers.IO) {
            try {
                clearEphemeralFiles()
                val encFile = File(vaultDir, privateVideo.encryptedFileName)
                if (!encFile.exists()) return@withContext null

                val ext = privateVideo.originalFileName.substringAfterLast('.', "mp4")
                val tempFile = File(ephemeralDir, "ephemeral_${privateVideo.id}.$ext")
                tempFile.deleteOnExit()

                val success = encryptionManager.decryptFileToEphemeralFile(encFile, tempFile)
                if (success && tempFile.exists() && tempFile.length() > 0) {
                    tempFile
                } else {
                    tempFile.delete()
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun clearEphemeralFiles() {
        try {
            ephemeralDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePrivateVideo(video: PrivateVideoEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val encFile = File(vaultDir, video.encryptedFileName)
                if (encFile.exists()) encFile.delete()
                privateVideoDao.deletePrivateVideo(video)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun renamePrivateVideo(video: PrivateVideoEntity, newTitle: String) {
        withContext(Dispatchers.IO) {
            privateVideoDao.updatePrivateVideo(video.copy(title = newTitle))
        }
    }

    companion object {
        private const val KEY_PIN_HASH = "private_locker_pin_hash"
        private const val KEY_PIN_SALT = "private_locker_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "private_locker_biometric_enabled"
        const val KEY_AUTO_LOCK_TIMEOUT = "private_locker_auto_lock_timeout"
    }
}
