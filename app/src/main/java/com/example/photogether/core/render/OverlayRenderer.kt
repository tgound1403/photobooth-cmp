package com.example.photogether.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.withTranslation
import kotlin.math.min

/** Labels offered by the overlay panel. The ids are what [OverlaySpec] stores. */
object OverlayCatalog {
    val lightLeaks = listOf("Light leak 1", "Light leak 2", "Light leak 3", "Light leak 4", "Light leak 5")
    val scratches = listOf("Scratch 1", "Scratch 2", "Scratch 3", "Scratch 4", "Scratch 5")
    val distortions = listOf("Distortion 1", "Distortion 2", "Distortion 3", "Distortion 4", "Distortion 5")
    val stickers = listOf("❤️", "⭐", "✨", "🎉", "🎈", "🌈", "☀️", "🌙", "💫", "🔥")
}

/**
 * CPU sticker and text items. The per-pixel effects (chromatic aberration, light leak, distortion
 * presets, scratch) now run on the GPU, see [EffectMath] and the present shader.
 */
object OverlayRenderer {

    /** Size of a sticker or text item at scale 1, as a fraction of the output's shorter side. */
    private const val ITEM_UNIT_SIDE = 1000f

    private fun Bitmap.mutableCopy(): Bitmap = copy(config ?: Bitmap.Config.ARGB_8888, true)

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
