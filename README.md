# 🎬 NezuPlay – Video Player & Private Locker

**Your Videos. Your Privacy. Your Control.**

NezuPlay is a modern Android video player designed to deliver a smooth, powerful and private video-watching experience.

Inspired by modern media players like PLAYit and VLC, NezuPlay aims to combine advanced video playback, intuitive gesture controls, subtitle support and a built-in encrypted private video locker in one application.

Built with Kotlin and Jetpack Compose, NezuPlay focuses on performance, simplicity, privacy and a premium user experience.

---

## ✨ Features

### 🎥 Advanced Video Player
- Play locally stored videos.
- Support for popular video formats.
- Smooth video playback powered by Android Media3 ExoPlayer.
- Resume playback from the last position.
- Multiple playback speed options.
- Full-screen playback.
- Video aspect ratio controls.
- Audio track selection.

### 📂 Smart Video Library
- Automatically discover accessible videos.
- Browse videos by folders.
- Detect downloaded videos from VidMate and other applications.
- Recently added videos.
- Recently played videos.
- Video thumbnails and metadata.
- Search and sorting functionality.

### 👆 Smart Gesture Controls
- Swipe vertically to adjust brightness.
- Swipe vertically to adjust volume.
- Swipe horizontally to seek.
- Double-tap to rewind or forward.
- Pinch to zoom.
- Long-press for temporary speed control.

### 🔒 Private Video Locker
- Secure PIN authentication.
- Fingerprint authentication where supported.
- AES-256-GCM encrypted video storage.
- Android Keystore integration.
- Private video library.
- Secure private video playback.
- Automatic locker timeout.
- Protected private playback screens.

**Privacy First:** Private videos are intended to be encrypted and stored in application-private storage rather than simply hidden from the gallery.

### 💬 Subtitle Support
- SRT subtitles.
- VTT subtitles.
- SSA subtitles.
- ASS subtitles.
- External subtitle selection.
- Subtitle synchronization.
- Custom subtitle appearance.

### 🚀 Additional Features
- Picture-in-Picture mode.
- Background audio playback.
- Sleep timer.
- Favorites.
- Playback history.
- Recently watched videos.
- Dark AMOLED interface.

---

## 🎨 Design Philosophy

NezuPlay follows a modern, minimalist design philosophy.

**Design highlights:**
- Deep AMOLED black backgrounds.
- Clean typography.
- Modern video thumbnails.
- Smooth animations.
- Minimalist controls.
- Red accent colours.
- User-friendly navigation.

The goal is to create a premium media-player experience without unnecessary clutter.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | Modern Android UI |
| Material 3 | UI components |
| Media3 ExoPlayer | Video playback |
| Room Database | Local database |
| Android MediaStore | Video discovery |
| Storage Access Framework | File access |
| Android Keystore | Encryption key protection |
| AES-256-GCM | Private video encryption |
| Android Biometric API | Fingerprint authentication |
| Kotlin Coroutines | Asynchronous operations |

---

## 📱 Compatibility

- Platform: Android
- Minimum Android Version: Android 8.0 (API 26)
- Primary Testing Device: Nothing Phone 2a
- Application Type: Native Android Application

Video playback compatibility depends on the device's supported codecs and decoders.

---

## 📥 Installation

### Option 1: Download APK

1. Visit the GitHub Releases section.
2. Download the latest NezuPlay APK.
3. Open the downloaded APK on your Android device.
4. Allow installation from the relevant source if Android requests it.
5. Install and launch NezuPlay.

**Latest Release:** Coming Soon.

### Option 2: Build from Source

Clone the repository:

```bash
git clone https://github.com/YOUR_USERNAME/NezuPlay.git
```

Open the project in Android Studio.

Wait for Gradle synchronization.

Connect your Android device or start an emulator.

Build and run the application.

To generate a debug APK:

```bash
gradlew assembleDebug
```

The generated APK will be located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📁 Project Structure

```text
NezuPlay/
│
├── app/
│   └── src/main/
│       ├── java/com/nezuplay/video/
│       │
│       ├── data/
│       ├── domain/
│       ├── player/
│       ├── locker/
│       ├── ui/
│       ├── navigation/
│       │
│       └── MainActivity.kt
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── DEVELOPMENT_PROGRESS.md
```

---

## 🗺️ Development Roadmap

- [x] Project concept and architecture planning
- [x] Feature specification
- [ ] Android project initialization
- [ ] Application UI
- [ ] Automatic video scanning
- [ ] VidMate download compatibility
- [ ] Video playback engine
- [ ] Gesture controls
- [ ] Subtitle support
- [ ] Picture-in-Picture
- [ ] Playback history
- [ ] Private video locker
- [ ] PIN authentication
- [ ] Biometric authentication
- [ ] AES-256-GCM encryption
- [ ] Application testing
- [ ] APK generation
- [ ] First stable release (v1.0)

*Roadmap status should be updated as development progresses.*

---

## 🔐 Privacy & Security

NezuPlay is designed with privacy as a priority.

Security goals include:

- Private video encryption.
- Secure encryption key management.
- Authentication before accessing private content.
- No unnecessary exposure of private media.
- Secure handling of temporary decrypted data.

Private locker security features must be verified through testing before being considered production-ready.

---

## 🤝 Contributing

Contributions, suggestions and bug reports are welcome.

If you want to contribute:

1. Fork this repository.
2. Create a new branch.
3. Make your changes.
4. Submit a Pull Request.

Please report bugs through GitHub Issues.

---

## 📌 Project Status

**Current Status:** In Development 🚧

NezuPlay is under active development. Features listed in the roadmap may not yet be available.

---

## 👨‍💻 Developer

Developed as an independent Android application project.

**Project Name:** NezuPlay  
**Version:** 1.0 (Planned)  
**Platform:** Android  
**License:** To be determined

---

## ⭐ Support

If you find NezuPlay interesting, consider starring this repository.

Your feedback and suggestions can help improve the project.

**NezuPlay – Watch Freely. Keep It Private.**
