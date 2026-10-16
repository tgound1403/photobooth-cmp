package com.example.photogether.core.moments

/**
 * Size as displayed, after applying the EXIF orientation. MediaStore WIDTH/HEIGHT are the raw
 * pixel size, so a portrait photo stored with a 90 or 270 rotation has its sides swapped.
 */
fun displaySize(width: Int, height: Int, orientation: Int): Pair<Int, Int> =
    if (orientation == 90 || orientation == 270) height to width else width to height
