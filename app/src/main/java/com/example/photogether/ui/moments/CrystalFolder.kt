package com.example.photogether.ui.moments

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.moments.MomentQuery
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke

/**
 * Home tile that opens Moments. Paper card: "GALLERY" and the photo count on the left,
 * a 2 x 2 grid of the latest moments with 2dp ink borders on the right.
 * The card lifts first; onClick runs when the lift animation ends.
 */
@Composable
fun CrystalFolder(
    moments: List<Moment>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var launching by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(
        targetValue = if (launching) -160f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "crystalLift",
        finishedListener = { if (launching) onClick() },
    )

    val shape = RoundedCornerShape(Radius.lg)
    val preview = MomentQuery.stackPreview(moments)

    Box(
        modifier = modifier
            .graphicsLayer { translationY = lift * density }
            .clip(shape)
            .background(AppColors.surface)
            .border(Stroke.border, AppColors.outline, shape)
            .clickable { launching = true }
            .padding(Spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "GALLERY",
                    style = BinsooType.label,
                    color = AppColors.textPrimary,
                )
                Text(
                    text = "${moments.size} PHOTOS",
                    style = BinsooType.micro,
                    color = AppColors.textSecondary,
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        for (col in 0 until 2) {
                            FolderTile(
                                moment = preview.getOrNull(row * 2 + col),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One photo slot in the folder grid. An empty slot is a plain bordered square. */
@Composable
private fun FolderTile(moment: Moment?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm))
            .padding(Stroke.border)
            .background(AppColors.surfaceElevated),
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
}
