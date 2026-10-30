package com.joel.biblewallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PhotoHttpException(val status: Int) : IOException("Photo service returned HTTP $status")
data class NaturePhoto(val bitmap: Bitmap, val id: String, val photographer: String, val photoUrl: String)

object PexelsPhotoProvider {
    private const val API_KEY = "jkUOQWpdX73pD2zpGUnw0oLyzHbHuM7fCxscFF1BcpFKk9cShmPnysxM"
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder()
            .header("User-Agent", "BibleNatureWidget/2.1 (Android)").build()) }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()
    private val queries = listOf("mountain landscape", "lake sunrise", "ocean sunset", "forest landscape",
        "waterfall nature", "green meadow", "snow mountains", "river valley", "tropical beach", "northern lights landscape")

    suspend fun getFreshNaturePhoto(context: Context): NaturePhoto {
        val used = WallpaperHistoryStore.usedPhotoIds(context)
        repeat(3) { attempt ->
            val query = queries.random()
            val page = if (attempt == 2) 1 else (1..3).random()
            val url = "https://api.pexels.com/v1/search".toHttpUrl().newBuilder()
                .addQueryParameter("query", query).addQueryParameter("orientation", "landscape")
                .addQueryParameter("per_page", "30").addQueryParameter("page", page.toString()).build()
            val photos = client.newCall(Request.Builder().url(url).header("Authorization", API_KEY).build())
                .awaitResponse().use { response ->
                    if (!response.isSuccessful) throw PhotoHttpException(response.code)
                    JSONObject(response.body?.string() ?: throw IOException("Empty photo search response"))
                        .getJSONArray("photos")
                }
            val candidates = (0 until photos.length()).map { photos.getJSONObject(it) }
                .filter { it.optInt("width") > it.optInt("height") }
            val fresh = candidates.filterNot { it.getLong("id").toString() in used }
            val selected = fresh.randomOrNull() ?: if (attempt == 2) candidates.randomOrNull() else null
            if (selected != null) {
                val imageUrl = selected.getJSONObject("src").getString("landscape")
                val bitmap = client.newCall(Request.Builder().url(imageUrl).build()).awaitResponse().use { response ->
                    if (!response.isSuccessful) throw PhotoHttpException(response.code)
                    val bytes = response.body?.bytes() ?: throw IOException("Empty photo download")
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Invalid photo")
                }
                return NaturePhoto(bitmap, selected.getLong("id").toString(),
                    selected.optString("photographer"), selected.optString("url"))
            }
        }
        throw IOException("No landscape photos found. Saved images are still available.")
    }
}
