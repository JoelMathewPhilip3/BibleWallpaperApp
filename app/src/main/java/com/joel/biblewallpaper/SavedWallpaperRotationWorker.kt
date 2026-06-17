package com.joel.biblewallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SavedWallpaperRotationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                val wallpaperManager = WallpaperManager.getInstance(applicationContext)

                if (!wallpaperManager.isSetWallpaperAllowed) {
                    return@withContext Result.failure()
                }

                val files = WallpaperHistoryStore.getHistoryFiles(applicationContext)

                if (files.isEmpty()) {
                    return@withContext Result.retry()
                }

                val prefs = applicationContext.getSharedPreferences(
                    "saved_wallpaper_rotation_prefs",
                    Context.MODE_PRIVATE
                )

                val lastPath = prefs.getString("last_rotated_wallpaper_path", null)

                val nextFile = if (files.size == 1) {
                    files.first()
                } else {
                    files.firstOrNull { it.absolutePath != lastPath } ?: files.first()
                }

                val bitmap = BitmapFactory.decodeFile(nextFile.absolutePath)
                    ?: return@withContext Result.retry()

                wallpaperManager.setBitmap(bitmap)

                prefs.edit()
                    .putString("last_rotated_wallpaper_path", nextFile.absolutePath)
                    .apply()

                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
