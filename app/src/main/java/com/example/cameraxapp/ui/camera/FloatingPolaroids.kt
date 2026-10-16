package com.example.cameraxapp.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cameraxapp.core.moments.Moment
import com.example.cameraxapp.ui.theme.BinsooType
import com.example.cameraxapp.ui.theme.Radius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CARD_WIDTH_DP = 96f
private const val CARD_HEIGHT_DP = 116f

/** Fixed slot layout around the centre: x and y offsets in dp, tilt in degrees. */
private data class PolaroidSlot(val x: Float, val y: Float, val tilt: Float)

private val SLOTS =
    listOf(
        PolaroidSlot(x = -86f, y = -70f, tilt = -8f),
        PolaroidSlot(x = 78f, y = -92f, tilt = 6f),
        PolaroidSlot(x = -70f, y = 64f, tilt = 5f),
        PolaroidSlot(x = 84f, y = 60f, tilt = -6f),
    )

/**
 * Floating white polaroid cards used as the backdrop of the camera picker.
 * Each card shows the moment at its index (if any); a grey card is shown while there are no photos.
 */
@Composable
fun FloatingPolaroids(moments: List<Moment>, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SLOTS.forEachIndexed { index, slot ->
            FloatingPolaroidCard(
                moment = moments.getOrNull(index),
                slot = slot,
                index = index,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun FloatingPolaroidCard(
    moment: Moment?,
    slot: PolaroidSlot,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "polaroid_float_$index")
    val drift by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200 + index * 300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "polaroid_drift_$index",
    )
    val takenAt = moment?.takenAt
    val caption =
        remember(takenAt) {
            if (takenAt == null) "" else SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(takenAt))
        }

    Column(
        modifier =
            modifier
                .offset(x = slot.x.dp, y = (slot.y + drift).dp)
                .graphicsLayer { rotationZ = slot.tilt }
                .size(width = CARD_WIDTH_DP.dp, height = CARD_HEIGHT_DP.dp)
                .background(Color.White, RoundedCornerShape(Radius.sm))
                .padding(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFBDBDBD)),
        ) {
            if (moment != null) {
                AsyncImage(
                    model = moment.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = caption,
            style = BinsooType.micro,
            color = Color(0xFF555555),
            maxLines = 1,
        )
    }
}
