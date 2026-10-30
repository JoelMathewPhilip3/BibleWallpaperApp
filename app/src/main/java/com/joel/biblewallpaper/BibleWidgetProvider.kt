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
    override fun onReceive(context: Context,intent: Intent) {
        when (intent.action) {
            ACTION_PREVIOUS -> WallpaperScheduler.previousNow(context)
            ACTION_NEXT -> WallpaperScheduler.nextNow(context)
            else -> super.onReceive(context,intent)
        }
    }
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
        private const val ACTION_PREVIOUS = "com.joel.biblewallpaper.PREVIOUS"
        private const val ACTION_NEXT = "com.joel.biblewallpaper.NEXT"
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
            val buttonBand = (h * (40f/dpHeight).coerceIn(.15f,.4f)).toInt()
            val bitmap = WallpaperImageCreator.render(c,card,w,h,bottomInset=buttonBand)
            try {
                val views = RemoteViews(c.packageName,R.layout.bible_widget)
                views.setImageViewBitmap(R.id.widget_image,bitmap)
                views.setContentDescription(R.id.widget_image,card?.let { "${it.verse.text} ${it.verse.reference}" }
                    ?: "Be still, and know that I am God. Psalm 46:10. Open the app to download a nature photo.")
                val intent = Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val tap = PendingIntent.getActivity(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image,tap)
                fun navigationIntent(action: String,code: Int): PendingIntent = PendingIntent.getBroadcast(c,code,
                    Intent(c,BibleWidgetProvider::class.java).setAction(action),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_previous,navigationIntent(ACTION_PREVIOUS,1))
                views.setOnClickPendingIntent(R.id.widget_next,navigationIntent(ACTION_NEXT,2))
                val canPrevious = WallpaperHistoryStore.canPrevious(c)
                views.setBoolean(R.id.widget_previous,"setEnabled",canPrevious)
                views.setFloat(R.id.widget_previous,"setAlpha",if(canPrevious) 1f else .4f)
                views.setBoolean(R.id.widget_next,"setEnabled",card != null)
                views.setFloat(R.id.widget_next,"setAlpha",if(card != null) 1f else .4f)
                manager.updateAppWidget(id,views)
            } finally { bitmap.recycle() }
        }
    }
}
