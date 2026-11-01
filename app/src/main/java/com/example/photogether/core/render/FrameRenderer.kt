package com.example.photogether.core.render

import android.graphics.Bitmap
import android.os.Build
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.photogether.data.model.FrameTheme
import com.example.photogether.data.model.FrameThemes
import com.example.photogether.data.model.ThemePattern
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
            return polaroid(src, mode, frame)
        }
        frame.poster?.let { mode ->
            return poster(src, mode, frame)
        }
        val theme = frame.themeId?.let { id -> FrameThemes.ALL_THEMES.firstOrNull { it.id == id } }
        if (theme == null && background == null) {
            return frame.caption?.let { captionOnImage(src, it, density) } ?: src
        }
        return themedFrame(src, theme, background, frame.caption, density)
    }

    private fun today(): String = SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date())

    /**
     * Polaroid frame: border colour and style from [frame], photo placed by [PolaroidGeometry],
     * caption (or the date when the caption is blank) in the strip given by the style.
     */
    private fun polaroid(src: Bitmap, mode: PolaroidMode, frame: FrameSpec): Bitmap {
        val style = PolaroidStyles.byId(frame.polaroidStyleId)
        val layout = PolaroidGeometry.compute(src.width, src.height, mode, style, frame.placement)
        val result =
            Bitmap.createBitmap(
                layout.frameWidth.roundToInt(),
                layout.frameHeight.roundToInt(),
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(result)
        paintPaper(canvas, layout.frameWidth, layout.frameHeight, frame.borderColor)

        val sr = layout.sourceRect
        val srcRect = Rect(sr.left.roundToInt(), sr.top.roundToInt(), sr.right.roundToInt(), sr.bottom.roundToInt())
        val d = layout.photoDest
        val area = layout.photoArea
        // FIT leaves bars around the photo inside the square window; fill them with a darker paper tone
        // so the window reads as a print window, not as a hole.
        if (mode == PolaroidMode.FIT) {
            canvas.drawRect(
                area.left, area.top, area.right, area.bottom,
                Paint().apply { color = shade(frame.borderColor, 0.9f) },
            )
        }
        canvas.drawBitmap(
            src,
            srcRect,
            RectF(d.left, d.top, d.right, d.bottom),
            Paint().apply { isFilterBitmap = true },
        )
        paintInnerShadow(canvas, area, layout.frameWidth)

        val raw = frame.caption?.takeIf { it.isNotBlank() } ?: today()
        val text = if (style.uppercase) raw.uppercase(Locale.getDefault()) else raw
        val box = layout.captionBox
        val textPaint =
            Paint().apply {
                color = shade(FrameColors.textColorOn(frame.borderColor), 1f, alpha = 0.82f)
                textSize = box.height * style.captionSizeRatio
                typeface = Typeface.create(style.fontFamily, if (style.bold) Typeface.BOLD else Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                letterSpacing = style.letterSpacing
                isAntiAlias = true
            }
        val maxWidth = box.width * 0.86f
        val measured = textPaint.measureText(text)
        if (measured > maxWidth) textPaint.textSize *= maxWidth / measured
        canvas.drawText(
            text,
            layout.frameWidth / 2f,
            box.top + box.height * style.baselineRatio,
            textPaint,
        )
        paintPaperEdge(canvas, layout.frameWidth, layout.frameHeight)
        return result
    }

    /** Paper: a faint top-to-bottom gradient of the border colour plus fine speckle so it is not a flat fill. */
    private fun paintPaper(canvas: Canvas, w: Float, h: Float, color: Int) {
        canvas.drawRect(
            0f, 0f, w, h,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h, shade(color, 1.02f), shade(color, 0.94f), Shader.TileMode.CLAMP)
            },
        )
        val speck = Paint().apply { style = Paint.Style.FILL; isAntiAlias = false }
        val rnd = Random(PAPER_SEED)
        val count = (w * h / 900f).toInt().coerceIn(200, 6000)
        val r = (w / 1500f).coerceAtLeast(0.6f)
        repeat(count) {
            speck.color = if (rnd.nextBoolean()) Color.argb(9, 0, 0, 0) else Color.argb(10, 255, 255, 255)
            canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, rnd.nextFloat() * r + 0.3f, speck)
        }
    }

    /** Soft dark edge just inside the photo window, as if the picture sat slightly below the paper. */
    private fun paintInnerShadow(canvas: Canvas, area: FloatRect, frameW: Float) {
        val depth = frameW * 0.012f
        val steps = 6
        val paint = Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.save()
        canvas.clipRect(area.left, area.top, area.right, area.bottom)
        for (i in 0 until steps) {
            val f = i / steps.toFloat()
            paint.strokeWidth = depth / steps * 2f
            paint.color = Color.argb(((1f - f) * 34f).toInt(), 0, 0, 0)
            val inset = depth / steps * i
            canvas.drawRect(area.left + inset, area.top + inset, area.right - inset, area.bottom - inset, paint)
        }
        canvas.restore()
        canvas.drawRect(
            area.left, area.top, area.right, area.bottom,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = (frameW / 900f).coerceAtLeast(1f); color = Color.argb(40, 0, 0, 0) },
        )
    }

    /** Faint darker rim around the sheet so a white border stays visible on a white background. */
    private fun paintPaperEdge(canvas: Canvas, w: Float, h: Float) {
        val t = (w / 600f).coerceAtLeast(1.5f)
        canvas.drawRect(
            t / 2f, t / 2f, w - t / 2f, h - t / 2f,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = t; color = Color.argb(30, 0, 0, 0) },
        )
    }

    /** Scales the RGB of [color] by [factor] (clamped); [alpha] overrides the alpha when given. */
    private fun shade(color: Int, factor: Float, alpha: Float? = null): Int {
        fun ch(v: Int) = (v * factor).roundToInt().coerceIn(0, 255)
        val a = alpha?.let { (it * 255).roundToInt().coerceIn(0, 255) } ?: Color.alpha(color)
        return Color.argb(a, ch(Color.red(color)), ch(Color.green(color)), ch(Color.blue(color)))
    }

    private const val PAPER_SEED = 7

    /**
     * Poster frame ("masthead"): header line and hairline, a title stretched across the photo
     * width with its baseline on the photo top, and a footer strip. In [PosterTextMode.CUTOUT]
     * the title is a window onto the photo, so the picture rises through the letters; in SOLID the
     * letters are plain ink.
     */
    private fun poster(src: Bitmap, mode: PosterTextMode, frame: FrameSpec): Bitmap {
        val layout = PosterGeometry.compute(src.width, src.height)
        val result =
            Bitmap.createBitmap(
                layout.frameWidth.roundToInt(),
                layout.frameHeight.roundToInt(),
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(result)
        canvas.drawColor(frame.borderColor)
        val ink = FrameColors.textColorOn(frame.borderColor)
        val d = layout.photoDest

        val infoPaint =
            Paint().apply {
                color = ink
                textSize = layout.infoSize
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
                letterSpacing = 0.12f
            }
        val rightPaint = Paint(infoPaint).apply { textAlign = Paint.Align.RIGHT }

        // Header: brand on the left, date on the right, hairline below.
        canvas.drawText("PHOTOGETHER", d.left, layout.headerBaselineY, infoPaint)
        canvas.drawText(posterDate(), d.right, layout.headerBaselineY, rightPaint)
        val rulePaint = Paint().apply { color = ink; strokeWidth = layout.infoSize * 0.12f; isAntiAlias = true }
        canvas.drawLine(d.left, layout.ruleY, d.right, layout.ruleY, rulePaint)

        // Footer: device on the left, aperture mark on the right.
        canvas.drawText("SHOT ON " + Build.MODEL.uppercase(Locale.US), d.left, layout.footerBaselineY, infoPaint)
        val ringR = layout.infoSize * 0.7f
        val ringY = layout.footerBaselineY - layout.infoSize * 0.35f
        val ringPaint =
            Paint().apply {
                color = ink
                style = Paint.Style.STROKE
                strokeWidth = layout.infoSize * 0.14f
                isAntiAlias = true
            }
        canvas.drawCircle(d.right - ringR, ringY, ringR, ringPaint)
        canvas.drawCircle(d.right - ringR, ringY, ringR * 0.4f, Paint(ringPaint).apply { style = Paint.Style.FILL })

        // Title: fitted to the photo width times titleScale (capped by height), centred, baseline on the photo top.
        val scale = frame.titleScale.coerceIn(PosterTitle.MIN_SCALE, 1f)
        val maxW = layout.titleMaxWidth * scale
        val maxH = layout.titleMaxHeight * scale
        val title = frame.caption?.takeIf { it.isNotBlank() } ?: SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val titlePaint =
            Paint().apply {
                color = ink
                textSize = maxH / 0.72f
                val font = PosterFonts.byId(frame.titleFontId)
                typeface = Typeface.create(
                    font.family,
                    when {
                        font.bold && font.italic -> Typeface.BOLD_ITALIC
                        font.bold -> Typeface.BOLD
                        font.italic -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    },
                )
                isAntiAlias = true
            }
        titlePaint.textSize *= maxW / titlePaint.measureText(title)
        if (titlePaint.textSize * 0.72f > maxH) titlePaint.textSize = maxH / 0.72f
        val titleWidth = titlePaint.measureText(title)
        val titleX = when (frame.titleAlign) {
            PosterTitleAlign.START -> d.left
            PosterTitleAlign.CENTER -> d.left + (d.width - titleWidth) / 2f
            PosterTitleAlign.END -> d.right - titleWidth
        }

        val paint = Paint().apply { isFilterBitmap = true }
        val photo = Path().apply { addRoundRect(RectF(d.left, d.top, d.right, d.bottom), layout.cornerRadius, layout.cornerRadius, Path.Direction.CW) }
        if (mode == PosterTextMode.SOLID) {
            canvas.drawText(title, titleX, d.top, titlePaint)
            canvas.save()
            canvas.clipPath(photo)
            canvas.drawBitmap(src, null, RectF(d.left, d.top, d.right, d.bottom), paint)
            canvas.restore()
        } else {
            // Window = rounded photo plus the letter shapes, united so overlaps do not cancel. The
            // photo covers the whole window, which makes it continue up through the letters.
            val glyphs = Path()
            titlePaint.getTextPath(title, 0, title.length, titleX, d.top, glyphs)
            val glyphBounds = RectF().also { glyphs.computeBounds(it, true) }
            val windowTop = minOf(glyphBounds.top, d.top)
            val window = Path(photo).apply { op(glyphs, Path.Op.UNION) }
            val s = PosterGeometry.coverSource(src.width, src.height, d.width, d.bottom - windowTop)
            canvas.save()
            canvas.clipPath(window)
            canvas.drawBitmap(
                src,
                Rect(s.left.roundToInt(), s.top.roundToInt(), s.right.roundToInt(), s.bottom.roundToInt()),
                RectF(d.left, windowTop, d.right, d.bottom),
                paint,
            )
            canvas.restore()
        }
        return result
    }

    private fun posterDate(): String = SimpleDateFormat("MMM dd yyyy", Locale.US).format(Date()).uppercase(Locale.US)

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
