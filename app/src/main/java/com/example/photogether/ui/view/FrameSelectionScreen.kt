package com.example.photogether.ui.view

import androidx.annotation.StringRes
import com.tgound.photogether.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.shared.domain.model.PhotoBoothLayout
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.components.GlassBox
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel

private data class LayoutOption(
    val layout: PhotoBoothLayout,
    /** Number of photos the layout keeps from the shots taken. */
    val count: Int,
    @StringRes val labelRes: Int
)

private val LAYOUT_OPTIONS = listOf(
    LayoutOption(PhotoBoothLayout.SINGLE, 1, R.string.frame_option_single),
    LayoutOption(PhotoBoothLayout.STRIP_1X2, 2, R.string.frame_option_strip2),
    LayoutOption(PhotoBoothLayout.STRIP_1X3, 3, R.string.frame_option_strip3),
    LayoutOption(PhotoBoothLayout.GRID_2X2, 4, R.string.frame_option_grid4),
    LayoutOption(PhotoBoothLayout.STRIP_1X4, 4, R.string.frame_option_strip4)
)

@Composable
fun FrameSelectionScreen(navController: NavController, viewModel: PhotoBoothViewModel) {
    FrameSelectionContent(
        onBack = { navController.navigateUp() },
        onLayoutSelected = { layout ->
            viewModel.updateLayout(layout)
            viewModel.clearCapturedImages()
            navController.navigate(AppRoutes.PHOTO_BOOTH)
        }
    )
}

/** Stateless body: layout list. Frame theme is chosen on the edit screen. */
@Composable
fun FrameSelectionContent(
    onBack: () -> Unit,
    onLayoutSelected: (PhotoBoothLayout) -> Unit,
) {
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = stringResource(R.string.frame_selection_title)) },
        containerColor = AppColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.frame_selection_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                modifier = Modifier.padding(bottom = Spacing.xl)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                modifier = Modifier.weight(1f)
            ) {
                items(LAYOUT_OPTIONS, key = { it.layout }) { option ->
                    FrameOption(
                        layout = option.layout,
                        label = stringResource(option.labelRes),
                        description = stringResource(
                            R.string.frame_desc_count,
                            option.count,
                            PhotoBoothViewModel.CAPTURE_COUNT
                        ),
                        onClick = { onLayoutSelected(option.layout) }
                    )
                }
            }
        }
    }
}

@Composable
fun FrameOption(
    layout: PhotoBoothLayout,
    label: String,
    description: String,
    onClick: () -> Unit,
    color: Color = AppColors.accent,
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = Radius.sm
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LayoutDiagram(layout = layout, color = color)
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleLarge, color = color)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}

/** Outline of the photo arrangement, so each option shows its shape before it is picked. */
@Composable
private fun LayoutDiagram(layout: PhotoBoothLayout, color: Color) {
    Canvas(modifier = Modifier.size(56.dp)) {
        val pad = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val stroke = Stroke(width = 1.5.dp.toPx())
        val corner = CornerRadius(2.dp.toPx())

        fun cell(x: Float, y: Float, w: Float, h: Float) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = corner,
                style = stroke
            )
        }

        when (layout) {
            PhotoBoothLayout.SINGLE -> cell(pad, pad, size.width - 2 * pad, size.height - 2 * pad)
            PhotoBoothLayout.GRID_2X2 -> {
                val cw = (size.width - 2 * pad - gap) / 2
                val ch = (size.height - 2 * pad - gap) / 2
                for (row in 0..1) {
                    for (col in 0..1) {
                        cell(pad + col * (cw + gap), pad + row * (ch + gap), cw, ch)
                    }
                }
            }
            else -> {
                // Film strips: a narrow column of stacked frames.
                val count = when (layout) {
                    PhotoBoothLayout.STRIP_1X2 -> 2
                    PhotoBoothLayout.STRIP_1X3 -> 3
                    else -> 4
                }
                val stripW = size.width * 0.55f
                val x = (size.width - stripW) / 2
                val ch = (size.height - 2 * pad - gap * (count - 1)) / count
                for (i in 0 until count) {
                    cell(x, pad + i * (ch + gap), stripW, ch)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FrameSelectionPreview() {
    PhotogetherTheme {
        FrameSelectionContent(
            onBack = {},
            onLayoutSelected = {}
        )
    }
}
