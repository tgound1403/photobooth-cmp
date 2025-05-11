package com.example.cameraxapp.ui.components

import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalZeroShutterLag::class)
@Composable
fun ZoomControl(cameraControl: CameraControl, cameraInfo: CameraInfo) {
    val zoomState = cameraInfo.zoomState.observeAsState()
    val currentZoom = zoomState.value?.zoomRatio ?: 1f
    val maxZoom = zoomState.value?.maxZoomRatio ?: 1f
    val minZoom = zoomState.value?.minZoomRatio ?: 1f

    Slider(
        colors = SliderDefaults.colors(
            thumbColor = Color.Gray
        ), value = currentZoom, onValueChange = { zoomRatio ->
            cameraControl.setZoomRatio(zoomRatio)
        }, valueRange = minZoom..maxZoom, modifier = Modifier.padding(horizontal = 16.dp)
    )
}