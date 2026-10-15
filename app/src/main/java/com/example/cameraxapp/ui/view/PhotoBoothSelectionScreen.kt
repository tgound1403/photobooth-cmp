package com.example.cameraxapp.ui.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.ui.components.DefaultAppBar
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Motion
import com.example.cameraxapp.ui.theme.PhotogetherTheme
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel

@Composable
fun PhotoBoothSelectionScreen(
    navController: NavController,
    viewModel: PhotoBoothViewModel,
) {
    val selectedImages by viewModel.selectedImages.collectAsState()
    val capturedImages by viewModel.capturedImages.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val requiredCount by viewModel.requiredPhotoCount.collectAsState()

    // Navigate to the gallery once the composed result is saved.
    LaunchedEffect(saveState) {
        if (saveState is PhotoBoothViewModel.SaveState.Success) {
            navController.navigate(AppRoutes.GALLERY) {
                popUpTo(AppRoutes.MAIN)
            }
        }
    }

    PhotoBoothSelectionContent(
        capturedImages = capturedImages,
        selectedImages = selectedImages,
        requiredCount = requiredCount,
        onBack = { navController.navigateUp() },
        onToggle = { viewModel.toggleImageSelection(it) },
        onConfirm = {
            val images = selectedImages.toList()
            if (images.size == requiredCount) {
                viewModel.saveImages(navController.context, images)
            }
        }
    )
}

/** Stateless selection body. */
@Composable
fun PhotoBoothSelectionContent(
    capturedImages: List<String>,
    selectedImages: Collection<String>,
    requiredCount: Int,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    // Fade the grid in on first composition.
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(Motion.DurationLong))
    }

    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = "Chọn ảnh & Layout") },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(Spacing.lg)
            ) {
                GlassButton(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    text = "Xem Kết Quả",
                    enabled = selectedImages.size == requiredCount
                )
            }
        },
        containerColor = AppColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { alpha = alphaAnim.value }
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.weight(1f)
            ) {
                items(capturedImages) { imagePath ->
                    val isSelected = selectedImages.contains(imagePath)
                    Box(
                        modifier = Modifier
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(Radius.sm))
                            .border(
                                width = if (isSelected) Stroke.selected else Stroke.hairline,
                                color = if (isSelected) AppColors.accent else AppColors.outline,
                                shape = RoundedCornerShape(Radius.sm)
                            )
                            .clickable { onToggle(imagePath) }
                    ) {
                        AsyncImage(
                            model = imagePath,
                            contentDescription = "Captured image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppColors.accent.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            Text(
                text = "Chọn $requiredCount ảnh để tạo kỷ niệm",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = Spacing.sm)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoBoothSelectionPreview() {
    PhotogetherTheme {
        PhotoBoothSelectionContent(
            capturedImages = emptyList(),
            selectedImages = emptySet(),
            requiredCount = 2,
            onBack = {},
            onToggle = {},
            onConfirm = {}
        )
    }
}
