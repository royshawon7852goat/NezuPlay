package com.nezuplay.video.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_metadata")
data class VideoMetadataEntity(
    @PrimaryKey val uriString: String,
    val isFavorite: Boolean = false,
    val customTitle: String? = null,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedTimestamp: Long = 0L,
    val playCount: Int = 0
)

@Entity(tableName = "private_videos")
data class PrivateVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val originalFileName: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val encryptedFileName: String,
    val ivBase64: String,
    val saltBase64: String,
    val importTimestamp: Long,
    val mimeType: String = "video/mp4",
    val thumbnailEncryptedName: String? = null
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
