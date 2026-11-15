package com.nezuplay.video

import android.net.Uri
import com.nezuplay.video.data.models.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoItemTest {

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", VideoItem.formatDuration(0L))
        assertEquals("00:45", VideoItem.formatDuration(45000L))
        assertEquals("02:15", VideoItem.formatDuration(135000L))
        assertEquals("1:01:05", VideoItem.formatDuration(3665000L))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", VideoItem.formatFileSize(0L))
        assertEquals("1.0 KB", VideoItem.formatFileSize(1024L))
        assertEquals("25.0 MB", VideoItem.formatFileSize(25 * 1024 * 1024L))
        assertEquals("1.5 GB", VideoItem.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }
}
