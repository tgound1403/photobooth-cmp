package com.example.cameraxapp.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatePhotoBoothImageUseCase {
    suspend fun execute(context: Context, imagePaths: List<String>): Result<String> = runCatching {
        if (imagePaths.size != 4) {
            throw IllegalArgumentException("Cần chính xác 4 ảnh để tạo photobooth")
        }

        // Đọc và resize các ảnh
        val bitmaps = imagePaths.mapNotNull { path ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Giảm kích thước ảnh để tránh OOM
            }
            BitmapFactory.decodeFile(path, options)
        }

        if (bitmaps.size != 4) {
            throw IOException("Không thể đọc một hoặc nhiều ảnh")
        }

        // Padding cho ảnh photobooth
        val padding = 40 // Padding từ lề vào (dp)
        val paddingPx = (padding * context.resources.displayMetrics.density).toInt()
        
        // Khoảng cách giữa các ảnh
        val spacing = 20 // Khoảng cách giữa các ảnh (dp)
        val spacingPx = (spacing * context.resources.displayMetrics.density).toInt()
        
        // Khoảng trống ở dưới cho ngày chụp
        val bottomPadding = 120 // Khoảng trống dưới (dp)
        val bottomPaddingPx = (bottomPadding * context.resources.displayMetrics.density).toInt()

        val singleImageWidth = bitmaps[0].width
        val singleImageHeight = bitmaps[0].height
        
        // Tính toán kích thước canvas mới với padding
        val totalWidth = singleImageWidth + (paddingPx * 2)
        val totalHeight = (singleImageHeight * 4) + (spacingPx * 3) + (paddingPx * 2) + bottomPaddingPx

        val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)

        // Vẽ từng ảnh lên canvas với padding
        var currentY = paddingPx
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, paddingPx.toFloat(), currentY.toFloat(), null)
            currentY += bitmap.height + spacingPx
        }

        // Thêm ngày chụp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 40f * context.resources.displayMetrics.density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        // Vẽ ngày chụp ở giữa khoảng trống dưới
        val textY = totalHeight - (bottomPaddingPx / 2)
        canvas.drawText(currentDate, totalWidth / 2f, textY.toFloat(), paint)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoBooth_${System.currentTimeMillis()}.jpg")
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
        resultBitmap.recycle()

        uri.toString()
    }

    suspend fun executeWithBackground(
        context: Context,
        imagePaths: List<String>,
        backgroundSource: String
    ): Result<String> = runCatching {
        if (imagePaths.size != 4) {
            throw IllegalArgumentException("Cần chính xác 4 ảnh để tạo photobooth")
        }

        // Load background image
        val backgroundBitmap = loadBackgroundImage(context, backgroundSource)
            ?: throw IOException("Không thể load ảnh nền")

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

        // Tính toán kích thước và vị trí
        val padding = 40
        val paddingPx = (padding * context.resources.displayMetrics.density).toInt()
        val spacing = 20
        val spacingPx = (spacing * context.resources.displayMetrics.density).toInt()
        val bottomPadding = 120
        val bottomPaddingPx = (bottomPadding * context.resources.displayMetrics.density).toInt()

        val singleImageWidth = bitmaps[0].width
        val singleImageHeight = bitmaps[0].height

        val totalWidth = singleImageWidth + (paddingPx * 2)
        val totalHeight = (singleImageHeight * 4) + (spacingPx * 3) + (paddingPx * 2) + bottomPaddingPx

        // Resize background để phù hợp với kích thước canvas
        val resizedBackground = Bitmap.createScaledBitmap(
            backgroundBitmap,
            totalWidth,
            totalHeight,
            true
        )

        // Tạo canvas với background
        val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(resizedBackground, 0f, 0f, null)

        // Vẽ các ảnh photobooth lên background
        var currentY = paddingPx
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, paddingPx.toFloat(), currentY.toFloat(), null)
            currentY += bitmap.height + spacingPx
        }

        // Thêm ngày chụp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 40f * context.resources.displayMetrics.density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }

        val textY = totalHeight - (bottomPaddingPx / 2)
        canvas.drawText(currentDate, totalWidth / 2f, textY.toFloat(), paint)

        // Lưu ảnh
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoBooth_${System.currentTimeMillis()}.jpg")
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
        backgroundBitmap.recycle()
        resizedBackground.recycle()
        resultBitmap.recycle()

        uri.toString()
    }

    private suspend fun loadBackgroundImage(context: Context, source: String): Bitmap? = withContext(Dispatchers.IO) {
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