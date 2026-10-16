package com.example.photogether.core.render

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Renders an [EditSnapshot] onto a source bitmap. Implemented by [EditPipeline]; faked in tests. */
interface EditRenderer : AutoCloseable {
    /**
     * [maxSize] limits the longest output side (preview); null renders at source resolution.
     * [background] is the decoded background image when [FrameSpec.backgroundUri] is set.
     */
    suspend fun render(
        source: Bitmap,
        snapshot: EditSnapshot,
        background: Bitmap?,
        density: Float,
        maxSize: Int?,
    ): Bitmap
}

/**
 * The full edit chain: GPU grade (filter and adjustments), crop, CPU effects, frame, items.
 * Always starts from the source passed in; callers never feed a previous output back in.
 */
class EditPipeline(private val catalog: FilterCatalog) : EditRenderer {

    private val gl = GlRenderer()

    override suspend fun render(
        source: Bitmap,
        snapshot: EditSnapshot,
        background: Bitmap?,
        density: Float,
        maxSize: Int?,
    ): Bitmap {
        val params = snapshot.params.clamped()
        val lut = withContext(Dispatchers.IO) { catalog.loadLut(params.filterId) }
        val graded = gl.render(source, params, lut, maxSize)

        return withContext(Dispatchers.Default) {
            var out = CropRenderer.apply(graded, params.crop)
            if (params.chromatic > 0f) {
                out = OverlayRenderer.applyChromaticAberration(out, params.chromatic)
            }
            val overlays = snapshot.overlays
            overlays.lightLeak?.let { out = OverlayRenderer.applyLightLeak(out, it) }
            overlays.distortion?.let { out = OverlayRenderer.applyDistortion(out, it) }
            overlays.scratch?.let { out = OverlayRenderer.applyScratch(out, it) }
            out = FrameRenderer.apply(out, snapshot.frame, background, density)
            OverlayRenderer.applyItems(out, overlays.items)
        }
    }

    override fun close() {
        gl.close()
    }
}
