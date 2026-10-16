package com.example.photogether.core.render

/** Pure maths shared by the grain shader inputs; the shader mirrors [luminanceWeight]. */
object GrainMath {
    /** Live frame seeds cycle through 0 until this, which keeps them exact in highp floats. */
    const val SEED_RANGE = 1024

    /** Fixed seed for offline renders so captures are repeatable. */
    const val CAPTURE_SEED = 0f

    /** Grain strength weight: 0 at black and white, 1 at mid grey. */
    fun luminanceWeight(l: Float): Float = (4f * l * (1f - l)).coerceIn(0f, 1f)

    /** Grain coordinate divisor so grain size matches across output heights (1080 p = 1). */
    fun sizeScale(outputHeight: Int): Float = (outputHeight / 1080f).coerceIn(0.5f, 4f)
}
