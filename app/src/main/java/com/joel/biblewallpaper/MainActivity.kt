package com.joel.biblewallpaper

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var emptyNote: TextView
    private lateinit var download: Button
    private lateinit var pause: Button
    private lateinit var gallery: LinearLayout
    @Volatile private var renderGeneration = 0
    private var galleryLimit = 12
    private var downloadPending = false
    private val ink = Color.rgb(35,49,44)
    private val muted = Color.rgb(100,109,100)
    private val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperScheduler.scheduleAll(this)
        buildUi()
        WorkManager.getInstance(this).getWorkInfosByTagLiveData(WallpaperScheduler.TAG).observe(this) { works ->
            downloadPending = works.any { "manual_download" in it.tags &&
                (it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED) }
            download.isEnabled = !downloadPending
            download.text = if (downloadPending) "Download queued / running…" else "Download new photo now"
            status.text = makeStatus(works.any { it.state == WorkInfo.State.RUNNING })
            refreshImages()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::preview.isInitialized) { status.text = makeStatus(); refreshImages() }
    }
    override fun onDestroy() { renderGeneration++; io.shutdown(); super.onDestroy() }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.app_background)); clipToPadding = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        root = column().apply { setPadding(dp(20),dp(24),dp(20),dp(32)) }
        scroll.addView(root)
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
        root.addView(label("BIBLE NATURE",13,true).apply { letterSpacing = .15f; setTextColor(muted) })
        root.addView(label("A moment of stillness",29,true).apply { setPadding(0,dp(8),0,dp(6)) })
        root.addView(label("Landscape scenery. Scripture you can read at a glance.",16).apply { setTextColor(muted) })
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Preview of your current Bible verse widget"
            adjustViewBounds = true
        }
        root.addView(preview, LinearLayout.LayoutParams(-1,dp(((resources.displayMetrics.widthPixels/resources.displayMetrics.density-40)/1.777f).toInt())).apply { topMargin=dp(20) })
        emptyNote = label("",14).apply { setTextColor(muted); setPadding(0,dp(10),0,dp(10)) }
        root.addView(emptyNote)
        val setup = panel("Lock screen setup")
        setup.addView(label("1. Download your first photo below.\n2. Open Good Lock → LockStar → edit your lock screen.\n3. Add the Bible Nature Widget and resize it into a wide card.\n\nYou can also place it on your home screen. Tapping the widget opens this app.",15))
        setup.addView(button("Add home screen widget") { requestWidget() })
        setup.addView(label("LockStar controls availability, placement and final size. If it is missing there, add it to the home screen first, then reopen LockStar.",13).apply { setTextColor(muted) })

        val controls = panel("Your widget")
        status = label(makeStatus(),14).apply { setTextColor(muted) }
        controls.addView(status)
        download = button("Download new photo now") {
            if (!downloadPending) {
                download.isEnabled = false
                WallpaperScheduler.downloadNow(this)
            }
        }
        controls.addView(download)
        controls.addView(label("Manual downloads can use mobile data and do not require charging.",12).apply { setTextColor(muted) })
        controls.addView(button("Next saved image") { WallpaperScheduler.nextNow(this) })
        pause = button(pauseText()) {
            WidgetSettings.setPaused(this,!WidgetSettings.paused(this))
            WallpaperScheduler.scheduleAll(this)
            pause.text = pauseText()
            status.text = makeStatus()
        }
        controls.addView(pause)
        controls.addView(label("Pausing holds the current card. Daily downloads may still save new photos in the gallery.",13).apply { setTextColor(muted) })

        val appearance = panel("Make the verse clear")
        appearance.addView(label("Text size is automatically reduced only when needed to fit the full verse.",14))
        val sizeLabel = label(getString(R.string.text_size_label,WidgetSettings.textSize(this)),14,true)
        appearance.addView(sizeLabel)
        val size = SeekBar(this).apply { max=12; progress=WidgetSettings.textSize(this@MainActivity)-16 }
        appearance.addView(size)
        val darkLabel = label(getString(R.string.darkness_label,WidgetSettings.darkness(this)),14,true)
        appearance.addView(darkLabel)
        val dark = SeekBar(this).apply { max=60; progress=WidgetSettings.darkness(this@MainActivity)-25 }
        appearance.addView(dark)
        size.setOnSeekBarChangeListener(seek { sizeLabel.text=getString(R.string.text_size_label,it+16) })
        dark.setOnSeekBarChangeListener(seek { darkLabel.text=getString(R.string.darkness_label,it+25) })
        appearance.addView(button("Apply appearance") {
            WidgetSettings.setAppearance(this,size.progress+16,dark.progress+25)
            perform { BibleWidgetProvider.refreshAll(this) }
        })

        val battery = panel("Battery saver · default")
        battery.addView(label("Saved images rotate about every 4 hours. A new photo downloads about once daily, only while charging on an unmetered connection (usually Wi-Fi).\n\nAndroid may delay updates. Keep battery mode Optimized. Cached photos remain visible offline; no continuously running service is needed. Scheduled work stops when the last widget is removed.",14))
        val historyPanel = panel("Saved landscape photos")
        historyPanel.addView(label("Tap a photo to preview it. Choose Use in widget to select it. All widget copies share this selection and appearance.",14).apply { setTextColor(muted) })
        gallery = column()
        historyPanel.addView(gallery)
        historyPanel.addView(label("Keeps up to 40 ordinary photos plus the current photo and favorites. Ordinary photos also have an 80 MiB cap. Favorites are never automatically removed and can use extra storage.",12).apply { setTextColor(muted) })

        val legacy = File(filesDir,"wallpaper_history")
        if (legacy.exists() && legacy.listFiles().orEmpty().isNotEmpty()) {
            val old = panel("Previous version")
            old.addView(label("Your old portrait wallpapers are preserved, but are not used in this landscape widget. Deleting them here is optional.",14))
            old.addView(button("Delete old portrait history") {
                AlertDialog.Builder(this).setTitle("Delete old portrait history?")
                    .setMessage("This deletes only the previous version's generated images. New widget images and favorites are kept.")
                    .setNegativeButton("Keep",null).setPositiveButton("Delete") { _,_ ->
                        perform { check(legacy.deleteRecursively()) { "Could not remove all old images" } }
                        old.visibility=View.GONE
                    }.show()
            })
        }
        root.addView(button("Photos provided by Pexels ↗") { openUrl("https://www.pexels.com") })
        root.addView(label("Bible Nature Widget · 2.0",12).apply { gravity=Gravity.CENTER; setTextColor(muted) })
        refreshImages()
    }
    private fun makeStatus(running: Boolean = false): String {
        val count = WallpaperHistoryStore.cards(this).size
        val active = if (BibleWidgetProvider.hasWidgets(this)) "Widget placed" else "No widget placed · automatic work stopped"
        val state = if (WidgetSettings.paused(this)) "Rotation paused" else "Rotation: about every 4 hours"
        val message = if (running) "Working…" else if (downloadPending) "Download queued; waiting for a connection or Android to run it." else WidgetSettings.status(this)
        return "$active\n$state · $count saved\n$message"
    }
    private fun pauseText() = if (WidgetSettings.paused(this)) "Resume automatic rotation" else "Pause automatic rotation"
    private fun refreshImages() {
        val generation = ++renderGeneration
        val limit = galleryLimit
        io.execute {
            if (generation != renderGeneration) return@execute
            try {
                val cards = WallpaperHistoryStore.cards(this)
                val current = WallpaperHistoryStore.current(this)
                val rendered = WallpaperImageCreator.render(this,current,960,540)
                val thumbs = cards.take(limit).map { it to WallpaperImageCreator.render(this,it,720,405) }
                runOnUiThread {
                    if (isDestroyed || generation != renderGeneration) {
                        rendered.recycle(); thumbs.forEach { it.second.recycle() }; return@runOnUiThread
                    }
                    preview.setImageBitmap(rendered)
                    emptyNote.text = if (cards.isEmpty()) "Offline starter card · download a photo to begin." else "Current verse: ${current?.verse?.reference.orEmpty()}"
                    gallery.removeAllViews()
                    if (cards.isEmpty()) gallery.addView(label("No saved landscape photos yet.",15).apply { setPadding(0,dp(16),0,dp(16)) })
                    thumbs.forEach { (card,bitmap) ->
                        val item = column().apply { setPadding(0,dp(16),0,dp(10)) }
                        val image = ImageView(this).apply {
                            scaleType=ImageView.ScaleType.FIT_CENTER; adjustViewBounds=true
                            setImageBitmap(bitmap); contentDescription="${card.verse.text} ${card.verse.reference}"
                            setOnClickListener { showCard(card) }
                        }
                        item.addView(image,LinearLayout.LayoutParams(-1,dp(150)))
                        item.addView(label("${if(card.favorite) "★ " else ""}${card.verse.reference}${if(card.id==current?.id) " · Current" else ""}",15,true))
                        item.addView(label(dateFormat.format(Date(card.created)),12).apply { setTextColor(muted) })
                        if (card.photographer.isNotBlank()) item.addView(label("Photo by ${card.photographer} on Pexels ↗",12).apply {
                            setTextColor(muted); setPadding(0,dp(8),0,dp(8)); setOnClickListener { openUrl(card.photoUrl) }
                        })
                        item.addView(button("Use in widget") { perform { WallpaperHistoryStore.select(this,card.id); BibleWidgetProvider.refreshAll(this) } })
                        val actions = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
                        actions.addView(button(if(card.favorite) "★ Unfavorite" else "☆ Favorite") {
                            perform { WallpaperHistoryStore.favorite(this,card.id); WallpaperHistoryStore.prune(this) }
                        },LinearLayout.LayoutParams(0,-2,1f))
                        actions.addView(button("Delete") { confirmDelete(card) },LinearLayout.LayoutParams(0,-2,1f))
                        item.addView(actions)
                        gallery.addView(item)
                    }
                    if (cards.size>galleryLimit) gallery.addView(button("Show more photos") { galleryLimit+=12; refreshImages() })
                }
            } catch (_: Exception) { runOnUiThread { if (!isDestroyed) toast("Could not load a preview. Try reopening the app.") } }
        }
    }
    private fun showCard(card: VerseCard) {
        val image = ImageView(this).apply { adjustViewBounds=true; scaleType=ImageView.ScaleType.FIT_CENTER }
        val dialog = AlertDialog.Builder(this).setTitle(card.verse.reference).setView(image)
            .setNegativeButton("Close",null).setPositiveButton("Use in widget") { _,_ ->
                perform { WallpaperHistoryStore.select(this,card.id); BibleWidgetProvider.refreshAll(this) }
            }.create()
        dialog.show()
        io.execute {
            val bitmap = WallpaperImageCreator.render(this,card,960,540)
            runOnUiThread { if(dialog.isShowing) image.setImageBitmap(bitmap) else bitmap.recycle() }
        }
    }
    private fun confirmDelete(card: VerseCard) {
        AlertDialog.Builder(this).setTitle(if(card.favorite) "Delete this favorite?" else "Delete this photo?")
            .setMessage("The saved photo and its verse card will be removed.")
            .setNegativeButton("Keep",null).setPositiveButton("Delete") { _,_ ->
                perform { WallpaperHistoryStore.delete(this,card.id); BibleWidgetProvider.refreshAll(this) }
            }.show()
    }
    private fun perform(action: () -> Unit) {
        io.execute {
            try {
                action()
                runOnUiThread { if(!isDestroyed) { status.text=makeStatus(); refreshImages() } }
            } catch (_: Exception) { runOnUiThread { if(!isDestroyed) toast("Could not complete that change. Please try again.") } }
        }
    }
    private fun requestWidget() {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(this,BibleWidgetProvider::class.java),null,null)
        else toast("Long-press the home screen → Widgets → Bible Nature Widget.")
    }
    private fun openUrl(value: String) {
        val uri = Uri.parse(value)
        if (uri.scheme != "https") return
        try { startActivity(Intent(Intent.ACTION_VIEW,uri)) } catch (_: Exception) { toast("No browser available.") }
    }
    private fun column() = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun label(value: String,size: Int,bold: Boolean=false) = TextView(this).apply {
        text=value; textSize=size.toFloat(); setTextColor(ink); setLineSpacing(dp(3).toFloat(),1f)
        if(bold) setTypeface(typeface,Typeface.BOLD)
    }
    private fun button(value: String,action: () -> Unit) = Button(this).apply {
        text=value; isAllCaps=false; minHeight=dp(48); setTextColor(ink)
        setOnClickListener { action() }
    }
    private fun panel(title: String): LinearLayout {
        val box=column().apply {
            setPadding(dp(16),dp(16),dp(16),dp(16))
            background=GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=dp(18).toFloat() }
        }
        box.addView(label(title,19,true).apply { setPadding(0,0,0,dp(12)) })
        root.addView(box,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(18) })
        return box
    }
    private fun seek(change:(Int)->Unit) = object: SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar:SeekBar?,progress:Int,fromUser:Boolean) { change(progress) }
        override fun onStartTrackingTouch(bar:SeekBar?) {}
        override fun onStopTrackingTouch(bar:SeekBar?) {}
    }
    private fun toast(message:String) { Toast.makeText(this,message,Toast.LENGTH_LONG).show() }
    private fun dp(value:Int) = (value*resources.displayMetrics.density).toInt()
}
