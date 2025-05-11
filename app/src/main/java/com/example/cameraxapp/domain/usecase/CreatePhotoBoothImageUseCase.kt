package com.example.cameraxapp.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.IOException
import java.io.File

class CreatePhotoBoothImageUseCase {
    suspend fun execute(context: Context, imagePaths: List<String>): Result<String> = runCatching {
        if (imagePaths.size != 4) {
            throw IllegalArgumentException("Cần chính xác 4 ảnh để tạo photobooth")
        }

        // Đọc và resize các ảnh
        val bitmaps = imagePaths.mapNotNull { path ->
            val file = File(path)
            if (!file.exists()) {
                throw IOException("File không tồn tại: $path")
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Giảm kích thước ảnh để tránh OOM
            }
            BitmapFactory.decodeFile(path, options)
        }

        if (bitmaps.size != 4) {
            throw IOException("Không thể đọc một hoặc nhiều ảnh")
        }

        // Tính toán kích thước cho ảnh cuối cùng
        val spacing = 20 // Khoảng cách giữa các ảnh (dp)
        val spacingPx = (spacing * context.resources.displayMetrics.density).toInt()
        
        val singleImageWidth = bitmaps[0].width
        val singleImageHeight = bitmaps[0].height
        val totalHeight = (singleImageHeight * 4) + (spacingPx * 3)
        val totalWidth = singleImageWidth

        // Tạo bitmap mới
        val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)

        // Vẽ từng ảnh lên canvas
        var currentY = 0
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, 0f, currentY.toFloat(), null)
            currentY += bitmap.height + spacingPx
        }

        // Lưu ảnh vào storage
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
} 