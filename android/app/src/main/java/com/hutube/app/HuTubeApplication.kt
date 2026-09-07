package com.hutube.app

import android.app.Application
import com.hutube.app.data.storage.WatchHistoryManager

class HuTubeApplication : Application() {

    lateinit var watchHistoryManager: WatchHistoryManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        watchHistoryManager = WatchHistoryManager(this)
    }

    companion object {
        lateinit var instance: HuTubeApplication
            private set
    }
}
