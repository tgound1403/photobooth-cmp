package com.example.photogether.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Paints a [ViewfinderSkin] onto a photo. Every size is a fraction of the shorter side, so the editor
 * preview (rendered small) and the full-resolution export look the same.
 */
object SkinRenderer {

    /** Returns [src] unchanged for [ViewfinderSkin.NONE], otherwise a mutable copy with the skin painted on it. */
    fun apply(src: Bitmap, skin: ViewfinderSkin): Bitmap {
        if (skin == ViewfinderSkin.NONE) return src
        val result = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val short = minOf(result.width, result.height).toFloat()
        when (skin) {
            ViewfinderSkin.NONE -> Unit
            ViewfinderSkin.FILM_FRAME -> filmFrame(canvas, short)
            ViewfinderSkin.RANGEFINDER -> rangefinder(canvas, short, Color.argb(179, 255, 255, 255))
            ViewfinderSkin.RANGEFINDER_MONO -> {
                rangefinder(canvas, short, Color.argb(217, 0xB0, 0xB0, 0xB0))
                innerBorder(canvas, short, Color.argb(153, 0x9A, 0x9A, 0x9A))
            }
            ViewfinderSkin.SOFT_GLASS -> softGlass(canvas)
            ViewfinderSkin.LCD_DIGICAM -> lcdDigicam(canvas, short)
        }
        return result
    }

    private fun filmFrame(canvas: Canvas, short: Float) {
        val inset = short * 0.02f
        val paint = strokePaint(Color.argb(217, 0xF2, 0xE8, 0xD5), short * 0.0025f)
        canvas.drawRect(inset, inset, canvas.width - inset, canvas.height - inset, paint)
        textPaint(Color.rgb(0xFF, 0x8A, 0x2B), short * 0.03f).let { text ->
            canvas.drawText("24A", short * 0.07f, canvas.height - short * 0.06f, text)
        }
    }

    /** Corner brackets and a centre focus box, drawn in [color]. */
    private fun rangefinder(canvas: Canvas, short: Float, color: Int) {
        val paint = strokePaint(color, short * 0.004f)
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val margin = short * 0.03f
        val len = short * 0.04f
        canvas.drawLine(margin, margin, margin + len, margin, paint)
        canvas.drawLine(margin, margin, margin, margin + len, paint)
        canvas.drawLine(w - margin, h - margin, w - margin - len, h - margin, paint)
        canvas.drawLine(w - margin, h - margin, w - margin, h - margin - len, paint)
        val fw = w * 0.22f
        val fh = fw * 0.75f
        canvas.drawRect(w / 2f - fw / 2f, h / 2f - fh / 2f, w / 2f + fw / 2f, h / 2f + fh / 2f, paint)
    }

    private fun innerBorder(canvas: Canvas, short: Float, color: Int) {
        val inset = short * 0.01f
        canvas.drawRect(inset, inset, canvas.width - inset, canvas.height - inset, strokePaint(color, short * 0.0025f))
    }

    /** Warm centre-to-edge vignette with a faint amber tint. */
    private fun softGlass(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val paint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                w / 2f,
                h / 2f,
                maxOf(w, h) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.argb(31, 255, 179, 71), Color.argb(89, 0, 0, 0)),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    /** Faint scanlines, a red REC dot and label in the top-left corner. */
    private fun lcdDigicam(canvas: Canvas, short: Float) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val step = (short * 0.008f).coerceAtLeast(4f)
        val line = Paint().apply { color = Color.argb(20, 0, 0, 0); strokeWidth = 1f }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, line)
            y += step
        }
        val dotRadius = short * 0.01f
        val dotX = short * 0.065f
        val dotY = short * 0.045f
        val red = Color.rgb(0xE5, 0x39, 0x35)
        canvas.drawCircle(dotX, dotY, dotRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = red })
        val label = textPaint(red, short * 0.03f)
        canvas.drawText("REC", dotX + dotRadius + short * 0.012f, dotY + label.textSize * 0.35f, label)
    }

    private fun strokePaint(color: Int, width: Float) = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = width
    }

    private fun textPaint(color: Int, size: Float) = Paint().apply {
        isAntiAlias = true
        this.color = color
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
    }
}
