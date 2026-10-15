package com.example.cameraxapp.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.cameraxapp.data.model.FrameTheme
import com.example.cameraxapp.data.model.FrameThemes
import com.example.cameraxapp.data.model.ThemePattern
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

/** Draws the frame around a photo: Polaroid (Fit or Fill), a theme or background image, and a caption. */
object FrameRenderer {

    private const val DATE_PATTERN = "dd.MM.yyyy"

    fun apply(src: Bitmap, frame: FrameSpec, background: Bitmap?, density: Float): Bitmap {
        frame.polaroid?.let { mode ->
            return polaroid(src, mode, frame.caption ?: today())
        }
        val theme = frame.themeId?.let { id -> FrameThemes.ALL_THEMES.firstOrNull { it.id == id } }
        if (theme == null && background == null) {
            return frame.caption?.let { captionOnImage(src, it, density) } ?: src
        }
        return themedFrame(src, theme, background, frame.caption, density)
    }

    private fun today(): String = SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date())

    /** Polaroid frame: white border, photo placed by [PolaroidGeometry], date in the bottom strip. */
    private fun polaroid(src: Bitmap, mode: PolaroidMode, dateText: String): Bitmap {
        val layout = PolaroidGeometry.compute(src.width, src.height, mode)
        val result =
            Bitmap.createBitmap(
                layout.frameWidth.roundToInt(),
                layout.frameHeight.roundToInt(),
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val sr = layout.sourceRect
        val srcRect = Rect(sr.left.roundToInt(), sr.top.roundToInt(), sr.right.roundToInt(), sr.bottom.roundToInt())
        val d = layout.photoDest
        canvas.drawBitmap(
            src,
            srcRect,
            RectF(d.left, d.top, d.right, d.bottom),
            Paint().apply { isFilterBitmap = true },
        )

        val strip = layout.frameHeight - layout.photoArea.bottom
        val textPaint =
            Paint().apply {
                color = Color.BLACK
                textSize = strip * 0.35f
                typeface = Typeface.create("sans-serif-bold", Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
        canvas.drawText(
            dateText,
            layout.frameWidth / 2f,
            layout.photoArea.bottom + strip * 0.6f,
            textPaint,
        )
        addPolaroidTexture(canvas, layout.frameWidth, layout.frameHeight)
        return result
    }

    private fun addPolaroidTexture(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply { style = Paint.Style.FILL; color = Color.argb(5, 0, 0, 0) }
        val random = Random
        for (i in 0..200) {
            canvas.drawCircle(random.nextFloat() * width, random.nextFloat() * height, random.nextFloat() * 2f, paint)
        }
    }

    /** Theme or background image around the photo, with an optional caption at the bottom. */
    private fun themedFrame(
        src: Bitmap,
        theme: FrameTheme?,
        background: Bitmap?,
        caption: String?,
        density: Float,
    ): Bitmap {
        val padding = (20 * density).toInt()
        val bottomPadding = if (caption != null) (80 * density).toInt() else padding
        val width = src.width + padding * 2
        val height = src.height + padding * 2 + bottomPadding
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        if (background != null) {
            val scaled = Bitmap.createScaledBitmap(background, width, height, true)
            canvas.drawBitmap(scaled, 0f, 0f, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
        } else if (theme != null) {
            drawThemedBackground(canvas, width, height, theme)
        }

        canvas.drawBitmap(src, padding.toFloat(), padding.toFloat(), null)

        caption?.let { text ->
            val textPaint =
                Paint().apply {
                    color = Color.WHITE
                    textSize = 40f * density
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    setShadowLayer(5f, 0f, 0f, Color.BLACK)
                }
            canvas.drawText(text, width / 2f, (height - bottomPadding / 2).toFloat(), textPaint)
        }
        return result
    }

    /** Caption with no frame: white text with a dark shadow over the bottom of the photo. */
    private fun captionOnImage(src: Bitmap, text: String, density: Float): Bitmap {
        val result = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val paint =
            Paint().apply {
                color = Color.WHITE
                textSize = 40f * density
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                setShadowLayer(5f, 0f, 0f, Color.BLACK)
            }
        Canvas(result).drawText(text, src.width / 2f, src.height - 50f * density, paint)
        return result
    }

    private fun drawThemedBackground(canvas: Canvas, width: Int, height: Int, theme: FrameTheme) {
        when (theme.pattern) {
            ThemePattern.SOLID -> canvas.drawColor(theme.backgroundColor)
            ThemePattern.GRADIENT -> {
                val paint =
                    Paint().apply {
                        shader =
                            LinearGradient(
                                0f,
                                0f,
                                0f,
                                height.toFloat(),
                                theme.backgroundColor,
                                theme.accentColor ?: theme.backgroundColor,
                                Shader.TileMode.CLAMP,
                            )
                    }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            ThemePattern.DOTS -> {
                canvas.drawColor(theme.backgroundColor)
                val dotPaint =
                    Paint().apply {
                        color = theme.accentColor ?: Color.WHITE
                        alpha = 100
                        style = Paint.Style.FILL
                    }
                for (y in 0 until height step 30) {
                    for (x in 0 until width step 30) {
                        canvas.drawCircle(x.toFloat(), y.toFloat(), 10f, dotPaint)
                    }
                }
            }
            ThemePattern.STRIPES -> {
                canvas.drawColor(theme.backgroundColor)
                val stripePaint =
                    Paint().apply {
                        color = theme.accentColor ?: Color.WHITE
                        alpha = 80
                        style = Paint.Style.FILL
                    }
                var x = 0f
                while (x < width) {
                    canvas.drawRect(x, 0f, x + 40f, height.toFloat(), stripePaint)
                    x += 80f
                }
            }
        }
    }
}
