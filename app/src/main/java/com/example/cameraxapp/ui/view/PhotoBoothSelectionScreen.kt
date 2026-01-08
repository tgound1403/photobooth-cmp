package com.example.cameraxapp.ui.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.shared.domain.model.ImageFilter
import com.example.cameraxapp.shared.domain.model.PhotoBoothLayout
import com.example.cameraxapp.ui.components.DefaultAppBar
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.DeepBlack
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.theme.NeonPurple
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import kotlinx.coroutines.delay

@Composable
fun PhotoBoothSelectionScreen(
    navController: NavController,
    viewModel: PhotoBoothViewModel,
) {
    val selectedImages by viewModel.selectedImages.collectAsState()
    val capturedImages by viewModel.capturedImages.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val selectedLayout by viewModel.selectedLayout.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    // Animation state
    val alphaAnim = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(1000))
    }

    // LaunchedEffect to navigate when save is successful
    LaunchedEffect(saveState) {
        if (saveState is PhotoBoothViewModel.SaveState.Success) {
            navController.navigate(AppRoutes.GALLERY) {
                popUpTo(AppRoutes.MAIN)
            }
        }
    }

    Scaffold(
        topBar = {
            DefaultAppBar(
                navController, "Chọn ảnh & Layout"
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBlack.copy(alpha=0.8f))
                    .padding(16.dp)
            ) {
                GlassButton(
                    onClick = {
                        val images = selectedImages.toList()
                        if (images.size == viewModel.requiredPhotoCount.value) {
                            val context = navController.context
                            viewModel.saveImages(context, images)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = "Xem Kết Quả",
                    enabled = selectedImages.size == viewModel.requiredPhotoCount.value
                )
            }
        },
        containerColor = DeepBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepBlack, Color(0xFF1a0b2e)) // Black to deep purple
                    )
                )
                .graphicsLayer { alpha = alphaAnim.value }
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(capturedImages) { imagePath ->
                    val isSelected = selectedImages.contains(imagePath)
                    Box(
                        modifier = Modifier
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.toggleImageSelection(imagePath) }
                    ) {
                        AsyncImage(
                            model = imagePath,
                            contentDescription = "Captured image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay when selected
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NeonCyan.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Chọn ${viewModel.requiredPhotoCount.collectAsState().value} ảnh để tạo kỷ niệm",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
        }
    }
}
