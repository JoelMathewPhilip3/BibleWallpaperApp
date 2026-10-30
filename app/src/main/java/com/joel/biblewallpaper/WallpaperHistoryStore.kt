package com.joel.biblewallpaper

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
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
    @Synchronized fun select(c: Context, id: String) {
        if (cards(c).any { it.id == id }) prefs(c).edit().putString("current", id).apply()
    }
    @Synchronized fun next(c: Context) {
        val id = RotationPolicy.next(cards(c).map { it.id }, current(c)?.id) ?: return
        select(c, id)
    }
    @Synchronized fun nextVerse(c: Context): Verse {
        val used = prefs(c).getStringSet("used_verses", emptySet()).orEmpty()
        return Verses.all.filterNot { it.reference in used }.ifEmpty { Verses.all }.random()
    }
    @Synchronized fun usedPhotoIds(c: Context): Set<String> =
        prefs(c).getStringSet("used_photos", emptySet()).orEmpty().toSet()
    @Synchronized fun save(c: Context, photo: NaturePhoto, verse: Verse, selectNew: Boolean = true): VerseCard {
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
        if (selectNew || current(c) == null) edit.putString("current", id)
        edit.apply()
        prune(c)
        return cards(c).first { it.id == id }
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
    @Synchronized fun prune(c: Context) {
        val all = cards(c)
        val protected = all.filter { it.favorite }.map { it.id }.toMutableSet()
        current(c)?.let { protected.add(it.id) }
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
