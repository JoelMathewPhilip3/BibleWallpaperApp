package com.joel.biblewallpaper

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/** Images remain separate from verses in storage; this composes only the displayed card. */
object WallpaperImageCreator {
    fun render(context: Context, card: VerseCard?, width: Int = 960, height: Int = 540): Bitmap {
        val w = width.coerceIn(360, 960)
        val h = height.coerceIn(180, 720)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val radius = w * .045f
        canvas.clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var source: Bitmap? = null
        try { if (card != null) source = BitmapFactory.decodeFile(card.image.absolutePath) } catch (_: Exception) { }
        if (source != null) {
            val ratio = w.toFloat() / h
            val src = if (source.width.toFloat() / source.height > ratio) {
                val sw = (source.height * ratio).toInt().coerceAtLeast(1)
                Rect((source.width-sw)/2, 0, (source.width+sw)/2, source.height)
            } else {
                val sh = (source.width / ratio).toInt().coerceAtLeast(1)
                Rect(0, (source.height-sh)/2, source.width, (source.height+sh)/2)
            }
            canvas.drawBitmap(source, src, bounds, paint)
            source.recycle()
        } else {
            paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
                Color.rgb(49,85,80), Color.rgb(15,28,44), Shader.TileMode.CLAMP)
            canvas.drawRect(bounds, paint)
            paint.shader = null
        }
        val dark = WidgetSettings.darkness(context) / 100f
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.argb(25,0,0,0), Color.argb((dark*160).toInt(),0,0,0), Color.argb((dark*230).toInt(),0,0,0)),
            floatArrayOf(0f,.45f,1f), Shader.TileMode.CLAMP)
        canvas.drawRect(bounds, paint)
        paint.shader = null
        val verse = card?.verse ?: Verse("Be still, and know that I am God.", "Psalm 46:10")
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setShadowLayer(w*.003f, 0f, w*.002f, Color.BLACK)
        }
        val ref = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255,237,185)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val maxWidth = (w*.82f).toInt()
        val availableHeight = h*.72f
        var size = WidgetSettings.textSize(context) * w / 340f
        var quote: StaticLayout
        var reference: StaticLayout
        var gap: Float
        while (true) {
            text.textSize = size
            ref.textSize = size*.70f
            quote = layout(verse.text, text, maxWidth)
            reference = layout(verse.reference, ref, maxWidth)
            gap = size*.42f
            if (quote.height + reference.height + gap <= availableHeight || size <= 10f) break
            size *= .94f
        }
        val total = quote.height + reference.height + gap
        val top = (h-total)/2f
        paint.color = Color.argb((dark*235).toInt(), 0,0,0)
        canvas.drawRoundRect(RectF(w*.055f, (top-h*.06f).coerceAtLeast(h*.03f), w*.945f,
            (top+total+h*.06f).coerceAtMost(h*.97f)), w*.025f,w*.025f,paint)
        canvas.save()
        canvas.translate((w-maxWidth)/2f, top)
        quote.draw(canvas)
        canvas.translate(0f, quote.height+gap)
        reference.draw(canvas)
        canvas.restore()
        return bitmap
    }
    private fun layout(value: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(value,0,value.length,paint,width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
            .setLineSpacing(0f,1.12f).build()
}
