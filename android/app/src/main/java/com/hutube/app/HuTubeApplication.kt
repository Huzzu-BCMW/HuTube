package com.hutube.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.hutube.app.data.storage.WatchHistoryManager
import okhttp3.OkHttpClient

class HuTubeApplication : Application(), ImageLoaderFactory {

    lateinit var watchHistoryManager: WatchHistoryManager
        private set

    // Current access token — set by MainActivity after auth, read by Coil interceptor
    @Volatile
    var currentAccessToken: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        watchHistoryManager = WatchHistoryManager(this)
    }

    /**
     * Provides a Coil ImageLoader that injects the Google Drive Bearer token
     * into every image request. This is what makes thumbnails actually load —
     * Drive thumbnail URLs require authorization.
     */
    override fun newImageLoader(): ImageLoader {
        val authClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = currentAccessToken
                val request = if (!token.isNullOrEmpty()) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(authClient)
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: HuTubeApplication
            private set
    }
}
