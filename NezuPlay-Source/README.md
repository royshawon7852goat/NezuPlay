# NezuPlay - Premium Native Android Video Player & Encrypted Private Locker

**NezuPlay** is a modern, offline-first native Android video player and secure media locker application built with Kotlin, Jetpack Compose, Android Media3 ExoPlayer, and Room Database. It combines the fluid gesture controls of PLAYit/VLC with military-grade AES-256-GCM encryption backed by Android Keystore and biometric security.

---

## 🚀 Key Features

### 1. Advanced Playback Engine (Android Media3 ExoPlayer)
- **Wide Format Compatibility**: Hardware-accelerated decoding for MP4, MKV, AVI, MOV, WEBM, FLV, 3GP, TS, M4V.
- **PLAYit-Style Gestures**:
  - **Left Side Vertical Swipe**: Smooth screen brightness control (0%–100%) with animated HUD.
  - **Right Side Vertical Swipe**: Stream volume control (0%–100%) with speaker icon HUD.
  - **Horizontal Swipe**: Precision seek scrubbing with target timestamp and delta indicators (+00:15 / -00:30).
  - **Double Tap Left**: Fast rewind 10 seconds.
  - **Double Tap Right**: Fast forward 10 seconds.
  - **Double Tap Center**: Instant Play/Pause toggle.
  - **Pinch to Zoom**: Smooth scaling from 1.0x to 3.5x.
  - **Long Press**: Temporary 2.0x playback speed boost with dynamic HUD badge.
- **Playback Controls**: Speeds (0.25x, 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x).
- **Aspect Ratio Modes**: Fit to screen, Fill, 16:9 Crop, Stretch, 100% Original.
- **Multi-Track Audio**: Switch between audio tracks (displaying language, sample rate, and channels).
- **Subtitle Management**: Embedded subtitle tracks + external subtitle picker (.srt, .vtt, .ssa, .ass).
- **Picture-in-Picture (PiP)**: Native floating mini-player supporting PiP mode while multitasking.
- **Background Audio**: Continue listening to video audio when the screen is off or in background via `NezuMediaService`.
- **Sleep Timer**: Configurable countdown (5, 10, 15, 30, 45, 60 minutes) with auto-pause.
- **Screen Lock**: Locks controls and touch interactions to prevent accidental touches.

---

### 2. Automatic Video Discovery & VidMate Compatibility
- **Real MediaStore Scanning**: Automatically queries local videos across internal and external storage.
- **VidMate Detection**: Specialized detection for VidMate download directories (`/Download`, `/Download/VidMate`, `/VidMate/download`, `/Movies/VidMate`) with prominent red "VidMate" badge.
- **Storage Access Framework (SAF)**: Users can grant access to any custom folder with persisted permissions (`takePersistableUriPermission`).
- **Grouping & Sorting**: Group by folders, sort by Name, Date Added, Duration, File Size, or Recently Played.
- **Filtering**: All Videos, Recently Added, Recently Played, Favorites.

---

### 3. Encrypted Private Video Locker
- **Genuine Cryptography**: AES-256-GCM authenticated encryption (`AES/GCM/NoPadding`) with unique 12-byte random IV per file and 128-bit authentication tags.
- **Android Keystore Protection**: Hardware-backed Master Key (`AndroidKeyStore`) protects encryption keys.
- **PIN Key Derivation**: High-iteration PBKDF2 with HMAC-SHA256 and unique 16-byte cryptographically secure random salt.
- **Biometric Authentication**: Fingerprint and Face Unlock support via `androidx.biometric.BiometricPrompt`.
- **Secure Ephemeral Playback**: Zero permanent unencrypted files. Ephemeral files are strictly scoped to internal cache, deleted on playback exit, and auto-purged on app backgrounding.
- **Window Security (FLAG_SECURE)**: Screenshots, screen recording, and system app switcher previews are disabled on private screens.
- **Brute-Force Throttling**: 30-second lockout after 5 consecutive failed PIN attempts.

---

## 🎨 Branding & Theme
- **AMOLED Pitch Black**: `#080808` primary background for maximum battery efficiency on AMOLED screens.
- **Crimson Red Accent**: `#E53935` primary accent.
- **Dark Surface**: `#151515`, Card Background: `#202020`, Elevated: `#282828`.
- **Custom Logo**: Sleek geometric play button integrated with a security shield.

