package com.example.cameraxapp.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.example.cameraxapp.data.model.FrameTheme
import com.example.cameraxapp.data.model.ThemePattern
import com.example.cameraxapp.shared.domain.model.ImageFilter
import com.example.cameraxapp.shared.domain.model.PhotoBoothLayout
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreatePhotoBoothImageUseCase {
    suspend fun execute(
            context: Context,
            imagePaths: List<String>,
            layout: PhotoBoothLayout = PhotoBoothLayout.GRID_2X2,
            filter: ImageFilter = ImageFilter.ORIGINAL,
            backgroundSource: String? = null,
            frameTheme: FrameTheme? = null
    ): Result<String> = runCatching {
        val requiredCount =
                when (layout) {
                    PhotoBoothLayout.SINGLE -> 1
                    PhotoBoothLayout.STRIP_1X2 -> 2
                    PhotoBoothLayout.STRIP_1X3 -> 3
                    PhotoBoothLayout.GRID_2X2, PhotoBoothLayout.STRIP_1X4 -> 4
                }

        if (imagePaths.size != requiredCount) {
            throw IllegalArgumentException("Cần chính xác $requiredCount ảnh cho layout $layout")
        }

        // Load background image nếu có
        val backgroundBitmap =
                backgroundSource?.let { source -> loadBackgroundImage(context, source) }

        // Đọc và resize các ảnh photobooth
        val bitmaps =
                imagePaths.mapNotNull { path ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(path, options)
                }

        if (bitmaps.size != requiredCount) {
            throw IOException("Không thể đọc một hoặc nhiều ảnh")
        }

        // Apply filter to bitmaps BEFORE drawing (optional optimization: apply paint filter during
        // draw)
        // We will apply filter during draw using Paint

        // Tính toán kích thước và vị trí
        val density = context.resources.displayMetrics.density
        val paddingPx = (40 * density).toInt()
        val spacingPx = (20 * density).toInt()
        val bottomPaddingPx = (120 * density).toInt()

        val singleImageWidth = bitmaps[0].width
        val singleImageHeight = bitmaps[0].height

        val totalWidth: Int
        val totalHeight: Int

        when (layout) {
            PhotoBoothLayout.GRID_2X2 -> {
                totalWidth = (singleImageWidth * 2) + (paddingPx * 2) + spacingPx
                totalHeight =
                        (singleImageHeight * 2) + (paddingPx * 2) + spacingPx + bottomPaddingPx
            }
            PhotoBoothLayout.SINGLE -> {
                totalWidth = singleImageWidth + (paddingPx * 2)
                totalHeight = singleImageHeight + (paddingPx * 2) + bottomPaddingPx
            }
            PhotoBoothLayout.STRIP_1X2, PhotoBoothLayout.STRIP_1X3, PhotoBoothLayout.STRIP_1X4 -> {
                val count = requiredCount
                totalWidth = singleImageWidth + (paddingPx * 2)
                totalHeight =
                        (singleImageHeight * count) +
                                (spacingPx * (count - 1)) +
                                (paddingPx * 2) +
                                bottomPaddingPx
            }
        }

        // Tạo canvas
        val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Vẽ background với theme nếu có
        if (frameTheme != null) {
            drawThemedBackground(canvas, totalWidth, totalHeight, frameTheme)
        } else if (backgroundBitmap != null) {
            val resizedBackground =
                    Bitmap.createScaledBitmap(backgroundBitmap, totalWidth, totalHeight, true)
            canvas.drawBitmap(resizedBackground, 0f, 0f, null)
            resizedBackground.recycle()
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // Prepare Paint with Filter
        val paintFilter =
                Paint().apply {
                    colorFilter =
                            when (filter) {
                                ImageFilter.BLACK_AND_WHITE -> {
                                    val matrix = ColorMatrix()
                                    matrix.setSaturation(0f)
                                    ColorMatrixColorFilter(matrix)
                                }
                                ImageFilter.SEPIA -> {
                                    val matrix = ColorMatrix()
                                    matrix.setSaturation(0f) // Desaturate first
                                    // Sepia matrix provided by Android docs/StackOverflow common
                                    // impl
                                    val sepiaMatrix = ColorMatrix()
                                    sepiaMatrix.setScale(1f, 0.95f, 0.82f, 1f)
                                    matrix.postConcat(sepiaMatrix)
                                    ColorMatrixColorFilter(matrix)
                                }
                                ImageFilter.ORIGINAL -> null
                            }
                }

        // Vẽ các ảnh photobooth theo layout
        when (layout) {
            PhotoBoothLayout.GRID_2X2 -> {
                // Row 1
                canvas.drawBitmap(bitmaps[0], paddingPx.toFloat(), paddingPx.toFloat(), paintFilter)
                canvas.drawBitmap(
                        bitmaps[1],
                        (paddingPx + singleImageWidth + spacingPx).toFloat(),
                        paddingPx.toFloat(),
                        paintFilter
                )

                // Row 2
                val row2Y = paddingPx + singleImageHeight + spacingPx
                canvas.drawBitmap(bitmaps[2], paddingPx.toFloat(), row2Y.toFloat(), paintFilter)
                canvas.drawBitmap(
                        bitmaps[3],
                        (paddingPx + singleImageWidth + spacingPx).toFloat(),
                        row2Y.toFloat(),
                        paintFilter
                )
            }
            else -> { // SINGLE, STRIP_1X2, STRIP_1X3, STRIP_1X4
                var currentY = paddingPx
                bitmaps.forEach { bitmap ->
                    canvas.drawBitmap(bitmap, paddingPx.toFloat(), currentY.toFloat(), paintFilter)
                    currentY += bitmap.height + spacingPx
                }
            }
        }

        // Thêm ngày chụp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val textPaint =
                Paint().apply {
                    color = if (backgroundBitmap != null) Color.WHITE else Color.BLACK
                    textSize = 40f * density
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                    if (backgroundBitmap != null) {
                        setShadowLayer(5f, 0f, 0f, Color.BLACK)
                    }
                }

        val textY = totalHeight - (bottomPaddingPx / 2)
        canvas.drawText(currentDate, totalWidth / 2f, textY.toFloat(), textPaint)

        // Lưu ảnh
        val contentValues =
                ContentValues().apply {
                    put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            "PhotoBooth_${System.currentTimeMillis()}.jpg"
                    )
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

        val resolver = context.contentResolver
        val uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use { outputStream ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
                ?: throw IOException("Failed to open output stream.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        // Giải phóng bộ nhớ
        bitmaps.forEach { it.recycle() }
        backgroundBitmap?.recycle()
        resultBitmap.recycle()

        uri.toString()
    }

    private fun drawThemedBackground(
            canvas: Canvas,
            width: Int,
            height: Int,
            theme: FrameTheme
    ) {
        when (theme.pattern) {
            ThemePattern.SOLID -> {
                canvas.drawColor(theme.backgroundColor)
            }
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
                                    Shader.TileMode.CLAMP
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
                val dotSize = 10f
                val spacing = 30f
                for (y in 0 until height step spacing.toInt()) {
                    for (x in 0 until width step spacing.toInt()) {
                        canvas.drawCircle(x.toFloat(), y.toFloat(), dotSize, dotPaint)
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
                val stripeWidth = 40f
                val spacing = 80f
                var x = 0f
                while (x < width) {
                    canvas.drawRect(x, 0f, x + stripeWidth, height.toFloat(), stripePaint)
                    x += spacing
                }
            }
        }
    }

    private suspend fun loadBackgroundImage(context: Context, source: String): Bitmap? =
            withContext(Dispatchers.IO) {
                try {
                    when {
                        source.startsWith("http") -> {
                            // Load từ URL (Unsplash)
                            val request = ImageRequest.Builder(context).data(source).build()
                            val result = Coil.imageLoader(context).execute(request)
                            result.drawable?.toBitmap()
                        }
                        source.startsWith("content://") -> {
                            // Load từ URI (thiết bị)
                            val uri = Uri.parse(source)
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    Log.e("CreatePhotoBoothImageUseCase", "Error loading background image", e)
                    null
                }
            }
}
