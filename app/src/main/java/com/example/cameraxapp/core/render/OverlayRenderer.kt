package com.example.cameraxapp.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.graphics.withTranslation
import kotlin.math.min
import kotlin.random.Random

/** Labels offered by the overlay panel. The ids are what [OverlaySpec] stores. */
object OverlayCatalog {
    val lightLeaks = listOf("Light leak 1", "Light leak 2", "Light leak 3", "Light leak 4", "Light leak 5")
    val scratches = listOf("Scratch 1", "Scratch 2", "Scratch 3", "Scratch 4", "Scratch 5")
    val distortions = listOf("Distortion 1", "Distortion 2", "Distortion 3", "Distortion 4", "Distortion 5")
    val stickers = listOf("❤️", "⭐", "✨", "🎉", "🎈", "🌈", "☀️", "🌙", "💫", "🔥")
}

/**
 * CPU effects painted on top of the GPU grade: light leak, scratch, distortion presets,
 * chromatic aberration and the sticker/text items. Behaviour is the same as the former
 * EditImageViewModel helpers.
 */
object OverlayRenderer {

    /** Size of a sticker or text item at scale 1, as a fraction of the output's shorter side. */
    private const val ITEM_UNIT_SIDE = 1000f

    private fun Bitmap.mutableCopy(): Bitmap = copy(config ?: Bitmap.Config.ARGB_8888, true)

    fun applyScratch(bitmap: Bitmap, type: String): Bitmap {
        val result = bitmap.mutableCopy()
        val canvas = Canvas(result)
        val paint =
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = 150
                isAntiAlias = true
            }
        // Deterministic random based on the label, so the same scratch always looks the same.
        val random = Random(type.hashCode())
        val count = 20 + random.nextInt(30)
        for (i in 0 until count) {
            val startX = random.nextFloat() * bitmap.width
            val startY = random.nextFloat() * bitmap.height
            val endX = startX + (random.nextFloat() - 0.5f) * 100
            val endY = startY + (random.nextFloat() - 0.5f) * 100
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        return result
    }

    fun applyChromaticAberration(bitmap: Bitmap, intensity: Float): Bitmap {
        val shift = 20f * intensity
        val result = createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Red channel shifted left.
        paint.colorFilter = LightingColorFilter(0xFFFF0000.toInt(), 0)
        canvas.drawBitmap(bitmap, -shift, 0f, paint)

        // Green channel centred, added on top.
        paint.colorFilter = LightingColorFilter(0xFF00FF00.toInt(), 0)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Blue channel shifted right.
        paint.colorFilter = LightingColorFilter(0xFF0000FF.toInt(), 0)
        canvas.drawBitmap(bitmap, shift, 0f, paint)

        paint.xfermode = null
        return result
    }

    fun applyLightLeak(bitmap: Bitmap, type: String): Bitmap {
        val result = bitmap.mutableCopy()
        val canvas = Canvas(result)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // Deterministic "random" based on the label.
        val hash = type.hashCode()
        val cx = if (hash % 2 == 0) 0f else width
        val cy = if (hash % 3 == 0) 0f else height
        val radius = width.coerceAtLeast(height) * (0.5f + (hash % 5) / 10f)

        val colors =
            intArrayOf(
                Color.argb(150, 255, 200, 0), // Orange/yellow
                Color.argb(100, 255, 100, 0), // Reddish
                Color.TRANSPARENT,
            )
        val gradient = RadialGradient(cx, cy, radius, colors, null, Shader.TileMode.CLAMP)
        val paint =
            Paint().apply {
                shader = gradient
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                isAntiAlias = true
            }
        canvas.drawRect(0f, 0f, width, height, paint)
        return result
    }

    fun applyGrain(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.mutableCopy()
        val canvas = Canvas(result)
        val paint =
            Paint().apply {
                alpha = (intensity * 100).toInt()
                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            }
        val random = Random(System.currentTimeMillis())
        for (i in 0 until 50) {
            val noise = createBitmap(100, 100)
            for (x in 0 until 100) {
                for (y in 0 until 100) {
                    val n = random.nextInt(255)
                    noise[x, y] = Color.argb(255, n, n, n)
                }
            }
            canvas.drawBitmap(noise, null, Rect(0, 0, width, height), paint)
        }
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            if (random.nextFloat() < intensity * 0.2f) {
                val delta = random.nextInt(50) - 25
                val p = pixels[i]
                val r = (Color.red(p) + delta).coerceIn(0, 255)
                val g = (Color.green(p) + delta).coerceIn(0, 255)
                val b = (Color.blue(p) + delta).coerceIn(0, 255)
                pixels[i] = Color.argb(Color.alpha(p), r, g, b)
            }
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.mutableCopy()
        val canvas = Canvas(result)
        val radius = width.coerceAtLeast(height) * 0.9f
        val gradient =
            RadialGradient(
                width / 2f,
                height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(1f - intensity, 1f),
                Shader.TileMode.CLAMP,
            )
        val paint =
            Paint().apply {
                shader = gradient
                alpha = (intensity * 255).toInt()
                isAntiAlias = true
            }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return result
    }

    /** Distortion presets map onto the CPU effects above, with the same strengths as before. */
    fun applyDistortion(bitmap: Bitmap, type: String): Bitmap =
        when (type) {
            "Distortion 1" -> applyChromaticAberration(bitmap, 0.3f)
            "Distortion 2" -> applyChromaticAberration(bitmap, 0.6f)
            "Distortion 3" -> applyGrain(bitmap, 0.5f)
            "Distortion 4" -> applyVignette(bitmap, 0.7f)
            "Distortion 5" -> applyChromaticAberration(bitmap, 1.0f)
            else -> bitmap
        }

    /**
     * Paints sticker and text items. Positions are fractions of the bitmap size, so the result is
     * the same on the preview and the full-resolution export.
     */
    fun applyItems(bitmap: Bitmap, items: List<OverlayItem>): Bitmap {
        if (items.isEmpty()) return bitmap
        val result = bitmap.mutableCopy()
        val canvas = Canvas(result)
        val unit = min(result.width, result.height) / ITEM_UNIT_SIDE
        items.forEach { item ->
            val isSticker = item.kind == OverlayKind.STICKER
            val paint =
                Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = (if (isSticker) 80f else 60f) * unit
                    color = if (isSticker) Color.BLACK else Color.WHITE
                    if (!isSticker) setShadowLayer(5f * unit, 0f, 0f, Color.BLACK)
                }
            canvas.withTranslation(item.x * result.width, item.y * result.height) {
                rotate(item.rotation)
                scale(item.scale, item.scale)
                drawText(item.content, 0f, 0f, paint)
            }
        }
        return result
    }
}