---

## 📱 Tech Stack & Dependencies
- **Language**: Kotlin 2.3.20 (JVM Toolchain 17)
- **UI Framework**: Jetpack Compose (BOM 2026.03.01) + Material 3
- **Media Engine**: AndroidX Media3 ExoPlayer 1.5.1
- **Database**: Room Persistence Library 2.8.5 with KSP
- **Biometric**: AndroidX Biometric 1.1.0
- **Image/Video Loading**: Coil 2.6.0 with `coil-video`
- **Gradle**: Gradle 9.1.0, AGP 9.0.1
- **Min SDK**: API 26 (Android 8.0) | **Target SDK**: API 34 | **Compile SDK**: API 36

---

## 🛠️ Project Structure
```
NezuPlay/
├── app/
│   ├── src/main/
│   │   ├── java/com/nezuplay/video/
│   │   │   ├── MainActivity.kt               # Main navigation, lifecycle, PiP, permissions
│   │   │   ├── NezuPlayApp.kt                # Application class
│   │   │   ├── data/
│   │   │   │   ├── database/                 # Room Database, DAOs & Entities
│   │   │   │   ├── models/                   # VideoItem, FolderItem, Enums
│   │   │   │   └── repository/               # VideoRepository (MediaStore, SAF, VidMate)
│   │   │   ├── player/
│   │   │   │   ├── VideoPlayerManager.kt     # ExoPlayer wrapper, tracks, speeds, sleep timer
│   │   │   │   ├── VideoPlayerScreen.kt      # Compose video player UI & HUD
│   │   │   │   ├── GestureOverlay.kt         # Brightness, Volume, Seek, Speed HUDs
│   │   │   │   ├── SubtitleManager.kt        # External and embedded subtitle loader
│   │   │   │   └── NezuMediaService.kt       # Background audio MediaSessionService
│   │   │   ├── locker/
│   │   │   │   ├── EncryptionManager.kt      # AES-256-GCM & PBKDF2 cryptography
│   │   │   │   ├── LockerManager.kt          # PIN, Vault storage, ephemeral playback
│   │   │   │   └── BiometricAuthHelper.kt    # AndroidX BiometricPrompt integration
│   │   │   └── ui/
│   │   │       ├── components/               # TopBar, BottomNav, VideoCard, Dialogs
│   │   │       ├── home/HomeScreen.kt        # Continue Watching, Quick Access, Status
│   │   │       ├── videos/VideosScreen.kt    # Library, grid/list view, filter & sort
│   │   │       ├── folders/FoldersScreen.kt  # Folder list, VidMate badge, SAF add
│   │   │       ├── locker/LockerScreen.kt    # PIN setup, locked keypad, vault gallery
│   │   │       ├── settings/SettingsScreen.kt# Preferences, themes, timers, storage
│   │   │       ├── search/SearchScreen.kt    # Instant search across titles & folders
│   │   │       └── theme/                    # AMOLED colors, typography, theme
│   │   └── res/                              # Vector icons, themes, mipmap resources
│   └── build.gradle.kts
├── DEVELOPMENT_PROGRESS.md
└── build.gradle.kts
```

---

## 📦 APK Location & Installation

### Debug APK
The pre-compiled debug APK is available at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing via ADB (Nothing Phone 2a or any Android device)
1. Enable **Developer Options** and **USB Debugging** on your Android device.
2. Connect your device via USB.
3. Open a terminal in the project directory and run:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. If installing directly on phone from storage:
   - Copy `app-debug.apk` to phone storage.
   - Tap to install (Allow "Install from Unknown Sources" if prompted).

---

## 🔑 Building a Signed Release APK

To create a signed release APK for distribution:

1. **Generate a Keystore** (if you don't already have one):
   ```bash
   keytool -genkey -v -keystore nezuplay-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nezuplay
   ```

2. **Add Signing Config in `app/build.gradle.kts`**:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("path/to/nezuplay-release.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = "nezuplay"
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           release {
               isMinifyEnabled = true
               signingConfig = signingConfigs.getByName("release")
               proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
           }
       }
   }
   ```

3. **Run the Gradle assemble command**:
   ```bash
   ./gradlew assembleRelease
   ```
   The signed release APK will be generated at:
   `app/build/outputs/apk/release/app-release.apk`
