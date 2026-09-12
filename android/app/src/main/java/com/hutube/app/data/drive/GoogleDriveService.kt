package com.hutube.app.data.drive

import android.util.Log
import com.hutube.app.data.model.Category
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.SeasonItem
import com.hutube.app.data.model.ShowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern

class GoogleDriveService(private val tokenProvider: () -> String?) {

    companion object {
        private const val TAG = "GoogleDriveService"
    }

    private val client = OkHttpClient.Builder().build()

    private val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".flv", ".m4v", ".ts")

    suspend fun listFolderChildren(folderId: String): List<DriveFile> = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext emptyList()
        val query = "'$folderId' in parents and trashed = false"
        fetchFiles(token, query)
    }

    suspend fun searchFolders(): List<DriveFile> = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        Log.d(TAG, "searchFolders: token=${if (token != null) "${token.take(10)}..." else "NULL"}")
        if (token == null) return@withContext emptyList()
        val query = "mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        fetchFiles(token, query)
    }

    private fun fetchFiles(token: String, query: String): List<DriveFile> {
        val result = mutableListOf<DriveFile>()
        var pageToken: String? = null

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val fields = URLEncoder.encode("nextPageToken, files(id, name, mimeType, size, thumbnailLink, hasThumbnail, videoMediaMetadata)", "UTF-8")

        do {
            val urlBuilder = StringBuilder("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&pageSize=100&fields=$fields&orderBy=name natural")
            if (pageToken != null) {
                urlBuilder.append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Authorization", "Bearer $token")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        Log.e(TAG, "API error (${response.code}): $errorBody")
                        throw Exception("Google Drive API Error (${response.code}): $errorBody")
                    }
                    val body = response.body?.string() ?: return result
                    val json = JSONObject(body)
                    val filesArray = json.optJSONArray("files") ?: return result
                    Log.d(TAG, "fetchFiles page: got ${filesArray.length()} items")

                    for (i in 0 until filesArray.length()) {
                        val obj = filesArray.getJSONObject(i)
                        val id = obj.getString("id")
                        val name = obj.getString("name")
                        val mimeType = obj.optString("mimeType", "")
                        val size = obj.optLong("size", 0)
                        val thumbnailLink = if (obj.has("thumbnailLink")) obj.getString("thumbnailLink") else null

                        var durationMillis: Long? = null
                        var resolution: String? = null
                        if (obj.has("videoMediaMetadata")) {
                            val vMeta = obj.getJSONObject("videoMediaMetadata")
                            if (vMeta.has("durationMillis")) {
                                durationMillis = vMeta.getLong("durationMillis")
                            }
                            val width = vMeta.optInt("width", 0)
                            val height = vMeta.optInt("height", 0)
                            resolution = when {
                                height >= 2160 || width >= 3840 -> "4K UHD"
                                height >= 1080 || width >= 1920 -> "1080p"
                                height >= 720 || width >= 1280 -> "720p"
                                height > 0 -> "${height}p"
                                else -> null
                            }
                        }

                        result.add(
                            DriveFile(
                                id = id,
                                name = name,
                                mimeType = mimeType,
                                size = size,
                                thumbnailLink = thumbnailLink,
                                durationMillis = durationMillis,
                                resolution = resolution
                            )
                        )
                    }

                    pageToken = json.optString("nextPageToken", null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception during API call", e)
                throw e
            }
        } while (pageToken != null)

        return result
    }

    fun isVideo(file: DriveFile): Boolean {
        if (file.mimeType.startsWith("video/")) return true
        val lower = file.name.lowercase()
        return videoExtensions.any { lower.endsWith(it) }
    }

    fun cleanTitle(filename: String): String {
        var name = filename.replace(Regex("\\.[^/.]+$"), "")
        name = name.replace(Regex("[._]"), " ")
        return name.trim()
    }

    fun parseEpisodeInfo(filename: String): Pair<Int, Int?> {
        val lower = filename.lowercase()
        // Matches S01E02, s1e2
        val sMatcher = Pattern.compile("s(\\d+)\\s*e(\\d+)").matcher(lower)
        if (sMatcher.find()) {
            return Pair(sMatcher.group(1)?.toIntOrNull() ?: 1, sMatcher.group(2)?.toIntOrNull())
        }

        // Matches Ep 12, Episode 12
        val epMatcher = Pattern.compile("(?:episode|ep|e)[.\\s_-]*(\\d+)").matcher(lower)
        if (epMatcher.find()) {
            return Pair(1, epMatcher.group(1)?.toIntOrNull())
        }

        // Leading digits
        val leadMatcher = Pattern.compile("^(\\d{1,3})\\b").matcher(lower)
        if (leadMatcher.find()) {
            return Pair(1, leadMatcher.group(1)?.toIntOrNull())
        }

        return Pair(1, null)
    }
}

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val thumbnailLink: String?,
    val durationMillis: Long?,
    val resolution: String?
) {
    val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"
}
