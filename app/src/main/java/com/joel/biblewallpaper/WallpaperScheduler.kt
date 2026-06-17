package com.joel.biblewallpaper

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WallpaperScheduler {
    private const val PHOTO_DOWNLOAD_WORK_NAME = "bible_wallpaper_photo_download_worker"
    private const val SAVED_ROTATION_WORK_NAME = "saved_wallpaper_20_min_rotation_worker"

    fun scheduleEvery8Hours(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(
            8,
            TimeUnit.HOURS,
            2,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PHOTO_DOWNLOAD_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleSavedWallpaperRotationEvery20Minutes(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SavedWallpaperRotationWorker>(
            20,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SAVED_ROTATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleAll(context: Context) {
        scheduleEvery8Hours(context)
        scheduleSavedWallpaperRotationEvery20Minutes(context)
    }

    fun runOnceNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
