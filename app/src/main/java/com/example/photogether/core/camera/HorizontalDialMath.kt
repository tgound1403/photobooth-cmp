package com.example.photogether.core.camera

import kotlin.math.roundToInt

/**
 * Detent a horizontal dial lands on after [dragPx] pixels of drag from [startIndex]: one detent per
 * [pxPerDetent], rounded to the nearest and clamped to `0..count-1`. Dragging right (positive) raises it.
 */
fun detentForDrag(startIndex: Int, dragPx: Float, pxPerDetent: Float, count: Int): Int {
    val last = (count - 1).coerceAtLeast(0)
    if (pxPerDetent <= 0f) return startIndex.coerceIn(0, last)
    return (startIndex + (dragPx / pxPerDetent).roundToInt()).coerceIn(0, last)
}
