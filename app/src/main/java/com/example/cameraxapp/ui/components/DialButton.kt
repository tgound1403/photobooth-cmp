package com.example.cameraxapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import kotlin.math.atan2

@Composable
fun DialControl(
    modifier: Modifier = Modifier,
    label: String,
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    onValueChange: (Float) -> Unit
) {
    var currentValue by remember { mutableFloatStateOf(initialValue) }
    var previousAngle by remember { mutableStateOf<Float?>(null) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var dragStartedAngle by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .rotate(previousAngle ?: rotationAngle)
                .background(Color.Gray, shape = CircleShape)
        ) {
            Box(
                modifier = modifier
                    .size(80.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                previousAngle =
                                    calculateAngle(center = size.center, position = offset)
                            },
                            onDrag = { change, dragAmount ->
                                val currentAngle =
                                    calculateAngle(center = size.center, position = change.position)

                                previousAngle?.let { prevAngle ->
                                    val angleDifference = currentAngle - prevAngle

                                    val deltaValue = ((angleDifference / 360f) * step).toInt()

                                    val newValue = (currentValue + deltaValue).coerceIn(valueRange)

                                    if (newValue != currentValue) {
                                        currentValue = newValue
                                        onValueChange(currentValue)
                                    }
                                }

                                previousAngle = currentAngle
                            },
//                            onDragStart = { touchPoint ->
//                                dragStartedAngle = calculateAngle(touchPoint.x, touchPoint.y, 75f)
//                            },
//                            onDrag = { change, _ ->
//                                val touchAngle =
//                                    calculateAngle(change.position.x, change.position.y, 75f)
//                                val newAngle =
//                                    (touchAngle - dragStartedAngle + 360) % 360 // Normalize angle 0-360
//                                rotationAngle = newAngle
//
//                                currentValue = calculateValueFromAngle(newAngle, valueRange)
//                                onValueChange(currentValue)
//                            }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier
                    .height(50.dp)
                    .width(2.dp)
                    .background(color = Color.White))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$label\n${currentValue}",
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

fun calculateAngle(center: IntOffset, position: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y

    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

    return angle
}

// Helper function to calculate angle from touch coordinates
private fun calculateAngle(touchX: Float, touchY: Float, radius: Float): Float {
    val centerX = radius
    val centerY = radius
    val angleRad = atan2(touchY - centerY, touchX - centerX)
    return Math.toDegrees(angleRad.toDouble()).toFloat()
}

// Helper function to calculate value from angle and value range
private fun calculateValueFromAngle(angle: Float, valueRange: ClosedFloatingPointRange<Float>): Float {
    val angleRange = 360f
    val valueScale = (valueRange.endInclusive - valueRange.start) / angleRange
    return (angle * valueScale + valueRange.start).coerceIn(valueRange)
}