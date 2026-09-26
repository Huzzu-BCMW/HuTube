package com.hutube.app.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.hutube.app.data.model.MediaItem
import java.io.File

class DownloadManagerHelper(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun startDownload(media: MediaItem, token: String) {
        val url = "https://www.googleapis.com/drive/v3/files/${media.id}?alt=media"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("Authorization", "Bearer $token")
            setTitle(media.title)
            setDescription("Downloading from HuTube")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            // Save to public Movies/HuTube directory
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES, 
                "HuTube/${media.name}"
            )
        }
        downloadManager.enqueue(request)
    }

    fun getDownloadedFiles(): List<File> {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HuTube")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file ->
            file.isFile && file.name.matches(Regex(".*\\.(mp4|mkv|webm|ts|avi)$", RegexOption.IGNORE_CASE))
        }?.toList() ?: emptyList()
    }
}
