package com.nezuplay.video

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.nezuplay.video.data.database.PrivateVideoEntity
import com.nezuplay.video.data.models.VideoItem
import com.nezuplay.video.data.repository.VideoRepository
import com.nezuplay.video.locker.LockerManager
import com.nezuplay.video.player.NezuMediaService
import com.nezuplay.video.player.VideoPlayerManager
import com.nezuplay.video.player.VideoPlayerScreen
import com.nezuplay.video.ui.components.MainDestination
import com.nezuplay.video.ui.components.NezuBottomNav
import com.nezuplay.video.ui.folders.FoldersScreen
import com.nezuplay.video.ui.home.HomeScreen
import com.nezuplay.video.ui.locker.LockerScreen
import com.nezuplay.video.ui.search.SearchScreen
import com.nezuplay.video.ui.settings.SettingsScreen
import com.nezuplay.video.ui.theme.AmoledBlack
import com.nezuplay.video.ui.theme.NezuPlayTheme
import com.nezuplay.video.ui.videos.VideosScreen
import kotlinx.coroutines.launch
import java.io.File

enum class ScreenState {
    MAIN_NAVIGATION,
    SEARCH,
    PLAYER
}

class MainActivity : FragmentActivity() {

    private lateinit var videoRepository: VideoRepository
    private lateinit var lockerManager: LockerManager
    private lateinit var playerManager: VideoPlayerManager

    private var isInPipMode by mutableStateOf(false)
    private var activePrivateFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoRepository = VideoRepository(this)
        lockerManager = LockerManager(this)
        playerManager = VideoPlayerManager(this) { uri, pos, dur ->
            lifecycleScopeLaunch {
                videoRepository.savePlaybackPosition(uri, pos, dur)
            }
        }

        NezuMediaService.activePlayer = playerManager.exoPlayer

        // Handle intent if launched from outside (e.g. VidMate or file manager)
        handleIntent(intent)

