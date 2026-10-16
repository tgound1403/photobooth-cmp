package com.example.cameraxapp.core.camera

import kotlin.math.hypot

/** Focal lengths (mm) the zoom pill steps through, in 35mm-equivalent terms. */
val FOCAL_STOPS_MM: List<Int> = listOf(35, 50, 75, 100)

/**
 * One back camera of the device. [cameraId] is the Camera2 id (a physical lens on multi-camera
 * phones) and [equivMm] is its focal length in 35mm-equivalent millimetres.
 */
data class Lens(val cameraId: String, val equivMm: Float)

/**
 * 35mm-equivalent focal length of a lens with real focal length [focalMm] on a sensor of
 * [sensorWidthMm] by [sensorHeightMm]. A full-frame 35mm sensor has a diagonal of about 43.27mm.
 */
fun equivalentFocalMm(focalMm: Float, sensorWidthMm: Float, sensorHeightMm: Float): Float {
    val diagonal = hypot(sensorWidthMm, sensorHeightMm)
    return if (diagonal > 0f) focalMm * FULL_FRAME_DIAGONAL_MM / diagonal else focalMm
}

/**
 * The lens to use for [targetMm]: the longest lens that does not exceed it, so the rest is covered
 * by digital zoom. When every lens is longer than the target, the shortest lens is used.
 */
fun pickLens(targetMm: Float, lenses: List<Lens>): Lens? =
    lenses.filter { it.equivMm <= targetMm + TOLERANCE_MM }.maxByOrNull { it.equivMm }
        ?: lenses.minByOrNull { it.equivMm }

/** Zoom ratio on [lens] that gives [targetMm]. */
fun zoomFor(targetMm: Float, lens: Lens): Float = targetMm / lens.equivMm

private const val FULL_FRAME_DIAGONAL_MM = 43.27f
private const val TOLERANCE_MM = 0.5f
