package com.joel.biblewallpaper

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : android.app.Activity() {
    private lateinit var galleryLayout: LinearLayout
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperScheduler.scheduleEvery12Hours(this)
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
            text = "The app automatically creates a new scenic nature wallpaper with a Bible verse every 12 hours. Generated wallpapers are saved below so you can view previous ones."
            textSize = 17f
            setPadding(0, 0, 0, 20)
        }
        val status = TextView(this).apply {
            val last = WallpaperHistoryStore.lastRunAt(this@MainActivity)
            text = if (last > 0L) {
                "Last wallpaper: ${dateFormat.format(Date(last))}\nVerses used before repeat: ${WallpaperHistoryStore.usedVerseCount(this@MainActivity)} of ${Verses.all.size}"
            } else {
                "Automatic schedule is active after the app is opened once. No wallpaper has been generated yet."
            }
            textSize = 14f
            setPadding(0, 0, 0, 22)
        }
        val button = Button(this).apply {
            text = "Create Wallpaper Now"
            setOnClickListener {
                WallpaperScheduler.runOnceNow(this@MainActivity)
                postDelayed({ loadHistoryGallery() }, 2500)
            }
        }
        val note = TextView(this).apply {
            text = "For best automatic updates, set this app to Unrestricted in Android battery settings. Android may delay background work during battery-saving modes."
            textSize = 13f
            setPadding(0, 24, 0, 28)
        }
        val historyTitle = TextView(this).apply {
            text = "Saved Wallpaper History"
            textSize = 22f
            setPadding(0, 8, 0, 16)
        }
        galleryLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

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
                text = "No saved wallpapers yet. The first automatic wallpaper will appear here after it runs, or you can create one now."
                textSize = 15f
            })
            return
        }

        files.forEach { file ->
            val label = TextView(this).apply {
                text = dateFormat.format(Date(file.lastModified()))
                textSize = 14f
                setPadding(0, 20, 0, 8)
            }
            val image = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                contentDescription = "Saved wallpaper"
            }
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(label)
                addView(image, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
            galleryLayout.addView(container)
        }
    }
}
