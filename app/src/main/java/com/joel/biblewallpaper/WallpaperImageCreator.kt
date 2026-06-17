package com.joel.biblewallpaper

import android.graphics.*

data class GeneratedWallpaper(
    val bitmap: Bitmap,
    val verse: Verse,
    val sceneName: String
)

object WallpaperImageCreator {
    fun createFromPhoto(
        source: Bitmap,
        width: Int,
        height: Int,
        verse: Verse,
        photoId: String
    ): GeneratedWallpaper {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        drawPhotoCover(canvas, source, width, height)
        drawReadableOverlay(canvas, width, height, verse)

        return GeneratedWallpaper(
            bitmap = output,
            verse = verse,
            sceneName = "Pexels Photo $photoId"
        )
    }

    private fun drawPhotoCover(canvas: Canvas, source: Bitmap, w: Int, h: Int) {
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val dstRatio = w.toFloat() / h.toFloat()

        val srcRect = if (srcRatio > dstRatio) {
            val newWidth = (source.height * dstRatio).toInt()
            val left = (source.width - newWidth) / 2
            Rect(left, 0, left + newWidth, source.height)
        } else {
            val newHeight = (source.width / dstRatio).toInt()
            val top = (source.height - newHeight) / 2
            Rect(0, top, source.width, top + newHeight)
        }

        val dstRect = Rect(0, 0, w, h)
        canvas.drawBitmap(source, srcRect, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))
    }

    private fun drawReadableOverlay(canvas: Canvas, w: Int, h: Int, verse: Verse) {
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dimPaint)

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 0, 0)
        }
        val boxRect = RectF(w * .075f, h * .30f, w * .925f, h * .67f)
        canvas.drawRoundRect(boxRect, 44f, 44f, boxPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = w * .062f
        }

        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 237, 175)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = w * .043f
        }

        val lines = wrapText(verse.text, textPaint, w * .72f)
        val lineHeight = textPaint.textSize * 1.24f
        val totalHeight = lines.size * lineHeight + refPaint.textSize * 1.8f

        var y = h * .485f - totalHeight / 2 + lineHeight

        for (line in lines) {
            canvas.drawText(line, w / 2f, y, textPaint)
            y += lineHeight
        }

        canvas.drawText(verse.reference, w / 2f, y + refPaint.textSize * .75f, refPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""

        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"

            if (paint.measureText(test) <= maxWidth) {
                current = test
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }

        if (current.isNotEmpty()) lines.add(current)

        return lines
    }
}
