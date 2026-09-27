package com.hutube.app.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.hutube.app.data.model.MediaItem
import java.io.File

class DownloadManagerHelper(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun startDownload(media: MediaItem, token: String) {
        val url = "https://www.googleapis.com/drive/v3/files/${media.id}?alt=media"

        // Ensure filename has a video extension — Drive files often don't
        val fileName = if (media.name.contains('.')) {
            media.name
        } else {
            "${media.name}.mp4"
        }

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("Authorization", "Bearer $token")
            setTitle(media.title)
            setDescription("Downloading from HuTube")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "HuTube/$fileName"
            )
        }
        downloadManager.enqueue(request)
    }

    fun getDownloadedFiles(): List<File> {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HuTube")
        if (!dir.exists()) return emptyList()
        // List ALL files in the HuTube directory, not just specific extensions
        return dir.listFiles { file -> file.isFile && file.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
