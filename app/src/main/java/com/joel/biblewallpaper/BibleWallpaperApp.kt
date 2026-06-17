package com.joel.biblewallpaper

import android.app.Application

class BibleWallpaperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WallpaperScheduler.scheduleEvery12Hours(this)
    }
}
