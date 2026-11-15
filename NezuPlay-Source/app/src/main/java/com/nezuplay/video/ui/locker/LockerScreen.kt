package com.nezuplay.video.ui.locker

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.nezuplay.video.data.database.PrivateVideoEntity
import com.nezuplay.video.data.models.VideoItem
import com.nezuplay.video.locker.BiometricAuthHelper
import com.nezuplay.video.locker.LockerAuthState
import com.nezuplay.video.locker.LockerManager
import com.nezuplay.video.ui.components.DeleteConfirmationDialog
import com.nezuplay.video.ui.components.RenameDialog
import com.nezuplay.video.ui.theme.AccentRed
import com.nezuplay.video.ui.theme.AmoledBlack
import com.nezuplay.video.ui.theme.CardDark
import com.nezuplay.video.ui.theme.CardElevated
import com.nezuplay.video.ui.theme.TextPrimary
import com.nezuplay.video.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockerScreen(
    lockerManager: LockerManager,
    onPlayPrivateVideo: (PrivateVideoEntity, File) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    val authState by lockerManager.authState.collectAsState()
    val privateVideos by lockerManager.privateVideos.collectAsState()
    val isImporting by lockerManager.isImporting.collectAsState()
    val importProgress by lockerManager.importProgress.collectAsState()
    val failedAttempts by lockerManager.failedAttempts.collectAsState()
    val lockoutUntilMs by lockerManager.lockoutUntilMs.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var setupErrorMessage by remember { mutableStateOf<String?>(null) }
    var enableBiometricInSetup by remember { mutableStateOf(true) }
    var unlockErrorMessage by remember { mutableStateOf<String?>(null) }
    var remainingLockoutSec by remember { mutableLongStateOf(0L) }

    // Dialog states
    var videoForRename by remember { mutableStateOf<PrivateVideoEntity?>(null) }
    var videoForDelete by remember { mutableStateOf<PrivateVideoEntity?>(null) }
    var isPreparingPlayback by remember { mutableStateOf(false) }

    val biometricHelper = remember(activity) {
        activity?.let { BiometricAuthHelper(it) }
    }

    // Protect Private Locker screen with FLAG_SECURE (anti-screenshot, anti-recording, recents blanking)
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Check initial state on first composition
    LaunchedEffect(Unit) {
        lockerManager.checkInitialState()
    }

    // Lockout countdown timer
    LaunchedEffect(lockoutUntilMs) {
        while (System.currentTimeMillis() < lockoutUntilMs) {
            remainingLockoutSec = (lockoutUntilMs - System.currentTimeMillis()) / 1000
            delay(1000L)
        }
        remainingLockoutSec = 0L
    }

    // Video Import Picker
    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                lockerManager.importVideo(uri)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        when (authState) {
            LockerAuthState.SETUP_REQUIRED -> {
                // First-Time PIN Setup Screen
                LockerSetupView(
                    enteredPin = enteredPin,
                    confirmPin = confirmPin,
                    isConfirmStep = isConfirmStep,
                    errorMessage = setupErrorMessage,
                    enableBiometric = enableBiometricInSetup,
                    isBiometricAvailable = biometricHelper?.isBiometricAvailable() == true,
                    onPinDigit = { digit ->
                        if (!isConfirmStep) {
                            if (enteredPin.length < 6) enteredPin += digit
                        } else {
                            if (confirmPin.length < 6) confirmPin += digit
                        }
                    },
                    onBackspace = {
                        if (!isConfirmStep) {
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        } else {
                            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                        }
                    },
                    onNextStep = {
                        if (enteredPin.length < 4) {
                            setupErrorMessage = "PIN must be at least 4 digits."
                        } else {
                            setupErrorMessage = null
                            isConfirmStep = true
                        }
                    },
                    onConfirm = {
                        if (enteredPin == confirmPin) {
                            scope.launch {
                                lockerManager.setupPin(enteredPin, enableBiometricInSetup)
                            }
                        } else {
                            setupErrorMessage = "PINs do not match. Try again."
                            confirmPin = ""
                        }
                    },
                    onBiometricChange = { enableBiometricInSetup = it }
                )
            }

            LockerAuthState.LOCKED -> {
                // Locked PIN & Biometric Screen
                LockerLockedView(
                    enteredPin = enteredPin,
                    errorMessage = unlockErrorMessage,
                    lockoutSec = remainingLockoutSec,
                    failedAttempts = failedAttempts,
                    showBiometricButton = biometricHelper?.isBiometricAvailable() == true,
                    onPinDigit = { digit ->
                        if (enteredPin.length < 6 && remainingLockoutSec == 0L) {
                            val newPin = enteredPin + digit
                            enteredPin = newPin
                            if (newPin.length >= 4) {
                                scope.launch {
                                    val success = lockerManager.unlockWithPin(newPin)
                                    if (success) {
                                        enteredPin = ""
                                        unlockErrorMessage = null
                                    } else {
                                        if (newPin.length >= 6) {
                                            unlockErrorMessage = "Incorrect PIN"
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                    },
                    onBiometricClick = {
                        biometricHelper?.promptBiometric(
                            onSuccess = {
                                scope.launch {
                                    lockerManager.unlockWithBiometric()
                                }
                            },
                            onError = { err ->
                                unlockErrorMessage = err
                            }
                        )
                    }
                )
            }

            LockerAuthState.UNLOCKED -> {
                // Unlocked Private Vault Gallery
                LockerUnlockedView(
                    privateVideos = privateVideos,
                    isImporting = isImporting,
                    importProgress = importProgress,
                    isPreparingPlayback = isPreparingPlayback,
                    onLockNow = { lockerManager.lock() },
                    onImportClick = { importPickerLauncher.launch(arrayOf("video/*")) },
                    onPlayVideo = { video ->
                        scope.launch {
                            isPreparingPlayback = true
                            val ephemeralFile = lockerManager.prepareEphemeralPlayback(video)
                            isPreparingPlayback = false
                            if (ephemeralFile != null) {
                                onPlayPrivateVideo(video, ephemeralFile)
                            }
                        }
                    },
                    onRenameClick = { videoForRename = it },
                    onDeleteClick = { videoForDelete = it }
                )
            }
        }
    }

    // Rename Dialog
    videoForRename?.let { video ->
        RenameDialog(
            currentTitle = video.title,
            onDismiss = { videoForRename = null },
            onConfirm = { newTitle ->
                scope.launch {
                    lockerManager.renamePrivateVideo(video, newTitle)
                    videoForRename = null
                }
            }
        )
    }

    // Delete Dialog
    videoForDelete?.let { video ->
        DeleteConfirmationDialog(
            title = video.title,
            onDismiss = { videoForDelete = null },
            onConfirm = {
                scope.launch {
                    lockerManager.deletePrivateVideo(video)
                    videoForDelete = null
                }
            }
        )
    }
}

@Composable
private fun LockerSetupView(
    enteredPin: String,
    confirmPin: String,
    isConfirmStep: Boolean,
    errorMessage: String?,
    enableBiometric: Boolean,
    isBiometricAvailable: Boolean,
    onPinDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onNextStep: () -> Unit,
    onConfirm: () -> Unit,
    onBiometricChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x33E53935)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AccentRed, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Welcome to NezuPlay Private Locker",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (!isConfirmStep) "Create a secure 4 to 6 digit PIN" else "Confirm your PIN",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pin Dots
            val pin = if (!isConfirmStep) enteredPin else confirmPin
            PinIndicatorRow(pinLength = pin.length)

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = AccentRed, fontSize = 12.sp)
            }

            if (!isConfirmStep && isBiometricAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardDark)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Biometric Unlock", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = enableBiometric,
                        onCheckedChange = onBiometricChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentRed)
                    )
                }
            }
        }

        // Keypad & Action
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            NumericKeypad(
                onDigit = onPinDigit,
                onBackspace = onBackspace
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = if (!isConfirmStep) onNextStep else onConfirm,
                enabled = if (!isConfirmStep) enteredPin.length >= 4 else confirmPin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (!isConfirmStep) "Next" else "Save & Protect", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LockerLockedView(
    enteredPin: String,
    errorMessage: String?,
    lockoutSec: Long,
    failedAttempts: Int,
    showBiometricButton: Boolean,
    onPinDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF201212)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentRed, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Private Locker",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (lockoutSec > 0) "Locked for $lockoutSec seconds" else "Enter PIN or use Biometric to unlock",
                color = if (lockoutSec > 0) AccentRed else TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
            PinIndicatorRow(pinLength = enteredPin.length)

            if (errorMessage != null && lockoutSec == 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = AccentRed, fontSize = 12.sp)
            }
            if (failedAttempts > 0 && lockoutSec == 0L) {
                Text(text = "Failed attempts: $failedAttempts (Max 5)", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Keypad with Biometric option
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            NumericKeypad(
                onDigit = onPinDigit,
                onBackspace = onBackspace,
                leftActionIcon = if (showBiometricButton) Icons.Default.Fingerprint else null,
                onLeftAction = onBiometricClick
            )
        }
    }
}

