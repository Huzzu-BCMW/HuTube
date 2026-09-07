package com.hutube.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.WatchProgress
import org.json.JSONArray
import org.json.JSONObject

class WatchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hutube_watch_history", Context.MODE_PRIVATE)

    fun saveProgress(media: MediaItem, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0 || positionMs <= 0) return

        val percent = (positionMs.toFloat() / durationMs.toFloat())
        val history = getHistory().toMutableList()

        // If completed (> 95%), remove from continue watching
        if (percent >= 0.95f) {
            history.removeAll { it.fileId == media.id }
            saveList(history)
            return
        }

        // Don't save if watched less than 10 seconds
        if (positionMs < 10000) return

        val item = WatchProgress(
            fileId = media.id,
            title = media.title,
            seriesTitle = media.seriesTitle,
            thumbnailLink = media.thumbnailLink,
            season = media.season,
            episode = media.episode,
            currentPositionMs = positionMs,
            durationMs = durationMs
        )

        history.removeAll { it.fileId == media.id }
        history.add(0, item)

        // Keep maximum 25 items
        saveList(history.take(25))
    }

    fun getProgress(fileId: String): Long {
        return getHistory().find { it.fileId == fileId }?.currentPositionMs ?: 0L
    }

    fun getHistory(): List<WatchProgress> {
        val jsonStr = prefs.getString("history_items", null) ?: return emptyList()
        val list = mutableListOf<WatchProgress>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WatchProgress(
                        fileId = obj.getString("fileId"),
                        title = obj.getString("title"),
                        seriesTitle = if (obj.has("seriesTitle") && !obj.isNull("seriesTitle")) obj.getString("seriesTitle") else null,
                        thumbnailLink = if (obj.has("thumbnailLink") && !obj.isNull("thumbnailLink")) obj.getString("thumbnailLink") else null,
                        season = obj.optInt("season", 1),
                        episode = if (obj.has("episode") && !obj.isNull("episode")) obj.getInt("episode") else null,
                        currentPositionMs = obj.getLong("currentPositionMs"),
                        durationMs = obj.getLong("durationMs"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun remove(fileId: String) {
        val list = getHistory().toMutableList()
        list.removeAll { it.fileId == fileId }
        saveList(list)
    }

    private fun saveList(list: List<WatchProgress>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("fileId", item.fileId)
                put("title", item.title)
                put("seriesTitle", item.seriesTitle)
                put("thumbnailLink", item.thumbnailLink)
                put("season", item.season)
                put("episode", item.episode)
                put("currentPositionMs", item.currentPositionMs)
                put("durationMs", item.durationMs)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("history_items", arr.toString()).apply()
    }
}
