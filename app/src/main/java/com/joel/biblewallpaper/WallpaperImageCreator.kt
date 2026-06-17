package com.joel.biblewallpaper

import android.graphics.*
import kotlin.math.roundToInt
import kotlin.random.Random

data class GeneratedWallpaper(val bitmap: Bitmap, val verse: Verse, val sceneName: String)

object WallpaperImageCreator {
    fun create(
        width: Int,
        height: Int,
        verse: Verse,
        sceneIndex: Int,
        seed: Long = System.nanoTime()
    ): GeneratedWallpaper {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = Random(seed)
        val scene = sceneIndex.coerceIn(0, SceneStyles.all.lastIndex)

        when (scene) {
            0 -> drawMountainSunrise(canvas, width, height, random)
            1 -> drawOceanSunset(canvas, width, height, random)
            2 -> drawForestMist(canvas, width, height, random)
            3 -> drawNightSky(canvas, width, height, random)
            4 -> drawLakeReflection(canvas, width, height, random)
            5 -> drawMeadowHills(canvas, width, height, random)
            else -> drawWaterfallValley(canvas, width, height, random)
        }

        drawReadableOverlay(canvas, width, height, verse)
        return GeneratedWallpaper(bitmap, verse, SceneStyles.all[scene])
    }

    private fun drawMountainSunrise(canvas: Canvas, w: Int, h: Int, random: Random) {
        val sky = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(Color.rgb(255, 185, 125), Color.rgb(116, 184, 215), Color.rgb(27, 74, 105)), null, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 238, 175); canvas.drawCircle(w * (.70f + random.nextFloat()*.18f), h * (.16f + random.nextFloat()*.12f), w * .11f, this) }
        drawClouds(canvas, w, h, random, Color.argb(85, 255, 255, 255))
        drawMountain(canvas, w, h, .60f, Color.rgb(61, 112, 128), random)
        drawMountain(canvas, w, h, .72f, Color.rgb(30, 76, 94), random)
        drawMountain(canvas, w, h, .84f, Color.rgb(15, 51, 66), random)
    }

    private fun drawOceanSunset(canvas: Canvas, w: Int, h: Int, random: Random) {
        val sky = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(Color.rgb(252, 148, 111), Color.rgb(119, 84, 149), Color.rgb(21, 49, 92)), null, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 214, 128); canvas.drawCircle(w * (.42f + random.nextFloat()*.18f), h * .34f, w * .14f, this) }
        val ocean = Paint().apply { shader = LinearGradient(0f, h*.48f, 0f, h.toFloat(), Color.rgb(28, 124, 158), Color.rgb(4, 48, 78), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, h*.48f, w.toFloat(), h.toFloat(), ocean)
        val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 255, 255); strokeWidth = 4f }
        repeat(16) { i ->
            val y = h*(.56f + random.nextFloat()*.34f)
            canvas.drawLine(i*w/16f, y, i*w/16f + w*(.08f + random.nextFloat()*.16f), y + random.nextInt(-8, 8), wave)
        }
    }

    private fun drawForestMist(canvas: Canvas, w: Int, h: Int, random: Random) {
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(194, 222, 205), Color.rgb(33, 82, 62), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        for (layer in 0..2) {
            val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20 - layer*3, 73 - layer*10, 54 - layer*8) }
            val top = h*(.15f + layer*.08f)
            val base = h*(.62f + layer*.14f)
            for (i in -2..10) {
                val x = i * w / 8f + random.nextInt(-35, 35)
                canvas.drawRect(x - 9 - layer*2, top + h*.18f, x + 9 + layer*2, h.toFloat(), treePaint)
                val path = Path().apply { moveTo(x, top); lineTo(x - w*(.11f + layer*.015f), base); lineTo(x + w*(.11f + layer*.015f), base); close() }
                canvas.drawPath(path, treePaint)
            }
        }
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(95,255,255,255); canvas.drawOval(-w*.1f, h*.34f, w*1.1f, h*.62f, this) }
    }

    private fun drawNightSky(canvas: Canvas, w: Int, h: Int, random: Random) {
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(13, 24, 61), Color.rgb(3, 8, 24), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        repeat(130) { canvas.drawCircle(random.nextInt(w).toFloat(), random.nextInt((h*.58f).roundToInt()).toFloat(), random.nextInt(1, 4).toFloat(), star) }
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(230, 231, 220); canvas.drawCircle(w*.75f, h*.18f, w*.08f, this); color = Color.rgb(13,24,61); canvas.drawCircle(w*.78f, h*.15f, w*.08f, this) }
        drawMountain(canvas, w, h, .76f, Color.rgb(18, 40, 61), random)
        drawMountain(canvas, w, h, .87f, Color.rgb(8, 25, 38), random)
    }

    private fun drawLakeReflection(canvas: Canvas, w: Int, h: Int, random: Random) {
        val sky = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(133, 190, 222), Color.rgb(21, 80, 110), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)
        drawMountain(canvas, w, h, .48f, Color.rgb(58, 111, 125), random)
        drawMountain(canvas, w, h, .56f, Color.rgb(25, 75, 92), random)
        val lake = Paint().apply { shader = LinearGradient(0f, h*.52f, 0f, h.toFloat(), Color.rgb(52, 132, 157), Color.rgb(12, 56, 86), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, h*.52f, w.toFloat(), h.toFloat(), lake)
        val ripple = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 255, 255, 255); strokeWidth = 3f }
        repeat(18) { canvas.drawLine(random.nextInt(w).toFloat(), h*(.56f + random.nextFloat()*.36f), random.nextInt(w).toFloat(), h*(.56f + random.nextFloat()*.36f), ripple) }
    }

    private fun drawMeadowHills(canvas: Canvas, w: Int, h: Int, random: Random) {
        val sky = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(175, 220, 240), Color.rgb(236, 247, 209), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)
        drawClouds(canvas, w, h, random, Color.argb(125, 255, 255, 255))
        val hill1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(91, 161, 87) }
        canvas.drawOval(-w*.2f, h*.55f, w*.9f, h*1.05f, hill1)
        val hill2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(50, 129, 74) }
        canvas.drawOval(w*.25f, h*.50f, w*1.25f, h*1.04f, hill2)
        val flower = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 255, 245, 210) }
        repeat(75) { canvas.drawCircle(random.nextInt(w).toFloat(), h*(.68f + random.nextFloat()*.28f), random.nextInt(2, 5).toFloat(), flower) }
    }

    private fun drawWaterfallValley(canvas: Canvas, w: Int, h: Int, random: Random) {
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(117, 183, 199), Color.rgb(18, 74, 59), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        drawMountain(canvas, w, h, .60f, Color.rgb(42, 105, 91), random)
        drawMountain(canvas, w, h, .76f, Color.rgb(16, 75, 61), random)
        val water = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(w*.48f, h*.36f, w*.55f, h*.9f, Color.rgb(235, 255, 255), Color.rgb(70, 166, 185), Shader.TileMode.CLAMP) }
        val path = Path().apply { moveTo(w*.45f, h*.34f); cubicTo(w*.55f, h*.48f, w*.44f, h*.62f, w*.54f, h*.78f); lineTo(w*.66f, h*.78f); cubicTo(w*.56f, h*.61f, w*.67f, h*.48f, w*.58f, h*.34f); close() }
        canvas.drawPath(path, water)
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 255, 255); canvas.drawOval(w*.32f, h*.72f, w*.78f, h*.84f, this) }
    }

    private fun drawClouds(canvas: Canvas, w: Int, h: Int, random: Random, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        repeat(5) {
            val x = random.nextInt(0, w)
            val y = random.nextInt((h*.06f).roundToInt(), (h*.28f).roundToInt())
            canvas.drawOval(x - w*.16f, y - h*.025f, x + w*.16f, y + h*.035f, p)
        }
    }

    private fun drawMountain(canvas: Canvas, w: Int, h: Int, base: Float, color: Int, random: Random) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val path = Path().apply {
            moveTo(0f, h.toFloat())
            lineTo(0f, h*base)
            var x = 0f
            while (x <= w) {
                val peak = h * (base - (.08f + random.nextFloat()*.18f))
                lineTo(x + w*.13f, peak)
                lineTo(x + w*.27f, h * (base - random.nextFloat()*.04f))
                x += w*.27f
            }
            lineTo(w.toFloat(), h.toFloat())
            close()
        }
        canvas.drawPath(path, p)
    }

    private fun drawReadableOverlay(canvas: Canvas, w: Int, h: Int, verse: Verse) {
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(145, 0, 0, 0) }
        val boxRect = RectF(w*.075f, h*.30f, w*.925f, h*.67f)
        canvas.drawRoundRect(boxRect, 44f, 44f, shadow)

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

        val lines = wrapText(verse.text, textPaint, w*.72f)
        val lineHeight = textPaint.textSize * 1.24f
        val totalHeight = lines.size * lineHeight + refPaint.textSize * 1.8f
        var y = h*.485f - totalHeight/2 + lineHeight
        for (line in lines) { canvas.drawText(line, w/2f, y, textPaint); y += lineHeight }
        canvas.drawText(verse.reference, w/2f, y + refPaint.textSize*.75f, refPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) current = test else { if (current.isNotEmpty()) lines.add(current); current = word }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }
}
