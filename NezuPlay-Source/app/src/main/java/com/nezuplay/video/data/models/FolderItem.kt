package com.nezuplay.video.data.models

data class FolderItem(
    val path: String,
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val isVidMate: Boolean = false
) {
    val formattedTotalSize: String
        get() = VideoItem.formatFileSize(totalSizeBytes)
}
