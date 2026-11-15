package com.nezuplay.video.data.models

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/mp4",
    val folderPath: String = "",
    val folderName: String = "",
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L
) {
    val formattedDuration: String
        get() = formatDuration(durationMs)

    val formattedSize: String
        get() = formatFileSize(sizeBytes)

    val resolutionLabel: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width > 0 && height > 0 -> "${width}x${height}"
            else -> ""
        }

    val progressFraction: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return "00:00"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = 0
            var b = bytes.toDouble()
            while (b >= 1024 && digitGroups < units.size - 1) {
                b /= 1024
                digitGroups++
            }
            return String.format("%.1f %s", b, units[digitGroups])
        }
    }
}