@Composable
private fun LockerUnlockedView(
    privateVideos: List<PrivateVideoEntity>,
    isImporting: Boolean,
    importProgress: Float,
    isPreparingPlayback: Boolean,
    onLockNow: () -> Unit,
    onImportClick: () -> Unit,
    onPlayVideo: (PrivateVideoEntity) -> Unit,
    onRenameClick: (PrivateVideoEntity) -> Unit,
    onDeleteClick: (PrivateVideoEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Unlocked Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33E53935)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Encrypted Vault",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${privateVideos.size} private videos • AES-256-GCM",
                    color = AccentRed,
                    fontSize = 11.sp
                )
            }

            // Lock Now Button
            IconButton(onClick = onLockNow) {
                Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = AccentRed)
            }

            // Import Button
            IconButton(onClick = onImportClick) {
                Icon(Icons.Default.Add, contentDescription = "Import Video", tint = Color.White)
            }
        }

        // Importing progress banner
        if (isImporting) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Encrypting & Importing Video...", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${(importProgress * 100).toInt()}%", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { importProgress },
                        color = AccentRed,
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            }
        }

        if (isPreparingPlayback) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = AccentRed, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decrypting securely for playback...", color = TextSecondary, fontSize = 12.sp)
            }
        }

        // Content
        if (privateVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Private Locker is Empty",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import videos from your library or VidMate downloads to encrypt them.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onImportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Videos", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(privateVideos, key = { it.id }) { video ->
                    PrivateVideoCard(
                        video = video,
                        onPlay = { onPlayVideo(video) },
                        onRename = { onRenameClick(video) },
                        onDelete = { onDeleteClick(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivateVideoCard(
    video: PrivateVideoEntity,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate = remember(video.importTimestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(video.importTimestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B1B1B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0x44FFFFFF), modifier = Modifier.size(28.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${VideoItem.formatFileSize(video.sizeBytes)} • Encrypted $formattedDate",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(CardDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Securely", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentRed) },
                        onClick = { showMenu = false; onPlay() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete from Locker", color = AccentRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinIndicatorRow(pinLength: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val filled = index < pinLength
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (filled) AccentRed else Color(0x33FFFFFF))
            )
        }
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    leftActionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onLeftAction: (() -> Unit)? = null
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("action", "0", "back")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { item ->
                    when (item) {
                        "action" -> {
                            if (leftActionIcon != null && onLeftAction != null) {
                                KeypadButton(onClick = onLeftAction) {
                                    Icon(leftActionIcon, contentDescription = "Biometric", tint = AccentRed, modifier = Modifier.size(28.dp))
                                }
                            } else {
                                Spacer(modifier = Modifier.size(68.dp))
                            }
                        }
                        "back" -> {
                            KeypadButton(onClick = onBackspace) {
                                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        else -> {
                            KeypadButton(onClick = { onDigit(item) }) {
                                Text(text = item, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color(0xFF1C1C1C))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
