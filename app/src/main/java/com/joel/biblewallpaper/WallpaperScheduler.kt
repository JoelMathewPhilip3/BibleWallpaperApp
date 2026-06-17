package com.joel.biblewallpaper

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WallpaperScheduler {
    private const val WORK_NAME = "bible_wallpaper_12_hour_worker"

    fun scheduleEvery12Hours(context: Context) {
        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runOnceNow(context: Context) {
        WorkManager.getInstance(context).enqueue(
            androidx.work.OneTimeWorkRequestBuilder<WallpaperWorker>().build()
        )
    }
}
