package com.joel.biblewallpaper

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : android.app.Activity() {
    private lateinit var galleryLayout: LinearLayout
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperScheduler.scheduleAll(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::galleryLayout.isInitialized) loadHistoryGallery()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(42, 70, 42, 42)
        }

        val title = TextView(this).apply {
            text = "Bible Nature Wallpaper"
            textSize = 28f
            setPadding(0, 0, 0, 18)
        }

        val description = TextView(this).apply {
            text = "The app downloads real nature scenery photos, adds Bible verses, saves them here, and rotates saved wallpapers about every 10 minutes. New online photo wallpapers are created about every 8 hours."
            textSize = 17f
            setPadding(0, 0, 0, 20)
        }

        val status = TextView(this).apply {
            val last = WallpaperHistoryStore.lastRunAt(this@MainActivity)
            text = if (last > 0L) {
                "Last new wallpaper created: ${dateFormat.format(Date(last))}\nVerses used before repeat: ${WallpaperHistoryStore.usedVerseCount(this@MainActivity)} of ${Verses.all.size}\nSaved wallpapers: ${WallpaperHistoryStore.getHistoryFiles(this@MainActivity).size}"
            } else {
                "Automatic schedule is active after the app is opened once. No wallpaper has been generated yet."
            }
            textSize = 14f
            setPadding(0, 0, 0, 22)
        }

        val button = Button(this).apply {
            text = "Create New Wallpaper Now"
            setOnClickListener {
                WallpaperScheduler.runOnceNow(this@MainActivity)
                postDelayed({ loadHistoryGallery() }, 4000)
            }
        }

        val note = TextView(this).apply {
            text = "Tap any saved photo below to set it as wallpaper now. Automatic rotation will still continue about every 10 minutes."
            textSize = 13f
            setPadding(0, 24, 0, 28)
        }

        val historyTitle = TextView(this).apply {
            text = "Saved Wallpaper History"
            textSize = 22f
            setPadding(0, 8, 0, 16)
        }

        galleryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(title)
        root.addView(description)
        root.addView(status)
        root.addView(button)
        root.addView(note)
        root.addView(historyTitle)
        root.addView(galleryLayout)

        scroll.addView(root)
        setContentView(scroll)

        loadHistoryGallery()
    }

    private fun loadHistoryGallery() {
        galleryLayout.removeAllViews()

        val files = WallpaperHistoryStore.getHistoryFiles(this)

        if (files.isEmpty()) {
            galleryLayout.addView(TextView(this).apply {
                text = "No saved wallpapers yet. Tap Create New Wallpaper Now, or wait for the automatic background task."
                textSize = 15f
            })
            return
        }

        files.chunked(3).forEach { rowFiles ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(10))
            }

            rowFiles.forEach { file ->
                val image = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    contentDescription = "Saved wallpaper"

                    setOnClickListener {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            WallpaperManager.getInstance(this@MainActivity).setBitmap(bitmap)

                            getSharedPreferences(
                                "saved_wallpaper_rotation_prefs",
                                MODE_PRIVATE
                            )
                                .edit()
                                .putString("last_rotated_wallpaper_path", file.absolutePath)
                                .apply()

                            Toast.makeText(
                                this@MainActivity,
                                "Wallpaper set. Auto-rotation will still continue.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Could not set wallpaper.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                val params = LinearLayout.LayoutParams(
                    0,
                    dp(150),
                    1f
                ).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }

                row.addView(image, params)
            }

            if (rowFiles.size < 3) {
                repeat(3 - rowFiles.size) {
                    val spacer = TextView(this)
                    row.addView(
                        spacer,
                        LinearLayout.LayoutParams(
                            0,
                            dp(150),
                            1f
                        )
                    )
                }
            }

            galleryLayout.addView(row)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
