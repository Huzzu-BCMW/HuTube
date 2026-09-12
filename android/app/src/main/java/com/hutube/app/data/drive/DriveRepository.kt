package com.hutube.app.data.drive

import android.util.Log
import com.hutube.app.data.model.Category
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.SeasonItem
import com.hutube.app.data.model.ShowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class CatalogData(
    val anime: List<Any> = emptyList(), // Can contain ShowItem or MediaItem
    val cartoon: List<Any> = emptyList(),
    val series: List<ShowItem> = emptyList(),
    val movies: List<MediaItem> = emptyList(),
    val news: List<MediaItem> = emptyList(),
    val allVideos: List<MediaItem> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null
)

class DriveRepository(private val driveService: GoogleDriveService) {

    companion object {
        private const val TAG = "DriveRepository"
    }

    private val _catalog = MutableStateFlow(CatalogData())
    val catalog: StateFlow<CatalogData> = _catalog

    suspend fun scanDrive() = withContext(Dispatchers.IO) {
        _catalog.value = _catalog.value.copy(isScanning = true, error = null)
        Log.d(TAG, "=== Starting Drive scan ===")

        try {
            val rootFolders = driveService.searchFolders()
            Log.d(TAG, "Found ${rootFolders.size} total folders in Drive")
            rootFolders.forEach { Log.d(TAG, "  Folder: '${it.name}' (${it.id})") }

            val animeFolders = rootFolders.filter { it.name.contains("anime", ignoreCase = true) }
            val cartoonFolders = rootFolders.filter { it.name.contains("cartoon", ignoreCase = true) || it.name.contains("animation", ignoreCase = true) }
            val seriesFolders = rootFolders.filter { it.name.contains("series", ignoreCase = true) || it.name.contains("show", ignoreCase = true) }
            val movieFolders = rootFolders.filter { it.name.contains("movie", ignoreCase = true) || it.name.contains("film", ignoreCase = true) }
            val newsFolders = rootFolders.filter { it.name.contains("funny breaking news", ignoreCase = true) || it.name.contains("news", ignoreCase = true) }

            Log.d(TAG, "Matched: anime=${animeFolders.size}, cartoon=${cartoonFolders.size}, series=${seriesFolders.size}, movies=${movieFolders.size}, news=${newsFolders.size}")

            val movies = mutableListOf<MediaItem>()
            val series = mutableListOf<ShowItem>()
            val anime = mutableListOf<Any>()
            val cartoon = mutableListOf<Any>()
            val news = mutableListOf<MediaItem>()
            val allVideos = mutableListOf<MediaItem>()

            // Scan Movies
            for (mFolder in movieFolders) {
                val children = driveService.listFolderChildren(mFolder.id)
                for (file in children) {
                    if (driveService.isVideo(file)) {
                        val mItem = toMediaItem(file, Category.MOVIES)
                        movies.add(mItem)
                        allVideos.add(mItem)
                    } else if (file.isFolder) {
                        // Movie in subfolder
                        val subFiles = driveService.listFolderChildren(file.id)
                        val video = subFiles.firstOrNull { driveService.isVideo(it) }
                        if (video != null) {
                            val mItem = toMediaItem(video, Category.MOVIES, titleOverride = file.name)
                            movies.add(mItem)
                            allVideos.add(mItem)
                        }
                    }
                }
            }

            // Scan Series
            for (sFolder in seriesFolders) {
                val shows = scanShowsInFolder(sFolder.id, Category.SERIES)
                series.addAll(shows)
                shows.forEach { allVideos.addAll(it.allEpisodes) }
            }

            // Scan Anime
            for (aFolder in animeFolders) {
                val shows = scanShowsInFolder(aFolder.id, Category.ANIME)
                anime.addAll(shows)
                shows.forEach { allVideos.addAll(it.allEpisodes) }

                // Check for single standalone anime movies directly in folder
                val children = driveService.listFolderChildren(aFolder.id)
                val directVideos = children.filter { driveService.isVideo(it) }
                for (v in directVideos) {
                    val aItem = toMediaItem(v, Category.ANIME)
                    anime.add(aItem)
                    allVideos.add(aItem)
                }
            }

            // Scan Cartoon
            for (cFolder in cartoonFolders) {
                val shows = scanShowsInFolder(cFolder.id, Category.CARTOON)
                cartoon.addAll(shows)
                shows.forEach { allVideos.addAll(it.allEpisodes) }

                val children = driveService.listFolderChildren(cFolder.id)
                val directVideos = children.filter { driveService.isVideo(it) }
                for (v in directVideos) {
                    val cItem = toMediaItem(v, Category.CARTOON)
                    cartoon.add(cItem)
                    allVideos.add(cItem)
                }
            }

            // Scan Funny Breaking News
            for (nFolder in newsFolders) {
                val children = driveService.listFolderChildren(nFolder.id)
                for (file in children) {
                    if (driveService.isVideo(file)) {
                        val nItem = toMediaItem(file, Category.NEWS)
                        news.add(nItem)
                        allVideos.add(nItem)
                    }
                }
            }

            Log.d(TAG, "=== Scan complete: anime=${anime.size}, cartoon=${cartoon.size}, series=${series.size}, movies=${movies.size}, news=${news.size}, total=${allVideos.size} ===")

            _catalog.value = CatalogData(
                anime = anime,
                cartoon = cartoon,
                series = series,
                movies = movies,
                news = news,
                allVideos = allVideos,
                isScanning = false,
                error = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "!!! Scan FAILED: ${e.message}", e)
            _catalog.value = _catalog.value.copy(isScanning = false, error = e.localizedMessage)
            e.printStackTrace()
        }
    }

