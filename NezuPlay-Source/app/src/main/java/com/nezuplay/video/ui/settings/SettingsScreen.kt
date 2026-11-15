package com.nezuplay.video.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nezuplay.video.ui.theme.AccentRed
import com.nezuplay.video.ui.theme.AmoledBlack
import com.nezuplay.video.ui.theme.CardDark
import com.nezuplay.video.ui.theme.TextPrimary
import com.nezuplay.video.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRescanLibrary: () -> Unit,
    onClearHistory: () -> Unit,
    onManageFolders: (Uri) -> Unit
) {
    val context = LocalContext.current

    // Preferences states
    var isDarkTheme by remember { mutableStateOf(true) }
    var gestureControlsEnabled by remember { mutableStateOf(true) }
    var doubleTapEnabled by remember { mutableStateOf(true) }
    var seekDuration by remember { mutableIntStateOf(10) }
    var backgroundAudioEnabled by remember { mutableStateOf(true) }
    var pipEnabled by remember { mutableStateOf(true) }
    var resumePlaybackEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(true) }

    // Dialog states
    var showSeekDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                onManageFolders(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Settings",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Group 1: Appearance
            item {
                SettingsGroup(title = "Appearance") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Palette,
                        title = "AMOLED Dark Theme",
                        subtitle = "Pitch black background for AMOLED screens",
                        checked = isDarkTheme,
                        onCheckedChange = { isDarkTheme = it }
                    )
                }
            }

            // Group 2: Video Player
            item {
                SettingsGroup(title = "Video Player & Gestures") {
                    SettingsClickableItem(
                        icon = Icons.Default.Speed,
                        title = "Seek Duration",
                        subtitle = "$seekDuration seconds",
                        onClick = { showSeekDialog = true }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.TouchApp,
                        title = "Gesture Controls",
                        subtitle = "Swipe left for brightness, right for volume, horizontal to seek",
                        checked = gestureControlsEnabled,
                        onCheckedChange = { gestureControlsEnabled = it }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.TouchApp,
                        title = "Double Tap to Seek",
                        subtitle = "Double tap sides to skip 10 seconds",
                        checked = doubleTapEnabled,
                        onCheckedChange = { doubleTapEnabled = it }
                    )
                }
            }

            // Group 3: Playback Behavior
            item {
                SettingsGroup(title = "Playback") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Headphones,
                        title = "Background Audio Playback",
                        subtitle = "Keep listening to video audio when screen is turned off",
                        checked = backgroundAudioEnabled,
                        onCheckedChange = { backgroundAudioEnabled = it }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.PictureInPicture,
                        title = "Picture-in-Picture (PiP)",
                        subtitle = "Continue playing in floating mini window",
                        checked = pipEnabled,
                        onCheckedChange = { pipEnabled = it }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.PlayCircle,
                        title = "Resume Playback",
                        subtitle = "Automatically resume videos from last saved position",
                        checked = resumePlaybackEnabled,
                        onCheckedChange = { resumePlaybackEnabled = it }
                    )
                }
            }

            // Group 4: Library & Storage
            item {
                SettingsGroup(title = "Library & Folders") {
                    SettingsClickableItem(
                        icon = Icons.Default.Refresh,
                        title = "Rescan Media Library",
                        subtitle = "Scan device storage and VidMate directories for new files",
                        onClick = onRescanLibrary
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.Folder,
                        title = "Add Custom Video Folder",
                        subtitle = "Grant Storage Access Framework permission to additional folders",
                        onClick = { safFolderLauncher.launch(null) }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Playback History",
                        subtitle = "Reset all saved positions and recently played records",
                        onClick = { showClearHistoryConfirm = true }
                    )
                }
            }

            // Group 5: Private Locker Security
            item {
                SettingsGroup(title = "Private Locker") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = "Use fingerprint or face recognition to unlock",
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.Lock,
                        title = "Auto-Lock Timeout",
                        subtitle = "Locks immediately when switching apps (FLAG_SECURE active)",
                        onClick = {}
                    )
                }
            }

            // Group 6: About
            item {
                SettingsGroup(title = "About NezuPlay") {
                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0 (Release Build)",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = "Open-Source Licenses",
                        subtitle = "ExoPlayer, Jetpack Compose, Room, Coil",
                        onClick = { showLicensesDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Seek Duration Dialog
    if (showSeekDialog) {
        val durations = listOf(5, 10, 15, 30)
        AlertDialog(
            onDismissRequest = { showSeekDialog = false },
            title = { Text("Seek Duration", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    durations.forEach { sec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    seekDuration = sec
                                    showSeekDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "$sec seconds",
                                color = if (seekDuration == sec) AccentRed else TextPrimary,
                                fontWeight = if (seekDuration == sec) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeekDialog = false }) {
                    Text("Close", color = AccentRed)
                }
            },
            containerColor = CardDark
        )
    }

    // Clear History Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear Playback History", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear your playback history?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showClearHistoryConfirm = false
                }) {
                    Text("Clear", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About NezuPlay", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("NezuPlay v1.0.0", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "A premium native Android video player with gesture controls, multi-track audio, subtitle management, PiP mode, and an encrypted private locker powered by AES-256-GCM and Android Keystore.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = AccentRed)
                }
            },
            containerColor = CardDark
        )
    }

    // Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open-Source Licenses", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("• AndroidX Media3 ExoPlayer (Apache 2.0)", color = TextSecondary, fontSize = 13.sp)
                    Text("• Jetpack Compose & Material 3 (Apache 2.0)", color = TextSecondary, fontSize = 13.sp)
                    Text("• Room Persistence Library (Apache 2.0)", color = TextSecondary, fontSize = 13.sp)
                    Text("• Coil Image & Video Loader (Apache 2.0)", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close", color = AccentRed)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = AccentRed,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentRed,
                uncheckedThumbColor = Color(0xFFAAAAAA),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}
