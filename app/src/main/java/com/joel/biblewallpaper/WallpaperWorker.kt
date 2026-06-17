package com.joel.biblewallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            if (!wallpaperManager.isSetWallpaperAllowed) return Result.failure()

            val display = applicationContext.resources.displayMetrics
            val width = display.widthPixels.coerceAtLeast(1080)
            val height = display.heightPixels.coerceAtLeast(1920)
            val verse = WallpaperHistoryStore.nextUnusedVerse(applicationContext)
            val sceneIndex = WallpaperHistoryStore.nextUnusedSceneIndex(applicationContext)
            val generated = WallpaperImageCreator.create(width, height, verse, sceneIndex)
            val bitmap: Bitmap = generated.bitmap

            WallpaperHistoryStore.save(applicationContext, bitmap, generated.verse, generated.sceneName)
            wallpaperManager.setBitmap(bitmap)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
