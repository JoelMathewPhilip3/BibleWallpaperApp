package com.joel.biblewallpaper

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WallpaperScheduler {
    const val TAG = "bible_widget_work"
    private const val ROTATION = "bible_widget_rotation_v2"
    private const val DOWNLOAD = "bible_widget_download_v2"
    private const val MANUAL_DOWNLOAD = "bible_widget_manual_download_v2"
    private const val MANUAL_NEXT = "bible_widget_manual_next_v2"
    fun migrate(c: Context) {
        val prefs = c.getSharedPreferences("bible_widget_migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("v2", false)) return
        val wm = WorkManager.getInstance(c)
        wm.cancelUniqueWork("bible_wallpaper_photo_download_worker")
        wm.cancelUniqueWork("saved_wallpaper_60_min_rotation_worker")
        // v1 manual jobs had no unique name. Cancel by class tag as well.
        wm.cancelAllWorkByTag("com.joel.biblewallpaper.WallpaperWorker")
        wm.cancelAllWorkByTag("com.joel.biblewallpaper.SavedWallpaperRotationWorker")
        prefs.edit().putBoolean("v2", true).apply()
        // Original PNGs are portrait composites; do not use them as landscape cards.
        // Leave legacy images intact; users can remove them explicitly in the app.
    }
    fun scheduleAll(c: Context) {
        migrate(c)
        val wm = WorkManager.getInstance(c)
        if (!BibleWidgetProvider.hasWidgets(c)) {
            cancelScheduled(c)
            return
        }
        if (WidgetSettings.paused(c)) wm.cancelUniqueWork(ROTATION) else {
            val rotation = PeriodicWorkRequestBuilder<WidgetRotationWorker>(4, TimeUnit.HOURS, 1, TimeUnit.HOURS)
                .setInitialDelay(4, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .addTag(TAG).build()
            wm.enqueueUniquePeriodicWork(ROTATION, ExistingPeriodicWorkPolicy.KEEP, rotation)
        }
        val download = PeriodicWorkRequestBuilder<WidgetDownloadWorker>(24,TimeUnit.HOURS,6,TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true).setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES)
            .addTag(TAG).build()
        wm.enqueueUniquePeriodicWork(DOWNLOAD, ExistingPeriodicWorkPolicy.KEEP, download)
    }
    fun cancelScheduled(c: Context) {
        val wm = WorkManager.getInstance(c)
        listOf(ROTATION, DOWNLOAD, MANUAL_DOWNLOAD, MANUAL_NEXT).forEach { wm.cancelUniqueWork(it) }
    }
    fun downloadNow(c: Context) {
        WorkManager.getInstance(c).enqueueUniqueWork(MANUAL_DOWNLOAD, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetDownloadWorker>()
                .setInputData(workDataOf("manual" to true))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true).build()).addTag(TAG).addTag("manual_download").build())
    }
    fun nextNow(c: Context) {
        WorkManager.getInstance(c).enqueueUniqueWork(MANUAL_NEXT, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetRotationWorker>().setInputData(workDataOf("manual" to true))
                .addTag(TAG).build())
    }
}
