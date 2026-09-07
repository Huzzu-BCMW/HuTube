package com.hutube.app.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory

/**
 * DriveMediaSourceFactory - Employs the X-plore / RS File Manager streaming technique.
 *
 * Instead of relying on Google Drive's web preview transcoder (which fails frequently),
 * this factory creates a direct HTTP Range-streaming source from Google Drive's REST API:
 * "https://www.googleapis.com/drive/v3/files/{fileId}?alt=media"
 * passing the Google OAuth Bearer token in the request headers.
 */
class DriveMediaSourceFactory(
    private val context: Context,
    private val tokenProvider: () -> String?
) {

    fun createMediaSource(fileId: String): MediaSource {
        val streamUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val token = tokenProvider() ?: ""

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setDefaultRequestProperties(
                mapOf(
                    "Authorization" to "Bearer $token"
                )
            )

        // Support Matroska (MKV), MP4, WebM, AVI, and TS files
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(fileId)
            .build()

        return ProgressiveMediaSource.Factory(httpDataSourceFactory, extractorsFactory)
            .createMediaSource(mediaItem)
    }
}
