package com.example.photogether.core.camera

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

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

/**
 * Focal lengths the lens panel offers, in 35mm-equivalent millimetres. When the device has at least two
 * distinct lenses these are the lenses themselves, sorted and rounded. Otherwise they are digital stops of
 * [mainMm] (1x, 1.5x, 2x), keeping only those that [maxZoom] can reach.
 */
fun lensOptions(lenses: List<Lens>, mainMm: Float, maxZoom: Float): List<Float> {
    val real = lenses.map { it.equivMm.roundToInt().toFloat() }.distinct().sorted()
    if (real.size >= 2) return real
    return listOf(1f, 1.5f, 2f)
        .map { mainMm * it }
        .filter { it <= mainMm * maxZoom + TOLERANCE_MM }
}

/** The option in [options] closest to [currentMm]. Returns [currentMm] when there are no options. */
fun closestOption(options: List<Float>, currentMm: Float): Float =
    options.minByOrNull { abs(it - currentMm) } ?: currentMm

/**
 * Magnification of [mm] against the main lens [mainMm], such as "1x", "1.5x" or "0.6x". At most one decimal;
 * a whole number drops ".0".
 */
fun ratioLabel(mm: Float, mainMm: Float): String {
    if (mainMm <= 0f) return "1x"
    val tenths = (mm / mainMm * 10f).roundToInt()
    val whole = tenths / 10
    val frac = abs(tenths % 10)
    return if (frac == 0) "${whole}x" else "$whole.${frac}x"
}

private const val FULL_FRAME_DIAGONAL_MM = 43.27f
private const val TOLERANCE_MM = 0.5f
