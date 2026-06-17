package com.joel.biblewallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.random.Random

object PexelsPhotoProvider {
    private const val API_KEY = "jkUOQWpdX73pD2zpGUnw0oLyzHbHuM7fCxscFF1BcpFKk9cShmPnysxM"
    private const val PREFS = "pexels_photo_prefs"
    private const val USED_PHOTO_IDS = "used_pexels_photo_ids"

    private val client = OkHttpClient()

    private val natureQueries = listOf(
        "mountain landscape",
        "forest trail",
        "waterfall nature",
        "lake sunrise",
        "ocean sunset",
        "green meadow",
        "misty forest",
        "snow mountains",
        "river valley",
        "wildflowers field",
        "pine forest",
        "tropical beach",
        "desert sunrise",
        "autumn forest",
        "northern lights landscape"
    )

    fun getFreshNaturePhoto(context: Context): Pair<Bitmap, String> {
        val query = natureQueries.random()
        val page = Random.nextInt(1, 40)
        val url =
            "https://api.pexels.com/v1/search?query=${query.replace(" ", "%20")}&orientation=portrait&per_page=15&page=$page"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", API_KEY)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Pexels search failed: ${response.code}")
            }

            val body = response.body?.string() ?: throw IllegalStateException("Empty Pexels response")
            val photos = JSONObject(body).getJSONArray("photos")

            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val usedIds = prefs.getStringSet(USED_PHOTO_IDS, emptySet()).orEmpty().toMutableSet()

            var chosenId: String? = null
            var chosenUrl: String? = null

            for (i in 0 until photos.length()) {
                val photo = photos.getJSONObject(i)
                val id = photo.getLong("id").toString()

                if (!usedIds.contains(id)) {
                    chosenId = id
                    chosenUrl = photo.getJSONObject("src").getString("portrait")
                    break
                }
            }

            if (chosenId == null || chosenUrl == null) {
                usedIds.clear()
                val photo = photos.getJSONObject(0)
                chosenId = photo.getLong("id").toString()
                chosenUrl = photo.getJSONObject("src").getString("portrait")
            }

            val imageRequest = Request.Builder()
                .url(chosenUrl)
                .build()

            client.newCall(imageRequest).execute().use { imageResponse ->
                if (!imageResponse.isSuccessful) {
                    throw IllegalStateException("Image download failed: ${imageResponse.code}")
                }

                val bytes = imageResponse.body?.bytes()
                    ?: throw IllegalStateException("Empty image body")

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Could not decode image")

                usedIds.add(chosenId)
                prefs.edit().putStringSet(USED_PHOTO_IDS, usedIds).apply()

                return bitmap to chosenId
            }
        }
    }
}
