package com.nezuplay.video.ui.videos

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nezuplay.video.data.models.FilterOption
import com.nezuplay.video.data.models.SortOption
import com.nezuplay.video.data.models.VideoItem
import com.nezuplay.video.ui.components.DeleteConfirmationDialog
import com.nezuplay.video.ui.components.RenameDialog
import com.nezuplay.video.ui.components.VideoCard
import com.nezuplay.video.ui.components.VideoInfoDialog
import com.nezuplay.video.ui.theme.AccentRed
import com.nezuplay.video.ui.theme.AmoledBlack
import com.nezuplay.video.ui.theme.CardDark
import com.nezuplay.video.ui.theme.TextPrimary
import com.nezuplay.video.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onFavoriteToggle: (VideoItem) -> Unit,
    onRenameVideo: (VideoItem, String) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit
) {
    var isGrid by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(FilterOption.ALL) }
    var selectedSort by remember { mutableStateOf(SortOption.DATE_ADDED_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var videoForInfo by remember { mutableStateOf<VideoItem?>(null) }
    var videoForRename by remember { mutableStateOf<VideoItem?>(null) }
    var videoForDelete by remember { mutableStateOf<VideoItem?>(null) }

    // Apply Filter
    val filteredVideos = remember(videos, selectedFilter) {
        when (selectedFilter) {
            FilterOption.ALL -> videos
            FilterOption.RECENTLY_ADDED -> videos.sortedByDescending { it.dateAdded }.take(20)
            FilterOption.RECENTLY_PLAYED -> videos.filter { it.lastPositionMs > 0 }.sortedByDescending { it.lastPlayedTimestamp }
            FilterOption.FAVORITES -> videos.filter { it.isFavorite }
        }
    }

    // Apply Sort
    val sortedVideos = remember(filteredVideos, selectedSort) {
        when (selectedSort) {
            SortOption.NAME_ASC -> filteredVideos.sortedBy { it.title.lowercase(Locale.ROOT) }
            SortOption.NAME_DESC -> filteredVideos.sortedByDescending { it.title.lowercase(Locale.ROOT) }
            SortOption.DATE_ADDED_DESC -> filteredVideos.sortedByDescending { it.dateAdded }
            SortOption.DATE_ADDED_ASC -> filteredVideos.sortedBy { it.dateAdded }
            SortOption.DURATION_DESC -> filteredVideos.sortedByDescending { it.durationMs }
            SortOption.DURATION_ASC -> filteredVideos.sortedBy { it.durationMs }
            SortOption.SIZE_DESC -> filteredVideos.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> filteredVideos.sortedBy { it.sizeBytes }
            SortOption.RECENTLY_PLAYED -> filteredVideos.sortedByDescending { it.lastPlayedTimestamp }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Video Library",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "${sortedVideos.size} videos",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Search
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }

            // Grid / List Toggle
            IconButton(onClick = { isGrid = !isGrid }) {
                Icon(
                    imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = Color.White
                )
            }

            // Sort Menu Button
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = AccentRed)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(CardDark)
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    color = if (selectedSort == option) AccentRed else TextPrimary,
                                    fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedSort = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(FilterOption.values()) { option ->
                val isSelected = selectedFilter == option
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = option },
                    label = {
                        Text(
                            text = option.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentRed,
                        selectedLabelColor = Color.White,
                        containerColor = CardDark,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Videos Content: Grid or List
        if (sortedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No videos found",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant storage permission or rescan your device",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan Library", color = Color.White)
                    }
                }
            }
        } else if (isGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedVideos, key = { it.uri.toString() }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onFavoriteToggle = { onFavoriteToggle(video) },
                        onRenameClick = { videoForRename = video },
                        onDeleteClick = { videoForDelete = video },
                        onInfoClick = { videoForInfo = video },
                        isGrid = true
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedVideos, key = { it.uri.toString() }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onFavoriteToggle = { onFavoriteToggle(video) },
                        onRenameClick = { videoForRename = video },
                        onDeleteClick = { videoForDelete = video },
                        onInfoClick = { videoForInfo = video },
                        isGrid = false
                    )
                }
            }
        }
    }

    // Dialogs
    videoForInfo?.let { video ->
        VideoInfoDialog(video = video, onDismiss = { videoForInfo = null })
    }

    videoForRename?.let { video ->
        RenameDialog(
            currentTitle = video.title,
            onDismiss = { videoForRename = null },
            onConfirm = { newTitle ->
                onRenameVideo(video, newTitle)
                videoForRename = null
            }
        )
    }

    videoForDelete?.let { video ->
        DeleteConfirmationDialog(
            title = video.title,
            onDismiss = { videoForDelete = null },
            onConfirm = {
                onDeleteVideo(video)
                videoForDelete = null
            }
        )
    }
}
