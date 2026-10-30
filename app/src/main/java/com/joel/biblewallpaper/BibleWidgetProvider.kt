package com.joel.biblewallpaper

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import java.util.concurrent.Executors

class BibleWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) { WallpaperScheduler.scheduleAll(context) }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WallpaperScheduler.scheduleAll(context)
        updateAsync(context, ids)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateAsync(context,intArrayOf(id))
    }
    override fun onDeleted(context: Context, ids: IntArray) { WallpaperScheduler.scheduleAll(context) }
    override fun onDisabled(context: Context) { WallpaperScheduler.cancelScheduled(context) }
    private fun updateAsync(context: Context, ids: IntArray) {
        val pending = goAsync()
        val c = context.applicationContext
        executor.execute { try { ids.forEach { refresh(c,it) } } finally { pending.finish() } }
    }
    companion object {
        private val executor = Executors.newSingleThreadExecutor()
        fun ids(c: Context): IntArray = AppWidgetManager.getInstance(c)
            .getAppWidgetIds(ComponentName(c,BibleWidgetProvider::class.java))
        fun hasWidgets(c: Context) = ids(c).isNotEmpty()
        fun refreshAll(c: Context) { ids(c).forEach { refresh(c,it) } }
        fun refresh(c: Context, id: Int) {
            val manager = AppWidgetManager.getInstance(c)
            val options = manager.getAppWidgetOptions(id)
            val dpWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320).coerceAtLeast(180)
            val dpHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,180).coerceAtLeast(100)
            val ratio = (dpWidth.toFloat()/dpHeight).coerceIn(1.35f,2.4f)
            // RGB/ARGB parcel is under 3 MiB, independent of source photo resolution.
            val w = (dpWidth * c.resources.displayMetrics.density).toInt().coerceIn(360,960)
            val h = (w/ratio).toInt().coerceIn(180,720)
            val card = WallpaperHistoryStore.current(c)
            val bitmap = WallpaperImageCreator.render(c,card,w,h)
            try {
                val views = RemoteViews(c.packageName,R.layout.bible_widget)
                views.setImageViewBitmap(R.id.widget_image,bitmap)
                views.setContentDescription(R.id.widget_image,card?.let { "${it.verse.text} ${it.verse.reference}" }
                    ?: "Be still, and know that I am God. Psalm 46:10. Open the app to download a nature photo.")
                val intent = Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val tap = PendingIntent.getActivity(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image,tap)
                manager.updateAppWidget(id,views)
            } finally { bitmap.recycle() }
        }
    }
}
