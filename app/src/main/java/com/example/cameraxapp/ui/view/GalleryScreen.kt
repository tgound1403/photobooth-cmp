package com.example.cameraxapp.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.theme.DeepBlack
import com.example.cameraxapp.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(navController: NavController, viewModel: GalleryViewModel) {
    val photoBooths by viewModel.photoBooths.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PhotoBooth?>(null) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Bộ Sưu Tập", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                )
                )
            },
            containerColor = DeepBlack
    ) { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(DeepBlack, Color(0xFF1a0b2e))
                                                )
                                )
        ) {
            if (photoBooths.isEmpty()) {
                Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                "Chưa có ảnh nào",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                "Chụp ảnh PhotoBooth đầu tiên của bạn!",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(photoBooths) { photoBooth ->
                        PhotoBoothItem(
                                photoBooth = photoBooth,
                                onDelete = {
                                    itemToDelete = photoBooth
                                    showDeleteDialog = true
                                },
                                onClick = {
                                    val encodedPath =
                                            java.net.URLEncoder.encode(
                                                    photoBooth.imagePaths.first(),
                                                    "UTF-8"
                                            )
                                    navController.navigate(AppRoutes.imageDetail(encodedPath))
                                }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Xóa ảnh?") },
                text = { Text("Bạn có chắc muốn xóa ảnh này không?") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                itemToDelete?.let { viewModel.deletePhotoBooth(it) }
                                showDeleteDialog = false
                                itemToDelete = null
                            }
                    ) { Text("Xóa", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
                }
        )
    }
}

@Composable
fun PhotoBoothItem(photoBooth: PhotoBooth, onDelete: () -> Unit, onClick: () -> Unit) {
    Box {
        GlassBox(
                modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clickable(onClick = onClick),
                cornerRadius = 16.dp
        ) {
            AsyncImage(
                    model = photoBooth.imagePaths.first(),
                    contentDescription = "Photo Booth",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
            )
        }

        // Delete button
        IconButton(
                onClick = onDelete,
                modifier =
                        Modifier.align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(
                                        color = Color.Red.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                )
        ) {
            Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
            )
        }
    }
}
