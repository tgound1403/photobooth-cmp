package com.example.photogether.core.render

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log

/**
 * The light leak and dust overlay textures of one GL context. Load with the context current, once;
 * [release] with the same context current. A texture id of 0 means that overlay is unavailable
 * (no assets or decode failure) and must be treated as disabled.
 */
internal class FxTextures private constructor(val leak: Int, val dust: Int) {

    fun release() {
        if (leak != 0) GLES30.glDeleteTextures(1, intArrayOf(leak), 0)
        if (dust != 0) GLES30.glDeleteTextures(1, intArrayOf(dust), 0)
    }

    companion object {
        const val LEAK_ASSET = "filters/fx/leak_warm.webp"
        const val DUST_ASSET = "filters/fx/dust.webp"
        private const val TAG = "FxTextures"

        /** Texture units used by the present pass for the overlays. */
        const val LEAK_UNIT = 3
        const val DUST_UNIT = 4

        val NONE = FxTextures(0, 0)

        fun load(assets: AssetManager?): FxTextures {
            if (assets == null) return NONE
            return FxTextures(loadTexture(assets, LEAK_ASSET), loadTexture(assets, DUST_ASSET))
        }

        private fun loadTexture(assets: AssetManager, path: String): Int {
            val bitmap: Bitmap? = try {
                val options = BitmapFactory.Options().apply {
                    inScaled = false
                    inPremultiplied = false // alpha is the strength, colour must stay straight
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot read $path; overlay disabled", e)
                null
            }
            if (bitmap == null) {
                Log.w(TAG, "Cannot decode $path; overlay disabled")
                return 0
            }
            try {
                val tex = GlUtil.newTexture()
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                GlUtil.checkGl("upload $path")
                return tex
            } catch (e: Exception) {
                Log.w(TAG, "Cannot upload $path; overlay disabled", e)
                return 0
            } finally {
                bitmap.recycle()
            }
        }
    }
}
