package com.example.cameraxapp.ui.view

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.DeepBlack
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothResultScreen(
        photoBoothId: Long,
        navController: NavController,
        viewModel: PhotoBoothViewModel,
) {
    val photoBooth by viewModel.photoBooth.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val gifExportState by viewModel.gifExportState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDeviceImagePicker by remember { mutableStateOf(false) }
    var selectedDeviceImage by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(photoBoothId) { viewModel.getPhotoBoothById(photoBoothId) }

    LaunchedEffect(saveState) {
        when (saveState) {
            is PhotoBoothViewModel.SaveState.Success -> {
                snackbarHostState.showSnackbar("Đã lưu ảnh thành công!")
            }
            is PhotoBoothViewModel.SaveState.Error -> {
                snackbarHostState.showSnackbar(
                        "Lỗi khi lưu ảnh: ${(saveState as PhotoBoothViewModel.SaveState.Error).message}"
                )
            }
            else -> {}
        }
    }

    LaunchedEffect(gifExportState) {
        when (gifExportState) {
            is PhotoBoothViewModel.GifExportState.Success -> {
                snackbarHostState.showSnackbar("Đã xuất GIF thành công!")
            }
            is PhotoBoothViewModel.GifExportState.Error -> {
                snackbarHostState.showSnackbar(
                        "Lỗi khi xuất GIF: ${(gifExportState as PhotoBoothViewModel.GifExportState.Error).message}"
                )
            }
            else -> {}
        }
    }

    if (showDeviceImagePicker) {
        DeviceImagePicker(
                onImageSelected = { uri ->
                    selectedDeviceImage = uri
                    showDeviceImagePicker = false
                },
                onDismiss = { showDeviceImagePicker = false }
        )
    }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                        title = { Text("Photo Booth Result", color = NeonCyan) },
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
                                        containerColor = DeepBlack,
                                        titleContentColor = NeonCyan
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
            // Preview Section
            GlassBox(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    cornerRadius = 24.dp
            ) {
                // Background Image
                selectedDeviceImage?.let { uri ->
                    AsyncImage(
                            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                            contentDescription = "Device background",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                    )
                }

                // Images Layout (2x2 Grid to maintain 3:4 Aspect Ratio)
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val images = photoBooth?.imagePaths ?: emptyList()
                    if (images.size >= 4) {
                        // Row 1
                        Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResultImageItem(images[0], Modifier.weight(1f))
                            ResultImageItem(images[1], Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row 2
                        Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResultImageItem(images[2], Modifier.weight(1f))
                            ResultImageItem(images[3], Modifier.weight(1f))
                        }
                    }
                }
            }

            // Controls Section
            Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassButton(
                            onClick = { navController.navigate(AppRoutes.PHOTO_BOOTH) },
                            modifier = Modifier.weight(1f),
                            text = "Chụp Lại"
                    )

                    GlassButton(
                            onClick = { showDeviceImagePicker = true },
                            modifier = Modifier.weight(1f),
                            text = "Nền"
                    )
                }

                GlassButton(
                        onClick = {
                            photoBooth?.imagePaths?.let { imagePaths ->
                                when {
                                    selectedDeviceImage != null -> {
                                        viewModel.saveImageWithBg(
                                                context,
                                                imagePaths,
                                                selectedDeviceImage.toString()
                                        )
                                    }
                                    else -> {
                                        viewModel.saveImages(context, imagePaths)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = "Lưu Ảnh",
                        enabled = saveState !is PhotoBoothViewModel.SaveState.Saving
                )

                GlassButton(
                        onClick = {
                            photoBooth?.imagePaths?.let { images ->
                                viewModel.exportGif(context, images)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text =
                                if (gifExportState is PhotoBoothViewModel.GifExportState.Exporting)
                                        "Đang xuất GIF..."
                                else "Lưu GIF",
                        enabled =
                                gifExportState !is PhotoBoothViewModel.GifExportState.Exporting &&
                                        (photoBooth?.imagePaths?.isNotEmpty() == true)
                )

                if (saveState is PhotoBoothViewModel.SaveState.Success) {
                    GlassButton(
                            onClick = {
                                photoBooth?.imagePaths?.firstOrNull()?.let {
                                    viewModel.shareToInstagramStories(context, it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            text = "Chia sẻ Instagram"
                    )
                }
            }
        }
    }
}

@Composable
fun ResultImageItem(imagePath: String, modifier: Modifier) {
    Box(
            modifier =
                    modifier.aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
                model = imagePath,
                contentDescription = "Photo booth image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
        )
    }
}
