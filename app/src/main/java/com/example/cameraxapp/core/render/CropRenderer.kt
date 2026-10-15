package com.example.cameraxapp.core.render

import android.graphics.Bitmap
import android.graphics.Matrix

/** Applies flips, rotation and the aspect crop from [CropParams] to a bitmap. */
object CropRenderer {

    fun apply(src: Bitmap, crop: CropParams): Bitmap {
        if (crop.isIdentity) return src
        val c = crop.normalized()
        val flipped =
            if (c.flipHorizontal || c.flipVertical || c.quarterTurns != 0) {
                val matrix = Matrix()
                if (c.flipHorizontal) matrix.postScale(-1f, 1f)
                if (c.flipVertical) matrix.postScale(1f, -1f)
                if (c.quarterTurns != 0) matrix.postRotate(90f * c.quarterTurns)
                Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            } else {
                src
            }
        val rect = CropMath.cropRect(flipped.width, flipped.height, c.aspect)
        if (rect.left == 0 && rect.top == 0 && rect.width == flipped.width && rect.height == flipped.height) {
            return flipped
        }
        return Bitmap.createBitmap(flipped, rect.left, rect.top, rect.width, rect.height)
    }
}
