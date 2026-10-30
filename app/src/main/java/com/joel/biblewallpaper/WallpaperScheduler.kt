package com.joel.biblewallpaper

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WallpaperScheduler {
    const val TAG = "bible_widget_work"
    private const val ROTATION = "bible_widget_rotation_v2"
    private const val DOWNLOAD = "bible_widget_download_v2"
    private const val MANUAL_DOWNLOAD = "bible_widget_manual_download_v2"
    private const val NAVIGATION = "bible_widget_navigation_v21"
    fun migrate(c: Context) {
        val prefs = c.getSharedPreferences("bible_widget_migration",Context.MODE_PRIVATE)
        val wm = WorkManager.getInstance(c)
        if (!prefs.getBoolean("v2",false)) {
            wm.cancelUniqueWork("bible_wallpaper_photo_download_worker")
            wm.cancelUniqueWork("saved_wallpaper_60_min_rotation_worker")
            wm.cancelAllWorkByTag("com.joel.biblewallpaper.WallpaperWorker")
            wm.cancelAllWorkByTag("com.joel.biblewallpaper.SavedWallpaperRotationWorker")
            prefs.edit().putBoolean("v2",true).apply()
        }
        if (!prefs.getBoolean("v21",false)) {
            wm.cancelUniqueWork(ROTATION)
            wm.cancelUniqueWork(DOWNLOAD)
            wm.cancelUniqueWork("bible_widget_manual_next_v2")
            prefs.edit().putBoolean("v21",true).apply()
        }
    }
    fun scheduleAll(c: Context,resetRotation: Boolean = false) {
        migrate(c)
        val wm = WorkManager.getInstance(c)
        if (!BibleWidgetProvider.hasWidgets(c)) { cancelScheduled(c); return }
        val minutes = WidgetSettings.rotationMinutes(c)
        if (minutes == 0 || WallpaperHistoryStore.cards(c).size < 2) wm.cancelUniqueWork(ROTATION) else {
            val rotation = PeriodicWorkRequestBuilder<WidgetRotationWorker>(minutes.toLong(),TimeUnit.MINUTES,
                RotationIntervals.flex(minutes),TimeUnit.MINUTES)
                .setInitialDelay(minutes.toLong(),TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .addTag(TAG).build()
            val policy = if (resetRotation) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP
            wm.enqueueUniquePeriodicWork(ROTATION,policy,rotation)
        }
        if (!WidgetSettings.autoDownloadsEnabled(c)) { wm.cancelUniqueWork(DOWNLOAD); return }
        val download = PeriodicWorkRequestBuilder<WidgetDownloadWorker>(24,TimeUnit.HOURS,6,TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true).setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES)
            .addTag(TAG).addTag("auto_download").build()
        wm.enqueueUniquePeriodicWork(DOWNLOAD,ExistingPeriodicWorkPolicy.KEEP,download)
    }
    fun cancelScheduled(c: Context) {
        val wm = WorkManager.getInstance(c)
        listOf(ROTATION,DOWNLOAD).forEach { wm.cancelUniqueWork(it) }
    }
    fun downloadNow(c: Context) {
        WorkManager.getInstance(c).enqueueUniqueWork(MANUAL_DOWNLOAD,ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetDownloadWorker>().setInputData(workDataOf("manual" to true))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true).build()).addTag(TAG).addTag("manual_download").build())
    }
    fun nextNow(c: Context) = navigate(c,1)
    fun previousNow(c: Context) = navigate(c,-1)
    private fun navigate(c: Context,direction: Int) {
        WorkManager.getInstance(c).enqueueUniqueWork(NAVIGATION,ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<WidgetRotationWorker>()
                .setInputData(workDataOf("manual" to true,"direction" to direction))
                .addTag(TAG).addTag("manual_navigation").build())
    }
}