        setContent {
            NezuPlayTheme {
                MainAppContent()
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            val tempItem = VideoItem(
                id = uri.hashCode().toLong(),
                uri = uri,
                title = uri.lastPathSegment ?: "External Video",
                durationMs = 0L,
                sizeBytes = 0L,
                dateAdded = System.currentTimeMillis() / 1000
            )
            playerManager.setPlaylist(listOf(tempItem), 0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        lifecycleScope.launch {
            block()
        }
    }

    @Composable
    private fun MainAppContent() {
        val scope = rememberCoroutineScope()
        var currentScreen by remember { mutableStateOf(ScreenState.MAIN_NAVIGATION) }
        var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
        var isPlayingPrivate by remember { mutableStateOf(false) }

        val allVideos by videoRepository.allVideos.collectAsState()
        val folders by videoRepository.folders.collectAsState()
        val isScanning by videoRepository.isScanning.collectAsState()

        // Permissions launcher
        val permissionsToRequest = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            // Trigger initial discovery scan
            scope.launch {
                videoRepository.refreshVideos()
            }
        }

        LaunchedEffect(Unit) {
            val hasPermission = permissionsToRequest.any {
                ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
            }
            if (hasPermission) {
                videoRepository.refreshVideos()
            } else {
                permissionLauncher.launch(permissionsToRequest)
            }
        }

        // Auto-lock on background lifecycle
        DisposableEffect(this) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    lockerManager.lock()
                    activePrivateFile?.delete()
                    activePrivateFile = null
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

        // Immersive mode for video player
        LaunchedEffect(currentScreen) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (currentScreen == ScreenState.PLAYER) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        if (currentScreen == ScreenState.PLAYER) {
            VideoPlayerScreen(
                playerManager = playerManager,
                isPrivateVideo = isPlayingPrivate,
                isInPipMode = isInPipMode,
                onBack = {
                    playerManager.exoPlayer.pause()
                    if (isPlayingPrivate) {
                        activePrivateFile?.delete()
                        activePrivateFile = null
                        isPlayingPrivate = false
                    }
                    currentScreen = ScreenState.MAIN_NAVIGATION
                }
            )
        } else if (currentScreen == ScreenState.SEARCH) {
            BackHandler { currentScreen = ScreenState.MAIN_NAVIGATION }
            SearchScreen(
                videos = allVideos,
                onVideoClick = { video ->
                    val index = allVideos.indexOfFirst { it.uri == video.uri }
                    playerManager.setPlaylist(allVideos, if (index != -1) index else 0)
                    isPlayingPrivate = false
                    currentScreen = ScreenState.PLAYER
                },
                onBack = { currentScreen = ScreenState.MAIN_NAVIGATION }
            )
        } else {
            Scaffold(
                bottomBar = {
                    if (!isInPipMode) {
                        NezuBottomNav(
                            currentDestination = currentDestination,
                            onSelectDestination = { currentDestination = it }
                        )
                    }
                },
                containerColor = AmoledBlack
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "MainTabs"
                    ) { dest ->
                        when (dest) {
                            MainDestination.HOME -> HomeScreen(
                                allVideos = allVideos,
                                isScanning = isScanning,
                                onVideoClick = { video ->
                                    val index = allVideos.indexOfFirst { it.uri == video.uri }
                                    playerManager.setPlaylist(allVideos, if (index != -1) index else 0)
                                    isPlayingPrivate = false
                                    currentScreen = ScreenState.PLAYER
                                },
                                onNavigate = { currentDestination = it },
                                onSearchClick = { currentScreen = ScreenState.SEARCH },
                                onRefresh = { scope.launch { videoRepository.refreshVideos() } }
                            )

                            MainDestination.VIDEOS -> VideosScreen(
                                videos = allVideos,
                                onVideoClick = { video ->
                                    val index = allVideos.indexOfFirst { it.uri == video.uri }
                                    playerManager.setPlaylist(allVideos, if (index != -1) index else 0)
                                    isPlayingPrivate = false
                                    currentScreen = ScreenState.PLAYER
                                },
                                onFavoriteToggle = { video ->
                                    scope.launch { videoRepository.toggleFavorite(video) }
                                },
                                onRenameVideo = { video, newTitle ->
                                    scope.launch { videoRepository.renameVideo(video, newTitle) }
                                },
                                onDeleteVideo = { video ->
                                    scope.launch { videoRepository.deleteVideo(video) }
                                },
                                onSearchClick = { currentScreen = ScreenState.SEARCH },
                                onRefresh = { scope.launch { videoRepository.refreshVideos() } }
                            )

                            MainDestination.FOLDERS -> FoldersScreen(
                                folders = folders,
                                allVideos = allVideos,
                                onVideoClick = { video ->
                                    val index = allVideos.indexOfFirst { it.uri == video.uri }
                                    playerManager.setPlaylist(allVideos, if (index != -1) index else 0)
                                    isPlayingPrivate = false
                                    currentScreen = ScreenState.PLAYER
                                },
                                onFavoriteToggle = { video ->
                                    scope.launch { videoRepository.toggleFavorite(video) }
                                },
                                onRenameVideo = { video, newTitle ->
                                    scope.launch { videoRepository.renameVideo(video, newTitle) }
                                },
                                onDeleteVideo = { video ->
                                    scope.launch { videoRepository.deleteVideo(video) }
                                },
                                onSafFolderAdded = {
                                    scope.launch { videoRepository.refreshVideos() }
                                }
                            )

                            MainDestination.LOCKER -> LockerScreen(
                                lockerManager = lockerManager,
                                onPlayPrivateVideo = { privateVideo, ephemeralFile ->
                                    activePrivateFile = ephemeralFile
                                    val tempVideoItem = VideoItem(
                                        id = privateVideo.id,
                                        uri = Uri.fromFile(ephemeralFile),
                                        title = privateVideo.title,
                                        durationMs = privateVideo.durationMs,
                                        sizeBytes = privateVideo.sizeBytes,
                                        dateAdded = privateVideo.importTimestamp / 1000,
                                        mimeType = privateVideo.mimeType
                                    )
                                    isPlayingPrivate = true
                                    playerManager.setPlaylist(listOf(tempVideoItem), 0)
                                    currentScreen = ScreenState.PLAYER
                                }
                            )

                            MainDestination.SETTINGS -> SettingsScreen(
                                onBack = { currentDestination = MainDestination.HOME },
                                onRescanLibrary = { scope.launch { videoRepository.refreshVideos() } },
                                onClearHistory = { scope.launch { videoRepository.clearHistory() } },
                                onManageFolders = { scope.launch { videoRepository.refreshVideos() } }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        activePrivateFile?.delete()
        activePrivateFile = null
        playerManager.release()
        super.onDestroy()
    }
}
