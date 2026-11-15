package com.nezuplay.video.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.nezuplay.video.data.database.NezuPlayDatabase
import com.nezuplay.video.data.database.VideoMetadataEntity
import com.nezuplay.video.data.models.FilterOption
import com.nezuplay.video.data.models.FolderItem
import com.nezuplay.video.data.models.SortOption
import com.nezuplay.video.data.models.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class VideoRepository(private val context: Context) {
    private val database = NezuPlayDatabase.getInstance(context)
    private val metadataDao = database.videoMetadataDao()
    private val preferencesDao = database.userPreferencesDao()

    private val _allVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val allVideos: StateFlow<List<VideoItem>> = _allVideos.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val supportedExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts", "m4v"
    )

    private val vidmateKeywords = listOf("vidmate", "download/vidmate", "movies/vidmate")

    suspend fun refreshVideos() {
        withContext(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val mediaStoreVideos = queryMediaStore()
                val directFileVideos = queryCommonDirectories()
                val safVideos = querySafFolders()

                // Merge and deduplicate by canonical path or URI string
                val combinedMap = mutableMapOf<String, VideoItem>()
                for (v in mediaStoreVideos) {
                    val key = v.folderPath + "/" + v.title
                    combinedMap[key] = v
                }
                for (v in directFileVideos) {
                    val key = v.folderPath + "/" + v.title
                    if (!combinedMap.containsKey(key)) {
                        combinedMap[key] = v
                    }
                }
                for (v in safVideos) {
                    val key = v.folderPath + "/" + v.title
                    if (!combinedMap.containsKey(key)) {
                        combinedMap[key] = v
                    }
                }

                // Attach Room metadata (isFavorite, lastPositionMs, customTitle, playCount)
                val finalVideos = combinedMap.values.map { video ->
                    val meta = metadataDao.getMetadata(video.uri.toString())
                    if (meta != null) {
                        video.copy(
                            title = meta.customTitle ?: video.title,
                            isFavorite = meta.isFavorite,
                            lastPositionMs = meta.lastPositionMs,
                            lastPlayedTimestamp = meta.lastPlayedTimestamp,
                            playCount = meta.playCount
                        )
                    } else {
                        video
                    }
                }

                // Group by folder
                val folderGroups = finalVideos.groupBy { it.folderPath.ifEmpty { "Default" } }
                val folderList = folderGroups.map { (path, videos) ->
                    val folderName = videos.firstOrNull()?.folderName?.ifEmpty {
                        File(path).name.ifEmpty { "Videos" }
                    } ?: "Videos"
                    val isVidMate = vidmateKeywords.any { path.lowercase(Locale.ROOT).contains(it) } ||
                            folderName.lowercase(Locale.ROOT).contains("vidmate")
                    FolderItem(
                        path = path,
                        name = folderName,
                        videoCount = videos.size,
                        totalSizeBytes = videos.sumOf { it.sizeBytes },
                        isVidMate = isVidMate
                    )
                }.sortedWith(compareByDescending<FolderItem> { it.isVidMate }.thenBy { it.name.lowercase(Locale.ROOT) })

                _allVideos.value = finalVideos
                _folders.value = folderList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun queryMediaStore(): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val titleColumn = c.getColumnIndex(MediaStore.Video.Media.TITLE)
            val durationColumn = c.getColumnIndex(MediaStore.Video.Media.DURATION)
            val sizeColumn = c.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateColumn = c.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val widthColumn = c.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn = c.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val mimeColumn = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val dataColumn = c.getColumnIndex(MediaStore.Video.Media.DATA)
            val bucketColumn = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val rawName = if (nameColumn != -1) c.getString(nameColumn) ?: "" else ""
                val rawTitle = if (titleColumn != -1) c.getString(titleColumn) ?: "" else ""
                val displayName = rawName.ifEmpty { rawTitle.ifEmpty { "Video_$id" } }
                val duration = if (durationColumn != -1) c.getLong(durationColumn) else 0L
                val size = if (sizeColumn != -1) c.getLong(sizeColumn) else 0L
                val dateAdded = if (dateColumn != -1) c.getLong(dateColumn) else 0L
                val width = if (widthColumn != -1) c.getInt(widthColumn) else 0
                val height = if (heightColumn != -1) c.getInt(heightColumn) else 0
                val mimeType = if (mimeColumn != -1) c.getString(mimeColumn) ?: "video/mp4" else "video/mp4"
                val filePath = if (dataColumn != -1) c.getString(dataColumn) ?: "" else ""
                val bucketName = if (bucketColumn != -1) c.getString(bucketColumn) ?: "" else ""

                val folderPath = if (filePath.isNotEmpty()) {
                    File(filePath).parent ?: ""
                } else {
                    bucketName
                }

                val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext.isEmpty() || ext in supportedExtensions) {
                    list.add(
                        VideoItem(
                            id = id,
                            uri = contentUri,
                            title = displayName,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            folderPath = folderPath,
                            folderName = bucketName.ifEmpty { File(folderPath).name }
                        )
                    )
                }
            }
        }
        return list
    }

    private fun queryCommonDirectories(): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        val externalStorage = Environment.getExternalStorageDirectory() ?: return list

        val candidateDirs = listOf(
            File(externalStorage, "Download"),
            File(externalStorage, "Download/VidMate"),
            File(externalStorage, "Download/vidmate"),
            File(externalStorage, "VidMate"),
            File(externalStorage, "VidMate/download"),
            File(externalStorage, "Movies"),
            File(externalStorage, "Movies/VidMate"),
            File(externalStorage, "DCIM/Camera")
        )

        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                scanDirectoryRecursively(dir, list, maxDepth = 2)
            }
        }
        return list
    }

    private fun scanDirectoryRecursively(dir: File, outList: MutableList<VideoItem>, maxDepth: Int) {
        if (maxDepth < 0) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory && !f.name.startsWith(".")) {
                scanDirectoryRecursively(f, outList, maxDepth - 1)
            } else if (f.isFile) {
                val ext = f.extension.lowercase(Locale.ROOT)
                if (ext in supportedExtensions && f.length() > 0) {
                    val uri = Uri.fromFile(f)
                    outList.add(
                        VideoItem(
                            id = f.hashCode().toLong(),
                            uri = uri,
                            title = f.name,
                            durationMs = 0L, // will be discovered by player or media metadata retriever if needed
                            sizeBytes = f.length(),
                            dateAdded = f.lastModified() / 1000,
                            folderPath = f.parent ?: "",
                            folderName = f.parentFile?.name ?: "Unknown"
                        )
                    )
                }
            }
        }
    }

    private fun querySafFolders(): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        try {
            val persistedUriStrings = context.contentResolver.persistedUriPermissions.map { it.uri.toString() }
            for (uriStr in persistedUriStrings) {
                val treeUri = Uri.parse(uriStr)
                val docFile = DocumentFile.fromTreeUri(context, treeUri)
                if (docFile != null && docFile.isDirectory) {
                    scanDocumentFileTree(docFile, list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun scanDocumentFileTree(dir: DocumentFile, outList: MutableList<VideoItem>) {
        val files = dir.listFiles()
        for (doc in files) {
            if (doc.isDirectory) {
                scanDocumentFileTree(doc, outList)
            } else if (doc.isFile) {
                val name = doc.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext in supportedExtensions) {
                    outList.add(
                        VideoItem(
                            id = doc.uri.hashCode().toLong(),
                            uri = doc.uri,
                            title = name,
                            durationMs = 0L,
                            sizeBytes = doc.length(),
                            dateAdded = doc.lastModified() / 1000,
                            folderPath = dir.name ?: "SAF Folder",
                            folderName = dir.name ?: "SAF Folder"
                        )
                    )
                }
            }
        }
    }

    suspend fun toggleFavorite(video: VideoItem) {
        val newFav = !video.isFavorite
        metadataDao.insertOrUpdate(
            VideoMetadataEntity(
                uriString = video.uri.toString(),
                isFavorite = newFav,
                customTitle = null,
                lastPositionMs = video.lastPositionMs,
                durationMs = video.durationMs,
                lastPlayedTimestamp = video.lastPlayedTimestamp,
                playCount = video.playCount
            )
        )
        _allVideos.value = _allVideos.value.map {
            if (it.uri == video.uri) it.copy(isFavorite = newFav) else it
        }
    }

    suspend fun savePlaybackPosition(uri: Uri, positionMs: Long, durationMs: Long) {
        val uriStr = uri.toString()
        val currentMeta = metadataDao.getMetadata(uriStr)
        val now = System.currentTimeMillis()
        val updatedMeta = if (currentMeta != null) {
            currentMeta.copy(
                lastPositionMs = positionMs,
                durationMs = durationMs,
                lastPlayedTimestamp = now,
                playCount = currentMeta.playCount + 1
            )
        } else {
            VideoMetadataEntity(
                uriString = uriStr,
                lastPositionMs = positionMs,
                durationMs = durationMs,
                lastPlayedTimestamp = now,
                playCount = 1
            )
        }
        metadataDao.insertOrUpdate(updatedMeta)
        _allVideos.value = _allVideos.value.map {
            if (it.uri == uri) it.copy(
                lastPositionMs = positionMs,
                durationMs = durationMs,
                lastPlayedTimestamp = now,
                playCount = updatedMeta.playCount
            ) else it
        }
    }

    suspend fun clearHistory() {
        metadataDao.clearHistory()
        _allVideos.value = _allVideos.value.map {
            it.copy(lastPositionMs = 0L, lastPlayedTimestamp = 0L)
        }
    }

    suspend fun renameVideo(video: VideoItem, newTitle: String) {
        val currentMeta = metadataDao.getMetadata(video.uri.toString())
        val updated = currentMeta?.copy(customTitle = newTitle) ?: VideoMetadataEntity(
            uriString = video.uri.toString(),
            customTitle = newTitle
        )
        metadataDao.insertOrUpdate(updated)
        _allVideos.value = _allVideos.value.map {
            if (it.uri == video.uri) it.copy(title = newTitle) else it
        }
    }

    suspend fun deleteVideo(video: VideoItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                metadataDao.deleteMetadata(video.uri.toString())
                var deleted = false
                if (video.uri.scheme == "file") {
                    val file = File(video.uri.path ?: "")
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                } else if (video.uri.scheme == "content") {
                    try {
                        val count = context.contentResolver.delete(video.uri, null, null)
                        deleted = count > 0
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _allVideos.value = _allVideos.value.filter { it.uri != video.uri }
                deleted
            } catch (e: Exception) {
                false
            }
        }
    }
}
