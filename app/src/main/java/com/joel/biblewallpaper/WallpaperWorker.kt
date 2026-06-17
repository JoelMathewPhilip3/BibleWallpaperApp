package com.joel.biblewallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperWorker(
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

                val display = applicationContext.resources.displayMetrics
                val width = display.widthPixels.coerceAtLeast(1080)
                val height = display.heightPixels.coerceAtLeast(1920)

                val verse = WallpaperHistoryStore.nextUnusedVerse(applicationContext)
                val photoResult = PexelsPhotoProvider.getFreshNaturePhoto(applicationContext)
                val photoBitmap = photoResult.first
                val photoId = photoResult.second

                val generated = WallpaperImageCreator.createFromPhoto(
                    source = photoBitmap,
                    width = width,
                    height = height,
                    verse = verse,
                    photoId = photoId
                )

                val bitmap: Bitmap = generated.bitmap

                WallpaperHistoryStore.save(
                    applicationContext,
                    bitmap,
                    generated.verse,
                    generated.sceneName
                )

                wallpaperManager.setBitmap(bitmap)

                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
