package com.nezuplay.video.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.nezuplay.video.data.models.AspectRatioMode
import com.nezuplay.video.data.models.VideoItem
import com.nezuplay.video.ui.theme.AccentRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    playerManager: VideoPlayerManager,
    isPrivateVideo: Boolean = false,
    isInPipMode: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val currentVideo by playerManager.currentVideo.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val aspectRatioMode by playerManager.aspectRatioMode.collectAsState()
    val isScreenLocked by playerManager.isScreenLocked.collectAsState()
    val zoomScale by playerManager.zoomScale.collectAsState()
    val audioTracks by playerManager.audioTracks.collectAsState()
    val subtitleTracks by playerManager.subtitleTracks.collectAsState()
    val selectedSubIndex by playerManager.selectedSubtitleIndex.collectAsState()
    val sleepTimerSec by playerManager.sleepTimerRemainingSeconds.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }
    var overlayType by remember { mutableStateOf(OverlayType.NONE) }
    var brightnessFraction by remember { mutableFloatStateOf(0.5f) }
    var volumeFraction by remember { mutableFloatStateOf(0.5f) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    var targetSeekMs by remember { mutableLongStateOf(0L) }

    // Dialog sheets
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Subtitle file picker launcher
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            playerManager.addExternalSubtitle(uri, uri.lastPathSegment ?: "External Subtitle")
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Protect private videos with FLAG_SECURE
    DisposableEffect(isPrivateVideo) {
        val window = activity?.window
        if (isPrivateVideo && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (isPrivateVideo && window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && !isScreenLocked) {
            delay(3500L)
            areControlsVisible = false
        }
    }

    // Hide overlays after inactivity
    LaunchedEffect(overlayType) {
        if (overlayType != OverlayType.NONE && overlayType != OverlayType.SPEED_BOOST) {
            delay(1200L)
            overlayType = OverlayType.NONE
        }
    }

    BackHandler {
        if (isScreenLocked) {
            playerManager.toggleScreenLock()
        } else {
            onBack()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()

        // 1. ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.exoPlayer
                    useController = false
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.CROP -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ORIGINAL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                }
        )

        // 2. Gesture Detector Layer
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isScreenLocked) {
                        if (isScreenLocked) {
                            detectTapGestures { areControlsVisible = !areControlsVisible }
                        } else {
                            detectTapGestures(
                                onTap = {
                                    areControlsVisible = !areControlsVisible
                                },
                                onDoubleTap = { offset ->
                                    val third = size.width / 3f
                                    when {
                                        offset.x < third -> {
                                            playerManager.rewind()
                                            overlayType = OverlayType.DOUBLE_TAP_REWIND
                                        }
                                        offset.x > third * 2 -> {
                                            playerManager.forward()
                                            overlayType = OverlayType.DOUBLE_TAP_FORWARD
                                        }
                                        else -> {
                                            playerManager.togglePlayPause()
                                        }
                                    }
                                },
                                onLongPress = {
                                    playerManager.setPlaybackSpeed(2.0f)
                                    overlayType = OverlayType.SPEED_BOOST
                                }
                            )
                        }
                    }
                    .pointerInput(isScreenLocked) {
                        if (!isScreenLocked) {
                            var isHorizontalDrag = false
                            var isVerticalDrag = false
                            var dragStartX = 0f
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var initialSeekMs = 0L

                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStartX = offset.x
                                    totalDragX = 0f
                                    totalDragY = 0f
                                    initialSeekMs = playerManager.exoPlayer.currentPosition
                                    isHorizontalDrag = false
                                    isVerticalDrag = false
                                },
                                onDragEnd = {
                                    if (isHorizontalDrag) {
                                        playerManager.seekTo(targetSeekMs)
                                    }
                                    overlayType = OverlayType.NONE
                                    if (playerManager.playbackSpeed.value > 1.0f && overlayType == OverlayType.SPEED_BOOST) {
                                        playerManager.setPlaybackSpeed(1.0f)
                                    }
                                },
                                onDragCancel = {
                                    overlayType = OverlayType.NONE
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y

                                    if (!isHorizontalDrag && !isVerticalDrag) {
                                        if (Math.abs(totalDragX) > Math.abs(totalDragY) && Math.abs(totalDragX) > 25f) {
                                            isHorizontalDrag = true
                                        } else if (Math.abs(totalDragY) > 25f) {
                                            isVerticalDrag = true
                                        }
                                    }

                                    if (isHorizontalDrag) {
                                        val seekRatio = totalDragX / size.width
                                        val delta = (seekRatio * 90000L).toLong() // up to +/- 90 seconds
                                        val target = (initialSeekMs + delta).coerceIn(0L, durationMs)
                                        seekDeltaMs = delta
                                        targetSeekMs = target
                                        overlayType = OverlayType.SEEK
                                    } else if (isVerticalDrag) {
                                        val deltaFraction = -dragAmount.y / size.height
                                        if (dragStartX < size.width * 0.5f) {
                                            // Left: Brightness
                                            val currentBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                            val base = if (currentBrightness < 0f) 0.5f else currentBrightness
                                            val newBrightness = (base + deltaFraction).coerceIn(0.01f, 1.0f)
                                            activity?.window?.let { win ->
                                                val lp = win.attributes
                                                lp.screenBrightness = newBrightness
                                                win.attributes = lp
                                            }
                                            brightnessFraction = newBrightness
                                            overlayType = OverlayType.BRIGHTNESS
                                        } else {
                                            // Right: Volume
                                            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                            val deltaVol = (deltaFraction * maxVolume).toInt()
                                            val targetVol = (curVol + deltaVol).coerceIn(0, maxVolume)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                            volumeFraction = targetVol.toFloat() / maxVolume.toFloat()
                                            overlayType = OverlayType.VOLUME
                                        }
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(isScreenLocked) {
                        if (!isScreenLocked) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1f) {
                                    val newZoom = (playerManager.zoomScale.value * zoom).coerceIn(1.0f, 3.0f)
                                    playerManager.setZoomScale(newZoom)
                                }
                            }
                        }
                    }
            )
        }

        // 3. Visual Feedback Overlays
        GestureOverlay(
            overlayType = overlayType,
            brightnessFraction = brightnessFraction,
            volumeFraction = volumeFraction,
            seekDeltaMs = seekDeltaMs,
            targetSeekPositionMs = targetSeekMs,
            durationMs = durationMs
        )

        // 4. Player Controls (Top, Center, Bottom)
        if (!isInPipMode) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Dark scrim overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                    )

                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color(0x99000000))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Text(
                            text = currentVideo?.title ?: "Video",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // PiP Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                try {
                                    val aspectRatio = if (currentVideo != null && currentVideo!!.width > 0 && currentVideo!!.height > 0) {
                                        Rational(currentVideo!!.width, currentVideo!!.height)
                                    } else {
                                        Rational(16, 9)
                                    }
                                    val pipParams = PictureInPictureParams.Builder()
                                        .setAspectRatio(aspectRatio)
                                        .build()
                                    activity?.enterPictureInPictureMode(pipParams)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                            }
                        }

                        // Sleep Timer Indicator
                        if (sleepTimerSec != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentRed)
                                    .clickable { showSleepTimerDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val mins = (sleepTimerSec ?: 0L) / 60
                                val secs = (sleepTimerSec ?: 0L) % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = Color.White)
                            }
                        }

                        // Audio tracks & Subtitles menu
                        IconButton(onClick = { showAudioTrackDialog = true }) {
                            Icon(Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White)
                        }

                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(Icons.Default.ClosedCaption, contentDescription = "Subtitles", tint = Color.White)
                        }
                    }

                    // Screen Lock floating button
                    IconButton(
                        onClick = { playerManager.toggleScreenLock() },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                    ) {
                        Icon(
                            imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = if (isScreenLocked) AccentRed else Color.White
                        )
                    }

                    // Center Playback Controls
                    if (!isScreenLocked) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            IconButton(
                                onClick = { playerManager.playPrevious() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            IconButton(
                                onClick = { playerManager.rewind() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            // Big Play/Pause Button
                            IconButton(
                                onClick = { playerManager.togglePlayPause() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AccentRed)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerManager.forward() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            IconButton(
                                onClick = { playerManager.playNext() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }

                    // Bottom Bar Controls
                    if (!isScreenLocked) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0x99000000))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Progress bar
                            val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                            Slider(
                                value = progress,
                                onValueChange = { frac ->
                                    val target = (frac * durationMs).toLong()
                                    playerManager.seekTo(target)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentRed,
                                    activeTrackColor = AccentRed,
                                    inactiveTrackColor = Color(0x55FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Timestamp and Bottom Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${VideoItem.formatDuration(currentPositionMs)} / ${VideoItem.formatDuration(durationMs)}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Speed Selector Pill
                                    TextButton(onClick = { showSpeedDialog = true }) {
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = AccentRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    // Aspect Ratio Toggle
                                    IconButton(onClick = {
                                        val modes = AspectRatioMode.values()
                                        val nextIndex = (aspectRatioMode.ordinal + 1) % modes.size
                                        playerManager.setAspectRatio(modes[nextIndex])
                                    }) {
                                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                                    }

                                    // Screen Orientation toggle
                                    IconButton(onClick = {
                                        val currentOrientation = activity?.requestedOrientation
                                        if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                    }) {
                                        Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Speed Selection Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${speed}x ${if (speed == 1.0f) "(Normal)" else ""}",
                                color = if (playbackSpeed == speed) AccentRed else Color.White,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = AccentRed)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Audio Tracks Selection Dialog
    if (showAudioTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAudioTrackDialog = false },
            title = { Text("Audio Tracks", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (audioTracks.isEmpty()) {
                        Text("Default Audio Track", color = Color.White)
                    } else {
                        audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.selectAudioTrack(track)
                                        showAudioTrackDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${track.label} (${track.channelCount} ch, ${track.sampleRate} Hz)",
                                    color = if (track.isSelected) AccentRed else Color.White,
                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioTrackDialog = false }) {
                    Text("Close", color = AccentRed)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Subtitles Dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Subtitles", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Turn Off option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerManager.selectSubtitleTrack(-1)
                                showSubtitleDialog = false
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Disable Subtitles",
                            color = if (selectedSubIndex == -1) AccentRed else Color.White,
                            fontWeight = if (selectedSubIndex == -1) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Available embedded/loaded subtitle tracks
                    subtitleTracks.forEachIndexed { index, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.selectSubtitleTrack(index)
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = track.label,
                                color = if (selectedSubIndex == index) AccentRed else Color.White,
                                fontWeight = if (selectedSubIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            showSubtitleDialog = false
                            subtitlePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Load External Subtitle (.srt, .vtt, .ssa)", color = AccentRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Close", color = AccentRed)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val timerOptions = listOf(5, 10, 15, 30, 45, 60)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (sleepTimerSec != null) {
                        TextButton(
                            onClick = {
                                playerManager.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn Off Timer", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    timerOptions.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.startSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "$minutes minutes",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = AccentRed)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}
