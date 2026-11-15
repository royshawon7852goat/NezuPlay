package com.nezuplay.video.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.nezuplay.video.data.models.AspectRatioMode
import com.nezuplay.video.data.models.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val sampleRate: Int,
    val channelCount: Int,
    val isSelected: Boolean
)

class VideoPlayerManager(
    private val context: Context,
    private val onPlaybackProgress: (uri: Uri, positionMs: Long, durationMs: Long) -> Unit = { _, _, _ -> }
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackingJob: Job? = null
    private var sleepTimerJob: Job? = null

    val trackSelector = DefaultTrackSelector(context)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setSeekBackIncrementMs(10000)
        .setSeekForwardIncrementMs(10000)
        .build()

    private val subtitleManager = SubtitleManager(context)

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _playlist = MutableStateFlow<List<VideoItem>>(emptyList())
    val playlist: StateFlow<List<VideoItem>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrackInfo>> = _subtitleTracks.asStateFlow()

    private val _selectedSubtitleIndex = MutableStateFlow(-1)
    val selectedSubtitleIndex: StateFlow<Int> = _selectedSubtitleIndex.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _seekDurationSeconds = MutableStateFlow(10)
    val seekDurationSeconds: StateFlow<Int> = _seekDurationSeconds.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracking()
            } else {
                stopProgressTracking()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                    _bufferedPositionMs.value = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                }
                Player.STATE_ENDED -> {
                    // Check if next video in playlist exists
                    if (_currentIndex.value < _playlist.value.size - 1) {
                        playNext()
                    }
                }
                else -> Unit
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            extractTracks(tracks)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    fun setPlaylist(videos: List<VideoItem>, startIndex: Int, startPositionMs: Long? = null) {
        if (videos.isEmpty() || startIndex !in videos.indices) return
        _playlist.value = videos
        _currentIndex.value = startIndex
        playCurrentVideo(startPositionMs)
    }

    private fun playCurrentVideo(startPositionMs: Long? = null) {
        val video = _playlist.value.getOrNull(_currentIndex.value) ?: return
        _currentVideo.value = video

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(video.uri)
            .setMediaId(video.id.toString())

        // Check for local external subtitles
        val autoSubs = subtitleManager.findMatchingExternalSubtitles(video.uri)
        if (autoSubs.isNotEmpty()) {
            val subConfigs = autoSubs.map { subUri ->
                subtitleManager.createSubtitleConfiguration(subUri, subUri.lastPathSegment ?: "Subtitle")
            }
            mediaItemBuilder.setSubtitleConfigurations(subConfigs)
        }

        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        val resumePos = startPositionMs ?: if (video.lastPositionMs > 5000 && video.lastPositionMs < video.durationMs - 5000) {
            video.lastPositionMs
        } else {
            0L
        }

        if (resumePos > 0) {
            exoPlayer.seekTo(resumePos)
        }
        exoPlayer.playWhenReady = true
    }

    fun playNext() {
        if (_currentIndex.value < _playlist.value.size - 1) {
            _currentIndex.value += 1
            playCurrentVideo(0L)
        }
    }

    fun playPrevious() {
        if (exoPlayer.currentPosition > 5000) {
            exoPlayer.seekTo(0)
        } else if (_currentIndex.value > 0) {
            _currentIndex.value -= 1
            playCurrentVideo(0L)
        } else {
            exoPlayer.seekTo(0)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _durationMs.value)
        exoPlayer.seekTo(target)
        _currentPositionMs.value = target
    }

    fun forward() {
        val seekMs = _seekDurationSeconds.value * 1000L
        seekTo(exoPlayer.currentPosition + seekMs)
    }

    fun rewind() {
        val seekMs = _seekDurationSeconds.value * 1000L
        seekTo(exoPlayer.currentPosition - seekMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(0.75f, 3.5f)
    }

    fun toggleScreenLock() {
        _isScreenLocked.value = !_isScreenLocked.value
    }

    fun setSeekDuration(seconds: Int) {
        _seekDurationSeconds.value = seconds
    }

    fun addExternalSubtitle(uri: Uri, label: String = "External Subtitle") {
        val video = _currentVideo.value ?: return
        val currentPos = exoPlayer.currentPosition
        val isCurrentlyPlaying = exoPlayer.isPlaying

        val subConfig = subtitleManager.createSubtitleConfiguration(uri, label)
        val mediaItem = MediaItem.Builder()
            .setUri(video.uri)
            .setMediaId(video.id.toString())
            .setSubtitleConfigurations(listOf(subConfig))
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(currentPos)
        exoPlayer.playWhenReady = isCurrentlyPlaying
    }

    fun selectSubtitleTrack(index: Int) {
        _selectedSubtitleIndex.value = index
        if (index == -1) {
            // Disable subtitles
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
        } else {
            val trackInfo = _subtitleTracks.value.getOrNull(index) ?: return
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguage(trackInfo.language)
            )
        }
    }

    fun selectAudioTrack(trackInfo: AudioTrackInfo) {
        val trackGroup = exoPlayer.currentTracks.groups.getOrNull(trackInfo.groupIndex) ?: return
        val override = TrackSelectionOverride(trackGroup.mediaTrackGroup, trackInfo.trackIndex)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .addOverride(override)
        )
        // Refresh audio track list state
        extractTracks(exoPlayer.currentTracks)
    }

    private fun extractTracks(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrackInfo>()
        val subList = mutableListOf<SubtitleTrackInfo>()

        for (groupIndex in tracks.groups.indices) {
            val group = tracks.groups[groupIndex]
            val trackType = group.type

            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    val lang = format.language ?: "und"
                    val label = format.label ?: "Audio ${audioList.size + 1} ($lang)"
                    audioList.add(
                        AudioTrackInfo(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            label = label,
                            language = lang,
                            sampleRate = format.sampleRate,
                            channelCount = format.channelCount,
                            isSelected = isSelected
                        )
                    )
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    val lang = format.language ?: "und"
                    val label = format.label ?: "Subtitle ${subList.size + 1} ($lang)"
                    subList.add(
                        SubtitleTrackInfo(
                            id = "$groupIndex:$trackIndex",
                            label = label,
                            language = lang
                        )
                    )
                }
            }
        }
        _audioTracks.value = audioList
        _subtitleTracks.value = subList
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (remaining <= 0) {
                exoPlayer.pause()
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                val buffered = exoPlayer.bufferedPosition.coerceAtLeast(0L)

                _currentPositionMs.value = current
                _durationMs.value = duration
                _bufferedPositionMs.value = buffered

                val video = _currentVideo.value
                if (video != null && duration > 0) {
                    onPlaybackProgress(video.uri, current, duration)
                }
                delay(1000L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        val current = exoPlayer.currentPosition.coerceAtLeast(0L)
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val video = _currentVideo.value
        if (video != null && duration > 0) {
            onPlaybackProgress(video.uri, current, duration)
        }
    }

    fun release() {
        stopProgressTracking()
        cancelSleepTimer()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
