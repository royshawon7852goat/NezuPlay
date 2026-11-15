package com.nezuplay.video.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nezuplay.video.data.models.VideoItem
import com.nezuplay.video.ui.theme.AccentRed

enum class OverlayType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    SPEED_BOOST,
    DOUBLE_TAP_REWIND,
    DOUBLE_TAP_FORWARD
}

@Composable
fun GestureOverlay(
    overlayType: OverlayType,
    brightnessFraction: Float, // 0f to 1f
    volumeFraction: Float,     // 0f to 1f
    seekDeltaMs: Long,
    targetSeekPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = overlayType != OverlayType.NONE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (overlayType) {
                OverlayType.BRIGHTNESS -> {
                    BrightnessVolumeHud(
                        title = "Brightness",
                        fraction = brightnessFraction,
                        icon = {
                            val icon = when {
                                brightnessFraction > 0.66f -> Icons.Default.BrightnessHigh
                                brightnessFraction > 0.33f -> Icons.Default.BrightnessMedium
                                else -> Icons.Default.BrightnessLow
                            }
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    )
                }

                OverlayType.VOLUME -> {
                    BrightnessVolumeHud(
                        title = "Volume",
                        fraction = volumeFraction,
                        icon = {
                            val icon = when {
                                volumeFraction <= 0.01f -> Icons.Default.VolumeMute
                                volumeFraction > 0.5f -> Icons.Default.VolumeUp
                                else -> Icons.Default.VolumeDown
                            }
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    )
                }

                OverlayType.SEEK -> {
                    SeekHud(
                        seekDeltaMs = seekDeltaMs,
                        targetPositionMs = targetSeekPositionMs,
                        durationMs = durationMs
                    )
                }

                OverlayType.SPEED_BOOST -> {
                    SpeedBoostHud()
                }

                OverlayType.DOUBLE_TAP_REWIND -> {
                    DoubleTapSeekHud(isForward = false)
                }

                OverlayType.DOUBLE_TAP_FORWARD -> {
                    DoubleTapSeekHud(isForward = true)
                }

                OverlayType.NONE -> Unit
            }
        }
    }
}

@Composable
private fun BrightnessVolumeHud(
    title: String,
    fraction: Float,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC151515))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${(fraction * 100).toInt()}%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x55FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(AccentRed)
                )
            }
        }
    }
}

@Composable
private fun SeekHud(
    seekDeltaMs: Long,
    targetPositionMs: Long,
    durationMs: Long
) {
    val sign = if (seekDeltaMs >= 0) "+" else ""
    val deltaFormatted = "$sign${VideoItem.formatDuration(Math.abs(seekDeltaMs))}"
    val targetFormatted = VideoItem.formatDuration(targetPositionMs)
    val totalFormatted = VideoItem.formatDuration(durationMs)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD151515))
            .padding(horizontal = 28.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (seekDeltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = deltaFormatted,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "[$targetFormatted / $totalFormatted]",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SpeedBoostHud() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(AccentRed.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "2.0X SPEED >>",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun DoubleTapSeekHud(isForward: Boolean) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color(0xAA151515)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = if (isForward) "+10s" else "-10s",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