    private suspend fun scanShowsInFolder(parentFolderId: String, category: Category): List<ShowItem> {
        val showItems = mutableListOf<ShowItem>()
        val showFolders = driveService.listFolderChildren(parentFolderId).filter { it.isFolder }

        for (showFolder in showFolders) {
            val showChildren = driveService.listFolderChildren(showFolder.id)
            val subFolders = showChildren.filter { it.isFolder }
            val directVideos = showChildren.filter { driveService.isVideo(it) }

            val seasons = mutableListOf<SeasonItem>()
            val directEpisodes = mutableListOf<MediaItem>()

            if (subFolders.isNotEmpty()) {
                // Has seasons
                var seasonIndex = 1
                for (seasonFolder in subFolders) {
                    val seasonFiles = driveService.listFolderChildren(seasonFolder.id)
                    val seasonEpisodes = seasonFiles.filter { driveService.isVideo(it) }.map {
                        val (sNum, epNum) = driveService.parseEpisodeInfo(it.name)
                        toMediaItem(it, category, showFolder.name, sNum, epNum)
                    }.sortedBy { it.episode ?: 0 }

                    seasons.add(
                        SeasonItem(
                            id = seasonFolder.id,
                            name = seasonFolder.name,
                            seasonNumber = seasonIndex++,
                            episodes = seasonEpisodes
                        )
                    )
                }
            } else {
                // Direct episodes
                directEpisodes.addAll(
                    directVideos.map {
                        val (sNum, epNum) = driveService.parseEpisodeInfo(it.name)
                        toMediaItem(it, category, showFolder.name, sNum, epNum)
                    }.sortedBy { it.episode ?: 0 }
                )
            }

            val firstThumb = seasons.firstOrNull()?.episodes?.firstOrNull()?.thumbnailLink
                ?: directEpisodes.firstOrNull()?.thumbnailLink

            showItems.add(
                ShowItem(
                    id = showFolder.id,
                    title = driveService.cleanTitle(showFolder.name),
                    category = category,
                    thumbnailLink = firstThumb,
                    seasons = seasons,
                    episodes = directEpisodes
                )
            )
        }

        return showItems
    }

    private fun toMediaItem(
        file: DriveFile,
        category: Category,
        seriesTitle: String? = null,
        season: Int = 1,
        episode: Int? = null,
        titleOverride: String? = null
    ): MediaItem {
        return MediaItem(
            id = file.id,
            name = file.name,
            title = titleOverride ?: driveService.cleanTitle(file.name),
            seriesTitle = seriesTitle,
            category = category,
            season = season,
            episode = episode,
            thumbnailLink = file.thumbnailLink,
            durationMillis = file.durationMillis,
            resolution = file.resolution,
            size = file.size,
            mimeType = file.mimeType
        )
    }
}
