package com.august.spiritscribe.application

import android.app.Application
import com.august.spiritscribe.utils.ResourceUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpiritScribeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ResourceUtils.init(this)
    }
}
