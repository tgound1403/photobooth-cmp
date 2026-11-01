package com.example.photogether.ui.moments

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.moments.MomentCalendar
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke

private val PRINT_PAPER = Color(0xFFFDFCF7)
private const val PEEK_COUNT = 5

/**
 * A drawer left slightly open: the newest photos poke out over the top edge of the front panel,
 * the rest stay inside. The panel carries the [label] and the photo count.
 * When tapped the panel pulls toward you and the photos lift, then [onClick] runs.
 */
@Composable
fun AjarDrawer(
    label: String,
    moments: List<Moment>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
) {
    var launching by remember { mutableStateOf(false) }
    val pull by animateFloatAsState(
        targetValue = if (launching) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "drawerPull",
        finishedListener = { if (launching) { launching = false; onClick() } },
    )
    val panelShape = RoundedCornerShape(Radius.lg)
    val backShape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(enabled = !launching) { launching = true }
            .semantics { contentDescription = label },
    ) {
        // Back of the drawer: the photos stand against it and poke out over its top edge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .shadow(6.dp, backShape)
                .clip(backShape)
                .background(Brush.verticalGradient(listOf(AppColors.surfaceElevated.compositeOver(Color(0x26000000)), AppColors.surfaceElevated)))
                .border(Stroke.border, AppColors.outline, backShape),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = -pull * 18.dp.toPx() },
        ) {
            val side = Spacing.md
            val inner = maxWidth - side * 2
            // Narrow drawers get smaller prints, so even a 3:2 print stays inside the card.
            val printH = minOf(height * 0.58f, inner / 1.6f)
            val shown = moments.take(PEEK_COUNT)
            // Top edge of the front panel, where the prints disappear behind it.
            val frontTop = height * 0.58f
            // Oldest first, so the newest print (index 0) is drawn last and lies on top.
            for ((index, moment) in shown.withIndex().reversed()) {
                val ratio = MomentCalendar.ratioOf(moment, min = 0.6f, max = 1.5f)
                // A print never takes more than half the width, or there would be no room to spread them.
                val h = minOf(printH, inner * 0.5f / ratio)
                val printW = h * ratio
                val pose = MomentCalendar.poseFor(moment.uri, maxTilt = 1f)
                // The prints are spread over the whole width of the drawer: the first at the left edge, the last at the right.
                val t = if (shown.size > 1) index / (shown.size - 1f) else 0.5f
                // Like cards fanned in a box: the leftmost leans left, the rightmost leans right, the middle ones stand almost upright.
                val lean = (t * 2f - 1f) * 9f + pose.rotation * 2.5f
                val x = side + (inner - printW).coerceAtLeast(0.dp) * t
                // Every print stands on the floor of the drawer, so its lower part is behind the front panel and only a
                // slice of its top shows, however tall or wide it is.
                val hidden = h * (0.32f + 0.08f * (index % 3))
                val y = frontTop + hidden - h + 4.dp * pose.dy
                Box(
                    modifier = Modifier
                        .offset(x = x + 3.dp * pose.dx, y = y)
                        .size(width = printW, height = h)
                        .graphicsLayer {
                            rotationZ = lean
                            shadowElevation = (8 - index * 1.2f).dp.toPx()
                            spotShadowColor = Color.Black
                        }
                        .background(PRINT_PAPER)
                        .padding(3.dp),
                ) {
                    AsyncImage(
                        model = moment.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Prints deeper in the drawer sit in more shade.
                    if (index > 0) {
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.1f * (index % 3 + 1))))
                    }
                }
            }
        }

        // Front panel hides the lower part of the photos.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .graphicsLayer { translationY = pull * 8.dp.toPx() }
                .shadow(12.dp, panelShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(panelShape)
                .background(Brush.verticalGradient(listOf(AppColors.surface.compositeOver(Color(0x18FFFFFF)), AppColors.surface.compositeOver(Color(0x20000000)))))
                .border(Stroke.border, AppColors.outline, panelShape)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Handle: a recessed grip with a light lower lip.
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(8.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF3B3B3B))), RoundedCornerShape(4.dp)),
            )
            Box(modifier = Modifier.height(Spacing.sm))
            Text(
                text = label,
                style = BinsooType.label,
                color = AppColors.textPrimary,
            )
            Text(
                text = "${moments.size}",
                style = BinsooType.micro,
                color = AppColors.textSecondary,
            )
        }
    }
}
