package com.rescate.app

import android.app.Application
import com.rescate.app.util.NotificationHelper
import org.osmdroid.config.Configuration

class RescateApp : Application() {
    override fun onCreate() {
        super.onCreate()
         
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("tiles")
        }
        NotificationHelper.createChannels(this)
    }
}
