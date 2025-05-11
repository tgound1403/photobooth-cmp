package com.example.cameraxapp.ui.components

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ManualControls(
    onExposureChange: (Long) -> Unit,
    onISOChange: (Int) -> Unit,
    onClickCapture:  Unit
) {
    val context = LocalContext.current
    var isoValue by remember { mutableIntStateOf(100) }
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList[0]
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)

    val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
    val minExposure = exposureRange?.lower?.toFloat()
    val maxExposure = exposureRange?.upper?.toFloat()

    val sensitivityRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
    val minISO = sensitivityRange?.lower?.toFloat()
    val maxISO = sensitivityRange?.upper?.toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
//        if (minExposure != null && maxExposure != null) {
//            DialControl(
//                label = "Exposure",
//                initialValue = minExposure,
//                valueRange = minExposure..maxExposure,
//                step = 10_000f,
//                onValueChange = { value -> onExposureChange(value.toLong()) }
//            )
//        }
//
//        if (minISO != null && maxISO != null) {
//            DialControl(
//                label = "ISO",
//                initialValue = minISO,
//                valueRange = minISO..maxISO,
//                step = isoValue.toFloat() * 2,
//                onValueChange = { value ->
//                    onISOChange(value.toInt())
//                    isoValue = value.toInt()
//                }
//            )
//        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, shape = CircleShape)
                .clickable {
                    onClickCapture
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color.Black, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.Red, shape = CircleShape)
                )
            }

        }
    }
}