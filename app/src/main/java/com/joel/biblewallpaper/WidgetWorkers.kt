package com.joel.biblewallpaper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val widgetWorkMutex = Mutex()
class WidgetRotationWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c,p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        widgetWorkMutex.withLock {
            val c = applicationContext
            val manual = inputData.getBoolean("manual",false)
            if (!manual && (!BibleWidgetProvider.hasWidgets(c) || WidgetSettings.paused(c))) return@withLock Result.success()
            WallpaperHistoryStore.next(c)
            BibleWidgetProvider.refreshAll(c)
            Result.success()
        }
    }
}
class WidgetDownloadWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c,p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        widgetWorkMutex.withLock {
            val c = applicationContext
            val manual = inputData.getBoolean("manual",false)
            if (!manual && !BibleWidgetProvider.hasWidgets(c)) return@withLock Result.success()
            if (!manual && System.currentTimeMillis()-WidgetSettings.lastDownload(c) < 20*60*60*1000L)
                return@withLock Result.success()
            try {
                WidgetSettings.setStatus(c,"Downloading a landscape photo…")
                val photo = PexelsPhotoProvider.getFreshNaturePhoto(c)
                try {
                    // Recheck after network I/O: a widget may have been removed during the download.
                    if (!manual && !BibleWidgetProvider.hasWidgets(c)) return@withLock Result.success()
                    WallpaperHistoryStore.save(c,photo,WallpaperHistoryStore.nextVerse(c),
                        selectNew = manual || !WidgetSettings.paused(c))
                } finally { photo.bitmap.recycle() }
                WidgetSettings.markDownloaded(c)
                val time = SimpleDateFormat("MMM d, h:mm a",Locale.getDefault()).format(Date())
                WidgetSettings.setStatus(c,"Last photo downloaded: $time")
                BibleWidgetProvider.refreshAll(c)
                Result.success()
            } catch (e: PhotoHttpException) {
                val detail = when(e.status) {
                    401,403 -> "Photo access was refused. Check the Pexels API key."
                    429 -> "Photo service limit reached. Try again later."
                    else -> "Photo service unavailable (HTTP ${e.status})."
                }
                WidgetSettings.setStatus(c,"$detail Saved images remain available.")
                if (!manual && e.status >= 500 && runAttemptCount < 2) Result.retry() else Result.failure()
            } catch (_: IOException) {
                WidgetSettings.setStatus(c,"Could not download a photo. Saved images remain available.")
                if (!manual && runAttemptCount < 2) Result.retry() else Result.failure()
            } catch (_: Exception) {
                WidgetSettings.setStatus(c,"Could not save the new photo. Existing images remain available.")
                Result.failure()
            }
        }
    }
}
