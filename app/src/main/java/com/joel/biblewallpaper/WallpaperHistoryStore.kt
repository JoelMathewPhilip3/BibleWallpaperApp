package com.joel.biblewallpaper

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object WallpaperHistoryStore {
    private const val PREFS = "wallpaper_history_prefs"
    private const val USED_VERSES = "used_verse_references"
    private const val USED_SCENES = "used_scene_names"
    private const val LAST_RUN_AT = "last_run_at"
    private const val LAST_SCENE = "last_scene"

    fun historyDir(context: Context): File = File(context.filesDir, "wallpaper_history").apply { mkdirs() }

    fun getHistoryFiles(context: Context): List<File> = historyDir(context)
        .listFiles { file -> file.extension.equals("png", ignoreCase = true) }?.toList()
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

    fun save(context: Context, bitmap: Bitmap, verse: Verse, sceneName: String): File {
        val safeReference = verse.reference.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        val file = File(historyDir(context), "${System.currentTimeMillis()}_${sceneName.replace(" ", "_")}_${safeReference}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        rememberVerse(context, verse)
        rememberScene(context, sceneName)
        setLastRunAt(context, System.currentTimeMillis())
        return file
    }

    fun nextUnusedVerse(context: Context): Verse {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val used = prefs.getStringSet(USED_VERSES, emptySet()).orEmpty()
        val remaining = Verses.all.filterNot { used.contains(it.reference) }
        return if (remaining.isNotEmpty()) {
            remaining.random()
        } else {
            prefs.edit().putStringSet(USED_VERSES, emptySet()).apply()
            Verses.all.random()
        }
    }

    fun nextUnusedSceneIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val used = prefs.getStringSet(USED_SCENES, emptySet()).orEmpty()
        val remaining = SceneStyles.all.filterNot { used.contains(it) }
        val selected = when {
            remaining.isNotEmpty() -> remaining.random()
            else -> {
                prefs.edit().putStringSet(USED_SCENES, emptySet()).apply()
                SceneStyles.all.random()
            }
        }

        val lastScene = prefs.getString(LAST_SCENE, null)
        val finalSelection = if (SceneStyles.all.size > 1 && selected == lastScene) {
            SceneStyles.all.firstOrNull { it != lastScene && !used.contains(it) }
                ?: SceneStyles.all.firstOrNull { it != lastScene }
                ?: selected
        } else {
            selected
        }

        return SceneStyles.all.indexOf(finalSelection).coerceAtLeast(0)
    }

    fun usedVerseCount(context: Context): Int = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(USED_VERSES, emptySet()).orEmpty().size

    fun usedSceneCount(context: Context): Int = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(USED_SCENES, emptySet()).orEmpty().size

    fun lastRunAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_RUN_AT, 0L)

    private fun rememberVerse(context: Context, verse: Verse) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(USED_VERSES, emptySet()).orEmpty().toMutableSet().apply { add(verse.reference) }
        prefs.edit().putStringSet(USED_VERSES, updated).apply()
    }

    private fun rememberScene(context: Context, sceneName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(USED_SCENES, emptySet()).orEmpty().toMutableSet().apply { add(sceneName) }
        prefs.edit()
            .putStringSet(USED_SCENES, updated)
            .putString(LAST_SCENE, sceneName)
            .apply()
    }

    private fun setLastRunAt(context: Context, millis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(LAST_RUN_AT, millis).apply()
    }
}
