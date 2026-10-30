package com.joel.biblewallpaper
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
/** v1 compatibility shim. Never changes wallpaper or enqueues more legacy work. */
class SavedWallpaperRotationWorker(c: Context,p: WorkerParameters) : CoroutineWorker(c,p) {
    override suspend fun doWork(): Result = Result.success()
}
