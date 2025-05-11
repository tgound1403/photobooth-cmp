package com.example.cameraxapp.ui.view.CameraScreen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

class ColorFilterProcessor(private val filterType: String) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val bitmap = imageToBitmap(image)
        val filteredBitmap = applyFilter(bitmap, filterType)
        // Update the preview with filteredBitmap
        image.close()
    }

    private fun imageToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer // Lấy data của kênh Y
        val uBuffer = image.planes[1].buffer // Lấy data của kênh U
        val vBuffer = image.planes[2].buffer // Lấy data của kênh V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y, U, V buffer vào mảng NV21
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val jpegArray = out.toByteArray()

        // Chuyển từ JPEG byte array sang Bitmap
        return BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)
    }

    private fun applyFilter(bitmap: Bitmap, filterType: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val filteredBitmap = Bitmap.createBitmap(width, height, bitmap.config!!)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val newPixel = when (filterType) {
                    "MONO" -> {
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        Color.rgb(gray, gray, gray)
                    }
                    "SEPIA" -> {
                        val red = (Color.red(pixel) * 0.393 + Color.green(pixel) * 0.769 + Color.blue(pixel) * 0.189).toInt()
                        val green = (Color.red(pixel) * 0.349 + Color.green(pixel) * 0.686 + Color.blue(pixel) * 0.168).toInt()
                        val blue = (Color.red(pixel) * 0.272 + Color.green(pixel) * 0.534 + Color.blue(pixel) * 0.131).toInt()
                        Color.rgb(red.coerceAtMost(255), green.coerceAtMost(255), blue.coerceAtMost(255))
                    }
                    else -> pixel
                }
                filteredBitmap.setPixel(x, y, newPixel)
            }
        }
        return filteredBitmap
    }
}
