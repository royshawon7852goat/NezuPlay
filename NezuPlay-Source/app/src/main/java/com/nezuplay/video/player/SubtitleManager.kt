package com.nezuplay.video.player

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val isExternal: Boolean = false,
    val uri: Uri? = null
)

data class SubtitleStyle(
    val textSizeSp: Float = 16f,
    val textColor: Color = Color.White,
    val backgroundColor: Color = Color(0x99000000),
    val delayMs: Long = 0L
)

class SubtitleManager(private val context: Context) {

    fun createSubtitleConfiguration(uri: Uri, label: String = "External Subtitle"): MediaItem.SubtitleConfiguration {
        val extension = uri.toString().substringAfterLast('.', "").lowercase()
        val mimeType = when (extension) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ssa", "ass" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP // default srt
        }

        return MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .setLabel(label)
            .build()
    }

    fun findMatchingExternalSubtitles(videoUri: Uri): List<Uri> {
        val results = mutableListOf<Uri>()
        if (videoUri.scheme == "file") {
            val videoPath = videoUri.path ?: return results
            val dotIndex = videoPath.lastIndexOf('.')
            if (dotIndex != -1) {
                val basePath = videoPath.substring(0, dotIndex)
                val extensions = listOf(".srt", ".vtt", ".ssa", ".ass")
                for (ext in extensions) {
                    val subFile = java.io.File(basePath + ext)
                    if (subFile.exists() && subFile.canRead()) {
                        results.add(Uri.fromFile(subFile))
                    }
                }
            }
        }
        return results
    }
}
