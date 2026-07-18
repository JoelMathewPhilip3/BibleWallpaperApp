package com.joel.biblewallpaper

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WallpaperScheduler {
    private const val PHOTO_DOWNLOAD_WORK_NAME = "bible_wallpaper_photo_download_worker"
    private const val SAVED_ROTATION_WORK_NAME = "saved_wallpaper_60_min_rotation_worker"

    private const val PREFS = "wallpaper_settings"
    private const val NEW_WALLPAPER_CREATION_ENABLED = "new_wallpaper_creation_enabled"

    fun isNewWallpaperCreationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(NEW_WALLPAPER_CREATION_ENABLED, true)
    }

    fun stopNewWallpaperCreation(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(NEW_WALLPAPER_CREATION_ENABLED, false)
            .apply()

        WorkManager.getInstance(context).cancelUniqueWork(PHOTO_DOWNLOAD_WORK_NAME)
    }

    fun startNewWallpaperCreation(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(NEW_WALLPAPER_CREATION_ENABLED, true)
            .apply()

        scheduleEvery8Hours(context)
    }

    fun scheduleEvery8Hours(context: Context) {
        if (!isNewWallpaperCreationEnabled(context)) return

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

    fun scheduleNextSavedWallpaperRotation(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<SavedWallpaperRotationWorker>()
            .setInitialDelay(60, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SAVED_ROTATION_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleNextSavedWallpaperRotationFromWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<SavedWallpaperRotationWorker>()
            .setInitialDelay(60, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SAVED_ROTATION_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun scheduleAll(context: Context) {
        scheduleEvery8Hours(context)
        scheduleNextSavedWallpaperRotation(context)
    }

    fun runOnceNow(context: Context) {
        if (!isNewWallpaperCreationEnabled(context)) return

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
