package com.example.photogether.core.render

import android.graphics.Bitmap
import android.os.Trace
import java.util.concurrent.atomic.AtomicInteger
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
 * The full edit chain: GPU grade and effects (filter, adjustments, chromatic, leak, distortion, scratch), crop, skin, frame, items.
 * Always starts from the source passed in; callers never feed a previous output back in.
 */
class EditPipeline(private val catalog: FilterCatalog) : EditRenderer {

    private val gl = GlRenderer(catalog.assets)

    override suspend fun render(
        source: Bitmap,
        snapshot: EditSnapshot,
        background: Bitmap?,
        density: Float,
        maxSize: Int?,
    ): Bitmap {
        // Async section: the render hops between the GL, IO and Default threads, so a sync
        // beginSection/endSection pair would land on different threads.
        val cookie = traceCookies.incrementAndGet()
        Trace.beginAsyncSection(TRACE_RENDER, cookie)
        try {
            return renderChain(source, snapshot, background, density, maxSize)
        } finally {
            Trace.endAsyncSection(TRACE_RENDER, cookie)
        }
    }

    private suspend fun renderChain(
        source: Bitmap,
        snapshot: EditSnapshot,
        background: Bitmap?,
        density: Float,
        maxSize: Int?,
    ): Bitmap {
        val params = snapshot.params.clamped()
        val lut = withContext(Dispatchers.IO) { catalog.loadLut(params.filterId) }
        val overlays = snapshot.overlays
        val effects = EffectSpec.of(params.chromatic, overlays.lightLeak, overlays.distortion, overlays.scratch)
        // Chromatic aberration, light leak, distortion and scratch are per-pixel GPU effects. They
        // ran after the crop on the CPU, and the leak corner and the horizontal shift depend on the
        // cropped frame, so with a crop they get their own GPU pass on the cropped bitmap.
        val grade = params.copy(chromatic = 0f)
        val cropped =
            if (effects.isNone || params.crop.isIdentity) {
                val graded = gl.render(source, grade, lut, maxSize, effects = effects)
                withContext(Dispatchers.Default) { CropRenderer.apply(graded, params.crop) }
            } else {
                val graded = gl.render(source, grade, lut, maxSize)
                val crop = withContext(Dispatchers.Default) { CropRenderer.apply(graded, params.crop) }
                gl.render(crop, EditParams(), null, null, effects = effects)
            }

        return withContext(Dispatchers.Default) {
            val skinned = SkinRenderer.apply(cropped, overlays.skin)
            val framed = FrameRenderer.apply(skinned, snapshot.frame, background, density)
            OverlayRenderer.applyItems(framed, overlays.items)
        }
    }

    override fun close() {
        gl.close()
    }

    private companion object {
        const val TRACE_RENDER = "EditPipeline.render"
        val traceCookies = AtomicInteger()
    }
}
