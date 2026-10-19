package com.example.photogether.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Drawable id of the still image of camera [cameraId] (`res/drawable-nodpi/camera_<id>.png`), or 0 when
 * that camera has no still yet. Callers fall back to the drawn icon on 0.
 */
@Composable
fun cameraImageRes(cameraId: String): Int {
    val context = LocalContext.current
    return remember(cameraId) {
        context.resources.getIdentifier("camera_$cameraId", "drawable", context.packageName)
    }
}
