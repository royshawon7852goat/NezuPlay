package com.nezuplay.video.locker

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptionManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "NezuPlayMasterLockerKey"
        private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PBKDF2_ITERATIONS = 12000
        private const val PBKDF2_KEY_LENGTH = 256
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val parameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Generates a 16-byte random salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Computes a PBKDF2 hash of the PIN with the provided salt.
     */
    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * Verifies user PIN against the stored hash and salt in constant time.
     */
    fun verifyPin(pin: String, storedHashBase64: String, saltBase64: String): Boolean {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val expectedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
            val computedHash = hashPin(pin, salt)
            MessageDigest.isEqual(expectedHash, computedHash)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encrypts a stream into a destination file using AES-256-GCM with a unique 12-byte IV.
     * The file format starts with [12-byte IV] followed by the GCM ciphertext and 16-byte auth tag.
     */
    fun encryptStreamToFile(
        inputStream: InputStream,
        destinationFile: File,
        onProgress: (bytesWritten: Long) -> Unit = {}
    ): String {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey(), gcmSpec)

        FileOutputStream(destinationFile).use { fos ->
            // Write the IV at the beginning of the file
            fos.write(iv)

            CipherOutputStream(fos, cipher).use { cos ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesWritten = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    cos.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                    onProgress(totalBytesWritten)
                }
                cos.flush()
            }
        }

        return Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    /**
     * Decrypts an encrypted file into a temporary output file for secure local playback.
     */
    fun decryptFileToEphemeralFile(encryptedFile: File, destinationFile: File): Boolean {
        return try {
            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH_BYTES)
                val readIv = fis.read(iv)
                if (readIv != GCM_IV_LENGTH_BYTES) return false

                val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), gcmSpec)

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(destinationFile).use { fos ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        while (cis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        fos.flush()
                    }
                }
            }
            true
        } catch (e: Exception) {
            destinationFile.delete()
            false
        }
    }
}
