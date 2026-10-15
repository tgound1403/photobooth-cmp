package com.example.cameraxapp.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import com.example.cameraxapp.ui.components.DefaultAppBar
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.PhotogetherTheme
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.viewmodel.GalleryViewModel

@Composable
fun GalleryScreen(navController: NavController, viewModel: GalleryViewModel) {
    val photoBooths by viewModel.photoBooths.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PhotoBooth?>(null) }

    GalleryContent(
        photoBooths = photoBooths,
        onBack = { navController.navigateUp() },
        onOpen = { photoBooth ->
            val encodedPath = java.net.URLEncoder.encode(photoBooth.imagePaths.first(), "UTF-8")
            navController.navigate(AppRoutes.imageDetail(encodedPath))
        },
        onDeleteRequest = { photoBooth ->
            itemToDelete = photoBooth
            showDeleteDialog = true
        }
    )

    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa ảnh?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Bạn có chắc muốn xóa ảnh này không?", style = MaterialTheme.typography.bodyMedium) },
            containerColor = AppColors.surfaceElevated,
            titleContentColor = AppColors.textPrimary,
            textContentColor = AppColors.textSecondary,
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { viewModel.deletePhotoBooth(it) }
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) { Text("Xóa", color = AppColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy", color = AppColors.textPrimary)
                }
            }
        )
    }
}

/** Stateless gallery body. Takes plain data and callbacks so it can be previewed without Koin. */
@Composable
fun GalleryContent(
    photoBooths: List<PhotoBooth>,
    onBack: () -> Unit,
    onOpen: (PhotoBooth) -> Unit,
    onDeleteRequest: (PhotoBooth) -> Unit
) {
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = "Bộ Sưu Tập") },
        containerColor = AppColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (photoBooths.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(Spacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Chưa có ảnh nào",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            "Chụp ảnh PhotoBooth đầu tiên của bạn!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    items(photoBooths) { photoBooth ->
                        PhotoBoothItem(
                            photoBooth = photoBooth,
                            onDelete = { onDeleteRequest(photoBooth) },
                            onClick = { onOpen(photoBooth) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoBoothItem(photoBooth: PhotoBooth, onDelete: () -> Unit, onClick: () -> Unit) {
    Box {
        GlassBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clickable(onClick = onClick),
            cornerRadius = Radius.sm
        ) {
            AsyncImage(
                model = photoBooth.imagePaths.first(),
                contentDescription = "Photo Booth",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.sm)
                .size(IconSize.xl)
                .background(color = AppColors.danger.copy(alpha = 0.85f), shape = RoundedCornerShape(Radius.sm))
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = AppColors.textPrimary,
                modifier = Modifier.size(IconSize.sm)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryEmptyPreview() {
    PhotogetherTheme {
        GalleryContent(photoBooths = emptyList(), onBack = {}, onOpen = {}, onDeleteRequest = {})
    }
}
