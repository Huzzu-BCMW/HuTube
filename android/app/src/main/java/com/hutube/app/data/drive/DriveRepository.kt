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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class CatalogData(
    val anime: List<Any> = emptyList(),
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

            val animeFolders = rootFolders.filter { it.name.contains("anime", ignoreCase = true) }
            val cartoonFolders = rootFolders.filter { it.name.contains("cartoon", ignoreCase = true) || it.name.contains("animation", ignoreCase = true) }
            val seriesFolders = rootFolders.filter { it.name.contains("series", ignoreCase = true) || it.name.contains("show", ignoreCase = true) }
            val movieFolders = rootFolders.filter { it.name.contains("movie", ignoreCase = true) || it.name.contains("film", ignoreCase = true) }
            val newsFolders = rootFolders.filter { it.name.contains("funny breaking news", ignoreCase = true) || it.name.contains("news", ignoreCase = true) }

            if (rootFolders.isNotEmpty() && animeFolders.isEmpty() && cartoonFolders.isEmpty() && seriesFolders.isEmpty() && movieFolders.isEmpty() && newsFolders.isEmpty()) {
                val folderNames = rootFolders.take(15).joinToString(", ") { it.name }
                throw Exception("Found folders, but none matched category names (Anime/Cartoon/Series/Movies/News). Folders found: $folderNames...")
            }

            val movies = mutableListOf<MediaItem>()
            val series = mutableListOf<ShowItem>()
            val anime = mutableListOf<Any>()
            val cartoon = mutableListOf<Any>()
            val news = mutableListOf<MediaItem>()
            val allVideos = mutableListOf<MediaItem>()

            coroutineScope {
                // === MOVIES ===
                val movieJobs = movieFolders.map { mFolder ->
                    async {
                        val mFolderVideos = mutableListOf<MediaItem>()
                        val children = driveService.listFolderChildren(mFolder.id)
                        for (file in children) {
                            if (driveService.isVideo(file)) {
                                mFolderVideos.add(toMediaItem(file, Category.MOVIES))
                            } else if (file.isFolder) {
                                // Movie in its own folder — use folder name as title
                                val subFiles = driveService.listFolderChildren(file.id)
                                val video = subFiles.firstOrNull { driveService.isVideo(it) }
                                if (video != null) {
                                    mFolderVideos.add(toMediaItem(video, Category.MOVIES, titleOverride = driveService.cleanTitle(file.name)))
                                }
                            }
                        }
                        mFolderVideos
                    }
                }

                // === SERIES / ANIME / CARTOON (with smart merging) ===
                val seriesJobs = seriesFolders.map { async { scanShowsInFolder(it.id, Category.SERIES) } }
                val animeJobs = animeFolders.map { async { scanShowsInFolder(it.id, Category.ANIME) } }
                val cartoonJobs = cartoonFolders.map { async { scanShowsInFolder(it.id, Category.CARTOON) } }

                // === NEWS ===
                val newsJobs = newsFolders.map { nFolder ->
                    async {
                        val children = driveService.listFolderChildren(nFolder.id)
                        children.filter { driveService.isVideo(it) }.map { toMediaItem(it, Category.NEWS) }
                    }
                }

                // Collect movies
                movieJobs.awaitAll().forEach { mList ->
                    movies.addAll(mList)
                    allVideos.addAll(mList)
                }

                // Collect series — merge duplicates by cleaned title
                val rawSeries = seriesJobs.awaitAll().flatten()
                val mergedSeries = mergeShowsByTitle(rawSeries)
                series.addAll(mergedSeries)
                mergedSeries.forEach { allVideos.addAll(it.allEpisodes) }

                // Collect anime shows
                val rawAnimeShows = animeJobs.awaitAll().flatten()
                val mergedAnimeShows = mergeShowsByTitle(rawAnimeShows)
                anime.addAll(mergedAnimeShows)
                mergedAnimeShows.forEach { allVideos.addAll(it.allEpisodes) }

                // Anime standalone videos (loose video files in anime folders)
                val animeMovieJobs = animeFolders.map { aFolder ->
                    async {
                        val children = driveService.listFolderChildren(aFolder.id)
                        children.filter { driveService.isVideo(it) }.map { toMediaItem(it, Category.ANIME) }
                    }
                }
                animeMovieJobs.awaitAll().forEach { aList ->
                    anime.addAll(aList)
                    allVideos.addAll(aList)
                }

                // Collect cartoon shows
                val rawCartoonShows = cartoonJobs.awaitAll().flatten()
                val mergedCartoonShows = mergeShowsByTitle(rawCartoonShows)
                cartoon.addAll(mergedCartoonShows)
                mergedCartoonShows.forEach { allVideos.addAll(it.allEpisodes) }

                // Cartoon standalone videos
                val cartoonMovieJobs = cartoonFolders.map { cFolder ->
                    async {
                        val children = driveService.listFolderChildren(cFolder.id)
                        children.filter { driveService.isVideo(it) }.map { toMediaItem(it, Category.CARTOON) }
                    }
                }
                cartoonMovieJobs.awaitAll().forEach { cList ->
                    cartoon.addAll(cList)
                    allVideos.addAll(cList)
                }

                // Collect news
                newsJobs.awaitAll().forEach { nList ->
                    news.addAll(nList)
                    allVideos.addAll(nList)
                }
            }

            Log.d(TAG, "=== Scan complete: anime=${anime.size}, cartoon=${cartoon.size}, series=${series.size}, movies=${movies.size}, news=${news.size}, total=${allVideos.size} ===")

            _catalog.value = CatalogData(
                anime = anime.sortedBy { if (it is ShowItem) it.title.lowercase() else (it as MediaItem).title.lowercase() },
                cartoon = cartoon.sortedBy { if (it is ShowItem) it.title.lowercase() else (it as MediaItem).title.lowercase() },
                series = series.sortedBy { it.title.lowercase() },
                movies = movies.sortedBy { it.title.lowercase() },
                news = news.sortedBy { it.title.lowercase() },
                allVideos = allVideos.sortedBy { it.title.lowercase() },
                isScanning = false,
                error = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "!!! Scan FAILED: ${e.message}", e)
            _catalog.value = _catalog.value.copy(isScanning = false, error = e.localizedMessage)
        }
    }

    /**
     * Merges ShowItems that have the same cleaned title.
     * 
     * This handles the case where you have multiple folders for the same show
     * (e.g. two "Shinchan" folders with different dubs). Instead of showing
     * two separate entries, we merge them into one ShowItem where each folder
     * becomes a separate "version" season.
     *
     * Each duplicate folder's content becomes its own season group, labeled
     * with the original folder name to distinguish them (e.g., "Shinchan (Hindi Dub)",
     * "Shinchan (Tamil Dub)").
     */
    private fun mergeShowsByTitle(shows: List<ShowItem>): List<ShowItem> {
        // Group shows by normalized title (lowercase, trimmed)
        val grouped = shows.groupBy { it.title.lowercase().trim() }

        return grouped.map { (_, group) ->
            if (group.size == 1) {
                // Only one folder with this name — no merging needed
                group.first()
            } else {
                // Multiple folders with same/similar name — merge them
                Log.d(TAG, "Merging ${group.size} folders for '${group.first().title}'")

                val mergedSeasons = mutableListOf<SeasonItem>()
                var seasonCounter = 1

                for ((folderIdx, show) in group.withIndex()) {
                    if (show.seasons.isNotEmpty()) {
                        // This folder has seasons — add each with a version prefix if there are duplicates
                        for (season in show.seasons) {
                            val label = if (group.size > 1) {
                                "Version ${folderIdx + 1} - ${season.name}"
                            } else {
                                season.name
                            }
                            mergedSeasons.add(
                                season.copy(
                                    name = label,
                                    seasonNumber = seasonCounter++
                                )
                            )
                        }
                    } else if (show.episodes.isNotEmpty()) {
                        // This folder has loose episodes — wrap them as a season
                        val label = if (group.size > 1) "Version ${folderIdx + 1}" else "Episodes"
                        mergedSeasons.add(
                            SeasonItem(
                                id = show.id,
                                name = label,
                                seasonNumber = seasonCounter++,
                                episodes = show.episodes
                            )
                        )
                    }
                }

                // Pick the best thumbnail (prefer one that exists)
                val bestThumb = group.firstOrNull { !it.thumbnailLink.isNullOrEmpty() }?.thumbnailLink
                    ?: group.first().thumbnailLink

                ShowItem(
                    id = group.first().id,
                    title = group.first().title,
                    category = group.first().category,
                    thumbnailLink = bestThumb,
                    seasons = mergedSeasons,
                    episodes = emptyList() // All episodes are now in seasons
                )
            }
        }
    }

    private suspend fun scanShowsInFolder(parentFolderId: String, category: Category): List<ShowItem> = coroutineScope {
        val showFolders = driveService.listFolderChildren(parentFolderId).filter { it.isFolder }

        val deferredShows = showFolders.map { showFolder ->
            async {
                val showChildren = driveService.listFolderChildren(showFolder.id)
                val directVideos = showChildren.filter { driveService.isVideo(it) }
                val subFolders = showChildren.filter { it.isFolder }

                val seasons = mutableListOf<SeasonItem>()
                val directEpisodes = mutableListOf<MediaItem>()

                suspend fun findVideoFolders(folder: DriveFile): List<Pair<DriveFile, List<DriveFile>>> {
                    val result = mutableListOf<Pair<DriveFile, List<DriveFile>>>()
                    val children = driveService.listFolderChildren(folder.id)
                    val videos = children.filter { driveService.isVideo(it) }
                    if (videos.isNotEmpty()) {
                        result.add(Pair(folder, videos))
                    }
                    val subs = children.filter { it.isFolder }
                    val subResults = subs.map { sub -> async { findVideoFolders(sub) } }.awaitAll()
                    subResults.forEach { result.addAll(it) }
                    return result
                }

                // Scan subfolders for seasons
                var seasonIndex = 1
                val subFolderVideoFolders = subFolders
                    .sortedBy { it.name.lowercase() } // Sort folders alphabetically
                    .map { sub -> async { Pair(sub.name, findVideoFolders(sub)) } }
                    .awaitAll()

                for ((parentName, videoFolders) in subFolderVideoFolders) {
                    // Sort video folders by name for consistent season ordering
                    val sortedVideoFolders = videoFolders.sortedBy { it.first.name.lowercase() }
                    for ((seasonFolder, videos) in sortedVideoFolders) {
                        val seasonEpisodes = videos.map {
                            val (sNum, epNum) = driveService.parseEpisodeInfo(it.name)
                            toMediaItem(it, category, showFolder.name, sNum, epNum)
                        }.sortedBy { it.episode ?: 0 }

                        // Use a smarter season name — detect if it's a season or just a subfolder
                        val seasonName = detectSeasonName(seasonFolder.name, seasonIndex)

                        seasons.add(
                            SeasonItem(
                                id = seasonFolder.id,
                                name = seasonName,
                                seasonNumber = seasonIndex++,
                                episodes = seasonEpisodes
                            )
                        )
                    }
                }

                // Direct episodes in show root
                directEpisodes.addAll(
                    directVideos.map {
                        val (sNum, epNum) = driveService.parseEpisodeInfo(it.name)
                        toMediaItem(it, category, showFolder.name, sNum, epNum)
                    }.sortedBy { it.episode ?: 0 }
                )

                if (seasons.isEmpty() && directEpisodes.isEmpty()) return@async null

                val firstThumb = seasons.firstOrNull()?.episodes?.firstOrNull()?.thumbnailLink
                    ?: directEpisodes.firstOrNull()?.thumbnailLink

                ShowItem(
                    id = showFolder.id,
                    title = driveService.cleanTitle(showFolder.name),
                    category = category,
                    thumbnailLink = firstThumb,
                    seasons = seasons,
                    episodes = directEpisodes
                )
            }
        }

        deferredShows.awaitAll().filterNotNull()
    }

    /**
     * Detect a nice season name from folder name.
     * "Season 1" -> "Season 1"
     * "S01" -> "Season 1"
     * "Hindi Dub" -> "Hindi Dub"
     * Random folder name -> keep as-is but clean it
     */
    private fun detectSeasonName(folderName: String, fallbackIndex: Int): String {
        val cleaned = driveService.cleanTitle(folderName)
        val lower = cleaned.lowercase()

        // Already says "Season X"
        if (lower.startsWith("season")) return cleaned

        // S01, S1 format
        val sMatch = Regex("^s(\\d+)$", RegexOption.IGNORE_CASE).find(lower)
        if (sMatch != null) {
            return "Season ${sMatch.groupValues[1].toInt()}"
        }

        // Just a number
        if (lower.matches(Regex("^\\d+$"))) {
            return "Season ${lower.toInt()}"
        }

        // Contains "dub" or language hint — keep the folder name as-is
        if (lower.contains("dub") || lower.contains("sub") || lower.contains("hindi") ||
            lower.contains("english") || lower.contains("tamil") || lower.contains("japanese")) {
            return cleaned
        }

        // Otherwise use the folder name
        return cleaned
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
            seriesTitle = seriesTitle?.let { driveService.cleanTitle(it) },
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
