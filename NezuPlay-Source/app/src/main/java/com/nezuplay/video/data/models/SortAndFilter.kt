package com.nezuplay.video.data.models

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_ADDED_DESC("Date (Newest)"),
    DATE_ADDED_ASC("Date (Oldest)"),
    DURATION_DESC("Duration (Longest)"),
    DURATION_ASC("Duration (Shortest)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)"),
    RECENTLY_PLAYED("Recently Played")
}

enum class FilterOption(val displayName: String) {
    ALL("All Videos"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    FAVORITES("Favorites")
}

enum class AspectRatioMode(val displayName: String) {
    FIT("Fit to Screen"),
    FILL("Fill Screen"),
    CROP("16:9 Crop"),
    STRETCH("Stretch"),
    ORIGINAL("100% Original")
}
