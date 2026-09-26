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
            
            // If we found root folders but 0 of them matched our categories, throw a descriptive error
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
                val movieJobs = movieFolders.map { mFolder ->
                    async {
                        val mFolderVideos = mutableListOf<MediaItem>()
                        val children = driveService.listFolderChildren(mFolder.id)
                        for (file in children) {
                            if (driveService.isVideo(file)) {
                                mFolderVideos.add(toMediaItem(file, Category.MOVIES))
                            } else if (file.isFolder) {
                                val subFiles = driveService.listFolderChildren(file.id)
                                val video = subFiles.firstOrNull { driveService.isVideo(it) }
                                if (video != null) {
                                    mFolderVideos.add(toMediaItem(video, Category.MOVIES, titleOverride = file.name))
                                }
                            }
                        }
                        mFolderVideos
                    }
                }

                val seriesJobs = seriesFolders.map { async { scanShowsInFolder(it.id, Category.SERIES) } }
                val animeJobs = animeFolders.map { async { scanShowsInFolder(it.id, Category.ANIME) } }
                val cartoonJobs = cartoonFolders.map { async { scanShowsInFolder(it.id, Category.CARTOON) } }
                
                val newsJobs = newsFolders.map { nFolder ->
                    async {
                        val nFolderVideos = mutableListOf<MediaItem>()
                        val children = driveService.listFolderChildren(nFolder.id)
                        for (file in children) {
                            if (driveService.isVideo(file)) {
                                nFolderVideos.add(toMediaItem(file, Category.NEWS))
                            }
                        }
                        nFolderVideos
                    }
                }

                movieJobs.awaitAll().forEach { mList ->
                    movies.addAll(mList)
                    allVideos.addAll(mList)
                }

                seriesJobs.awaitAll().forEach { sList ->
                    series.addAll(sList)
                    sList.forEach { allVideos.addAll(it.allEpisodes) }
                }

                animeJobs.awaitAll().forEach { aList ->
                    anime.addAll(aList)
                    aList.forEach { allVideos.addAll(it.allEpisodes) }
                }
                
                // Anime single movies
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

                cartoonJobs.awaitAll().forEach { cList ->
                    cartoon.addAll(cList)
                    cList.forEach { allVideos.addAll(it.allEpisodes) }
                }

                // Cartoon single movies
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

                newsJobs.awaitAll().forEach { nList ->
                    news.addAll(nList)
                    allVideos.addAll(nList)
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

    private suspend fun scanShowsInFolder(parentFolderId: String, category: Category): List<ShowItem> = coroutineScope {
        val showFolders = driveService.listFolderChildren(parentFolderId).filter { it.isFolder }

        val deferredShows = showFolders.map { showFolder ->
            async {
                val showChildren = driveService.listFolderChildren(showFolder.id)
                val directVideos = showChildren.filter { driveService.isVideo(it) }
                val subFolders = showChildren.filter { it.isFolder }

                val seasons = mutableListOf<SeasonItem>()
                val directEpisodes = mutableListOf<MediaItem>()

                // Recursive function to find all folders that contain videos
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

                var seasonIndex = 1
                val subFolderVideoFolders = subFolders.map { sub -> async { findVideoFolders(sub) } }.awaitAll()
                for (videoFolders in subFolderVideoFolders) {
                    for ((seasonFolder, videos) in videoFolders) {
                        val seasonEpisodes = videos.map {
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
                }

                // Direct episodes
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



