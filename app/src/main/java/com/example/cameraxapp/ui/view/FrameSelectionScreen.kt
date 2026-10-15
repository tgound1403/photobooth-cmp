package com.example.cameraxapp.ui.view

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
import androidx.compose.foundation.shape.CircleShape
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
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.data.model.FrameTheme
import com.example.cameraxapp.data.model.FrameThemes
import com.example.cameraxapp.shared.domain.model.PhotoBoothLayout
import com.example.cameraxapp.ui.components.DefaultAppBar
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.PhotogetherTheme
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel

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
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    FrameSelectionContent(
        selectedTheme = selectedTheme,
        onBack = { navController.navigateUp() },
        onLayoutSelected = { layout ->
            viewModel.updateLayout(layout)
            viewModel.clearCapturedImages()
            navController.navigate(AppRoutes.PHOTO_BOOTH)
        },
        onThemeSelected = { viewModel.updateTheme(it) }
    )
}

/** Stateless body: layout list plus frame theme picker. */
@Composable
fun FrameSelectionContent(
    selectedTheme: FrameTheme,
    onBack: () -> Unit,
    onLayoutSelected: (PhotoBoothLayout) -> Unit,
    onThemeSelected: (FrameTheme) -> Unit,
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

                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        "Chọn giao diện khung",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.textPrimary,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }

                items(FrameThemes.ALL_THEMES.chunked(2)) { themeRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        themeRow.forEach { theme ->
                            ThemeOption(
                                theme = theme,
                                isSelected = selectedTheme.id == theme.id,
                                onClick = { onThemeSelected(theme) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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

@Composable
fun ThemeOption(
    theme: FrameTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Light frame backgrounds need dark text. Frame colours come from the data model and are not theme tokens.
    val isLightBackground = theme.backgroundColor == android.graphics.Color.WHITE ||
        theme.backgroundColor == android.graphics.Color.parseColor("#F5E6D3")

    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .background(color = Color(theme.backgroundColor), shape = RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        contentAlignment = Alignment.BottomStart
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(IconSize.md)
                    .background(AppColors.accent, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", style = MaterialTheme.typography.labelSmall, color = AppColors.onAccent)
            }
        }
        Text(
            text = theme.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isLightBackground) AppColors.onAccent else AppColors.textPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FrameSelectionPreview() {
    PhotogetherTheme {
        FrameSelectionContent(
            selectedTheme = FrameThemes.ALL_THEMES.first(),
            onBack = {},
            onLayoutSelected = {},
            onThemeSelected = {}
        )
    }
}
