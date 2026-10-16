package com.example.photogether.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.data.model.FrameTheme
import com.example.photogether.data.model.FrameThemes
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
    val label: String,
    val description: String
)

private val LAYOUT_OPTIONS = listOf(
    LayoutOption(PhotoBoothLayout.SINGLE, "1 Ảnh (Single)", "Chụp 1 ảnh duy nhất"),
    LayoutOption(PhotoBoothLayout.STRIP_1X2, "2 Ảnh (Film Strip)", "Chụp 2 ảnh - Dọc"),
    LayoutOption(PhotoBoothLayout.STRIP_1X3, "3 Ảnh (Film Strip)", "Chụp 3 ảnh - Dọc"),
    LayoutOption(PhotoBoothLayout.GRID_2X2, "4 Ảnh (Grid 2x2)", "Chụp 4 ảnh - Khung vuông cổ điển"),
    LayoutOption(PhotoBoothLayout.STRIP_1X4, "4 Ảnh (Film Strip)", "Chụp 4 ảnh - Dọc")
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
        topBar = { DefaultAppBar(onBack = onBack, title = "Chọn Khung Ảnh") },
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
                "Chọn kiểu khung bạn muốn chụp",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                modifier = Modifier.padding(bottom = Spacing.xl)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                modifier = Modifier.weight(1f)
            ) {
                items(LAYOUT_OPTIONS) { option ->
                    FrameOption(
                        label = option.label,
                        description = option.description,
                        onClick = { onLayoutSelected(option.layout) }
                    )
                }
            }
        }
    }
}

@Composable
fun FrameOption(
    label: String,
    description: String,
    onClick: () -> Unit,
    color: Color = AppColors.accent,
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = Radius.sm
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.xl)) {
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
