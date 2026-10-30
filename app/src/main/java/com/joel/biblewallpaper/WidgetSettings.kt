package com.joel.biblewallpaper

import android.content.Context

object WidgetSettings {
    private fun prefs(c: Context) = c.getSharedPreferences("bible_widget_settings", Context.MODE_PRIVATE)
    fun paused(c: Context) = prefs(c).getBoolean("paused", false)
    fun setPaused(c: Context, value: Boolean) { prefs(c).edit().putBoolean("paused", value).apply() }
    fun textSize(c: Context) = prefs(c).getInt("text_size", 20).coerceIn(16, 28)
    fun darkness(c: Context) = prefs(c).getInt("darkness", 55).coerceIn(25, 85)
    fun setAppearance(c: Context, textSize: Int, darkness: Int) {
        prefs(c).edit().putInt("text_size", textSize.coerceIn(16,28))
            .putInt("darkness", darkness.coerceIn(25,85)).apply()
    }
    fun status(c: Context) = prefs(c).getString("status", "No download yet. Add a widget, then download your first photo.").orEmpty()
    fun setStatus(c: Context, status: String) { prefs(c).edit().putString("status", status).apply() }
    fun lastDownload(c: Context) = prefs(c).getLong("last_download", 0)
    fun markDownloaded(c: Context) { prefs(c).edit().putLong("last_download", System.currentTimeMillis()).apply() }
}
