package com.joel.biblewallpaper

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.UUID

/** Original class/file name retained so a GitHub overlay needs no source deletions. */
data class VerseCard(val id: String, val image: File, val verse: Verse,
    val photographer: String, val photoUrl: String, val photoId: String,
    val created: Long, val favorite: Boolean)

object WallpaperHistoryStore {
    private fun prefs(c: Context) = c.getSharedPreferences("bible_card_history", Context.MODE_PRIVATE)
    private fun dir(c: Context) = File(c.filesDir, "verse_cards").apply { mkdirs() }
    @Synchronized fun cards(c: Context): List<VerseCard> = dir(c).listFiles()
        .orEmpty().filter { it.extension == "json" }.mapNotNull { f ->
            try {
                val o = JSONObject(f.readText())
                val id = f.nameWithoutExtension
                val image = File(dir(c), "$id.jpg")
                if (!image.exists()) null else VerseCard(id, image,
                    Verse(o.getString("text"), o.getString("reference")),
                    o.optString("photographer"), o.optString("photoUrl"), o.optString("photoId"),
                    o.getLong("created"), o.optBoolean("favorite"))
            } catch (_: Exception) { null }
        }.sortedByDescending { it.created }
    @Synchronized fun current(c: Context): VerseCard? {
        val cards = cards(c)
        val id = prefs(c).getString("current", null)
        return cards.firstOrNull { it.id == id } ?: cards.firstOrNull()
    }
    private fun navigation(c: Context, available: List<VerseCard>): RotationPolicy.State {
        val state = try {
            val o = JSONObject(prefs(c).getString("navigation", "{}").orEmpty())
            fun strings(name: String): List<String> {
                val a = o.optJSONArray(name) ?: JSONArray()
                return (0 until a.length()).map { a.getString(it) }
            }
            RotationPolicy.State(strings("history"),o.optInt("position",-1),strings("seen").toSet())
        } catch (_: Exception) { RotationPolicy.State() }
        val current = prefs(c).getString("current",null)
            ?.takeIf { id -> available.any { it.id == id } } ?: available.firstOrNull()?.id
        return RotationPolicy.reconcile(state,available.map { it.id },current)
    }
    private fun saveNavigation(c: Context,state: RotationPolicy.State) {
        val o = JSONObject().put("history",JSONArray(state.history))
            .put("position",state.position).put("seen",JSONArray(state.seen.toList()))
        prefs(c).edit().putString("navigation",o.toString()).putString("current",state.current).apply()
    }
    @Synchronized fun select(c: Context,id: String) {
        val all = cards(c)
        if (all.any { it.id == id }) saveNavigation(c,RotationPolicy.select(navigation(c,all),id))
    }
    @Synchronized fun next(c: Context) {
        val all = cards(c)
        saveNavigation(c,RotationPolicy.next(navigation(c,all),all.map { it.id }))
    }
    @Synchronized fun previous(c: Context) { saveNavigation(c,RotationPolicy.previous(navigation(c,cards(c)))) }
    @Synchronized fun canPrevious(c: Context) = navigation(c,cards(c)).position > 0
    @Synchronized fun nextVerse(c: Context): Verse {
        val used = prefs(c).getStringSet("used_verses", emptySet()).orEmpty()
        return Verses.all.filterNot { it.reference in used }.ifEmpty { Verses.all }.random()
    }
    @Synchronized fun usedPhotoIds(c: Context): Set<String> =
        prefs(c).getStringSet("used_photos", emptySet()).orEmpty().toSet()
    @Synchronized fun save(c: Context, photo: NaturePhoto, verse: Verse, selectNew: Boolean = true): VerseCard {
        val previous = current(c)?.id
        val id = UUID.randomUUID().toString()
        val image = File(dir(c), "$id.jpg")
        val temp = File(dir(c), "$id.tmp")
        try {
            temp.outputStream().use { check(photo.bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            check(temp.renameTo(image))
            val o = JSONObject().put("text", verse.text).put("reference", verse.reference)
                .put("photographer", photo.photographer).put("photoUrl", photo.photoUrl)
                .put("photoId", photo.id).put("created", System.currentTimeMillis()).put("favorite", false)
            atomicJson(File(dir(c), "$id.json"), o)
        } catch (e: Exception) { temp.delete(); image.delete(); throw e }
        val old = prefs(c).getStringSet("used_verses", emptySet()).orEmpty()
        val used = if (Verses.all.all { it.reference in old }) mutableSetOf() else old.toMutableSet()
        used.add(verse.reference)
        val ids = usedPhotoIds(c).toMutableSet().apply { add(photo.id) }.toList().takeLast(2000).toSet()
        val edit = prefs(c).edit().putStringSet("used_verses", used).putStringSet("used_photos", ids)
        edit.apply()
        if (selectNew || previous == null) select(c,id)
        prune(c,extraProtected = setOf(id))
        return VerseCard(id,image,verse,photo.photographer,photo.photoUrl,photo.id,
            JSONObject(File(dir(c), "$id.json").readText()).getLong("created"),false)
    }
    @Synchronized fun favorite(c: Context, id: String) {
        val f = File(dir(c), "$id.json")
        if (!f.exists()) return
        val o = JSONObject(f.readText())
        o.put("favorite", !o.optBoolean("favorite"))
        atomicJson(f, o)
    }
    @Synchronized fun delete(c: Context, id: String) {
        // Only ids present in our own index are accepted.
        val card = cards(c).firstOrNull { it.id == id } ?: return
        File(dir(c), "$id.json").delete()
        card.image.delete()
        if (prefs(c).getString("current", null) == id) prefs(c).edit().remove("current").apply()
    }
    @Synchronized fun prune(c: Context,extraProtected: Set<String> = emptySet()) {
        val all = cards(c)
        val protected = all.filter { it.favorite }.map { it.id }.toMutableSet()
        current(c)?.let { protected.add(it.id) }
        protected.addAll(extraProtected)
        // Up to 40 ordinary cards + current/favorites; 80 MiB ordinary-image cap.
        val remove = RotationPolicy.removable(all.map { it.id }, protected, 40).toMutableSet()
        var bytes = all.filterNot { it.id in protected || it.id in remove }.sumOf { it.image.length() }
        all.asReversed().filterNot { it.id in protected || it.id in remove }.forEach {
            if (bytes > 80L * 1024 * 1024) { remove.add(it.id); bytes -= it.image.length() }
        }
        remove.forEach { delete(c, it) }
    }
    private fun atomicJson(f: File, o: JSONObject) {
        val temp = File(f.parentFile, f.name + ".tmp")
        temp.writeText(o.toString())
        check(temp.renameTo(f)) { "Could not save photo details" }
    }
}
