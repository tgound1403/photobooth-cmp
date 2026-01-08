package com.example.cameraxapp.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.example.cameraxapp.shared.domain.model.ImageFilter
import com.example.cameraxapp.shared.domain.model.PhotoBoothLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatePhotoBoothImageUseCase {
    suspend fun execute(
        context: Context,
        imagePaths: List<String>,
        layout: PhotoBoothLayout = PhotoBoothLayout.GRID_2X2,
        filter: ImageFilter = ImageFilter.ORIGINAL,
        backgroundSource: String? = null
    ): Result<String> = runCatching {
        if (imagePaths.size != 4) {
            throw IllegalArgumentException("Cần chính xác 4 ảnh để tạo photobooth")
        }

        // Load background image nếu có
        val backgroundBitmap = backgroundSource?.let { source ->
            loadBackgroundImage(context, source)
        }

        // Đọc và resize các ảnh photobooth
        val bitmaps = imagePaths.mapNotNull { path ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            BitmapFactory.decodeFile(path, options)
        }

        if (bitmaps.size != 4) {
            throw IOException("Không thể đọc một hoặc nhiều ảnh")
        }

        // Apply filter to bitmaps BEFORE drawing (optional optimization: apply paint filter during draw)
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
            PhotoBoothLayout.STRIP_1X4 -> {
                totalWidth = singleImageWidth + (paddingPx * 2)
                totalHeight =
                    (singleImageHeight * 4) + (spacingPx * 3) + (paddingPx * 2) + bottomPaddingPx
            }
        }

        // Tạo canvas
        val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Vẽ background hoặc nền trắng
        if (backgroundBitmap != null) {
            val resizedBackground = Bitmap.createScaledBitmap(
                backgroundBitmap,
                totalWidth,
                totalHeight,
                true
            )
            canvas.drawBitmap(resizedBackground, 0f, 0f, null)
            resizedBackground.recycle()
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // Prepare Paint with Filter
        val paintFilter = Paint().apply {
            colorFilter = when (filter) {
                ImageFilter.BLACK_AND_WHITE -> {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f)
                    ColorMatrixColorFilter(matrix)
                }
                ImageFilter.SEPIA -> {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f) // Desaturate first
                    // Sepia matrix provided by Android docs/StackOverflow common impl
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
                canvas.drawBitmap(
                    bitmaps[2],
                    paddingPx.toFloat(),
                    row2Y.toFloat(),
                    paintFilter
                )
                canvas.drawBitmap(
                    bitmaps[3],
                    (paddingPx + singleImageWidth + spacingPx).toFloat(),
                    row2Y.toFloat(),
                    paintFilter
                )
            }
            PhotoBoothLayout.STRIP_1X4 -> {
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

        val textPaint = Paint().apply {
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
        val contentValues = ContentValues().apply {
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
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use { outputStream ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        } ?: throw IOException("Failed to open output stream.")

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

    private suspend fun loadBackgroundImage(context: Context, source: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                when {
                    source.startsWith("http") -> {
                        // Load từ URL (Unsplash)
                        val request = ImageRequest.Builder(context)
                            .data(source)
                            .build()
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
