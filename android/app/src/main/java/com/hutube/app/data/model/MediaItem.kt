package com.hutube.app.data.model

import java.io.Serializable

enum class Category(val title: String, val icon: String) {
    ANIME("Anime", "🎌"),
    CARTOON("Cartoon", "🎨"),
    SERIES("Series", "📺"),
    MOVIES("Movies", "🎬"),
    NEWS("Funny Breaking News", "📰"),
    OTHER("All Files", "📁")
}

data class MediaItem(
    val id: String,
    val name: String,
    val title: String,
    val seriesTitle: String? = null,
    val category: Category = Category.MOVIES,
    val season: Int = 1,
    val episode: Int? = null,
    val thumbnailLink: String? = null,
    val durationMillis: Long? = null,
    val resolution: String? = null,
    val size: Long = 0,
    val mimeType: String? = null
) : Serializable {

    val formattedDuration: String
        get() {
            val ms = durationMillis ?: return ""
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%dh %02dm", hours, minutes)
            } else {
                String.format("%dm %02ds", minutes, seconds)
            }
        }
}

data class SeasonItem(
    val id: String,
    val name: String,
    val seasonNumber: Int,
    val episodes: List<MediaItem>
) : Serializable

data class ShowItem(
    val id: String,
    val title: String,
    val category: Category,
    val thumbnailLink: String? = null,
    val seasons: List<SeasonItem> = emptyList(),
    val episodes: List<MediaItem> = emptyList()
) : Serializable {

    val allEpisodes: List<MediaItem>
        get() = if (seasons.isNotEmpty()) seasons.flatMap { it.episodes } else episodes

    val totalEpisodesCount: Int
        get() = allEpisodes.size
}

data class WatchProgress(
    val fileId: String,
    val title: String,
    val seriesTitle: String?,
    val thumbnailLink: String?,
    val season: Int,
    val episode: Int?,
    val currentPositionMs: Long,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    val progressPercent: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
