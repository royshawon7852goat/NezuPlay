package com.nezuplay.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nezuplay.video.ui.theme.AccentRed
import com.nezuplay.video.ui.theme.AmoledBlack
import com.nezuplay.video.ui.theme.DarkSurface
import com.nezuplay.video.ui.theme.TextSecondary

enum class MainDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    VIDEOS("Videos", Icons.Default.VideoLibrary),
    FOLDERS("Folders", Icons.Default.Folder),
    LOCKER("Locker", Icons.Default.Lock),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun NezuBottomNav(
    currentDestination: MainDestination,
    onSelectDestination: (MainDestination) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        MainDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentRed,
                    selectedTextColor = AccentRed,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Color(0x22E53935)
                )
            )
        }
    }
}
