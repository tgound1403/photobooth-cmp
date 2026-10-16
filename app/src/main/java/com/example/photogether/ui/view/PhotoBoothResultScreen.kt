package com.example.photogether.ui.view

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.ui.components.GlassBox
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.components.SecondaryButton
import com.example.photogether.ui.components.ShimmerBox
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Motion
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel
import java.io.File
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
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

    PhotoBoothResultContent(
            imagePaths = photoBooth?.imagePaths,
            backgroundImage = selectedDeviceImage,
            isSaving = saveState is PhotoBoothViewModel.SaveState.Saving,
            isExportingGif = gifExportState is PhotoBoothViewModel.GifExportState.Exporting,
            showInstagramShare = saveState is PhotoBoothViewModel.SaveState.Success,
            snackbarHostState = snackbarHostState,
            onBack = { navController.navigateUp() },
            onShare = { path ->
                runCatching { shareImage(context, path) }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar("Không thể chia sẻ ảnh") }
                }
            },
            onShareInstagram = { path -> viewModel.shareToInstagramStories(context, path) },
            onSave = {
                photoBooth?.imagePaths?.let { imagePaths ->
                    if (selectedDeviceImage != null) {
                        viewModel.saveImageWithBg(context, imagePaths, selectedDeviceImage.toString())
                    } else {
                        viewModel.saveImages(context, imagePaths)
                    }
                }
            },
            onRetake = { navController.navigate(AppRoutes.PHOTO_BOOTH) },
            onPickBackground = { showDeviceImagePicker = true },
            onExportGif = {
                photoBooth?.imagePaths?.let { images -> viewModel.exportGif(context, images) }
            }
    )
    PhotoBoothPaywallOverlay(viewModel)
}

/**
 * Stateless body of the result screen. Takes plain values and lambdas so it can be previewed
 * without Koin or a NavController. A null [imagePaths] means the result is still loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothResultContent(
        imagePaths: List<String>?,
        backgroundImage: Uri?,
        isSaving: Boolean,
        isExportingGif: Boolean,
        showInstagramShare: Boolean,
        snackbarHostState: SnackbarHostState,
        onBack: () -> Unit,
        onShare: (String) -> Unit,
        onShareInstagram: (String) -> Unit,
        onSave: () -> Unit,
        onRetake: () -> Unit,
        onPickBackground: () -> Unit,
        onExportGif: () -> Unit
) {
    val context = LocalContext.current
    val firstPath = imagePaths?.firstOrNull()
    val hasImages = firstPath != null

    // Appear animation: the preview grows from 0.92 and fades in with the default spring.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, Motion.DefaultSpring) }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                        title = {
                            Text("Photo Booth Result", style = MaterialTheme.typography.titleLarge)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = AppColors.textPrimary
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = AppColors.background,
                                        titleContentColor = AppColors.textPrimary
                                )
                )
            },
            containerColor = AppColors.background
    ) { padding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Preview section
            Box(
                    modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(Spacing.lg)
                            .graphicsLayer {
                                val progress = appear.value
                                val scale = 0.92f + 0.08f * progress
                                scaleX = scale
                                scaleY = scale
                                alpha = progress.coerceIn(0f, 1f)
                            }
            ) {
                GlassBox(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = Radius.lg
                ) {
                    when {
                        imagePaths == null -> ShimmerBox(
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = Radius.lg
                        )
                        else -> {
                            backgroundImage?.let { uri ->
                                AsyncImage(
                                        model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                                        contentDescription = "Device background",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                )
                            }
                            if (imagePaths.size >= 4) {
                                Column(
                                        modifier = Modifier.fillMaxSize().padding(Spacing.md),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        ResultImageItem(imagePaths[0], Modifier.weight(1f))
                                        ResultImageItem(imagePaths[1], Modifier.weight(1f))
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        ResultImageItem(imagePaths[2], Modifier.weight(1f))
                                        ResultImageItem(imagePaths[3], Modifier.weight(1f))
                                    }
                                }
                            } else if (firstPath != null) {
                                // Saved result: a single composed image.
                                AsyncImage(
                                        model = firstPath,
                                        contentDescription = "Photo booth result",
                                        modifier = Modifier.fillMaxSize().padding(Spacing.md),
                                        contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            // Controls section
            Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Primary action
                GlassButton(
                        onClick = { firstPath?.let(onShare) },
                        modifier = Modifier.fillMaxWidth(),
                        text = "Chia sẻ",
                        enabled = hasImages
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    SecondaryButton(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            text = "Lưu Ảnh",
                            enabled = hasImages && !isSaving
                    )
                    SecondaryButton(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f),
                            text = "Chụp Lại"
                    )
                }

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    SecondaryButton(
                            onClick = onPickBackground,
                            modifier = Modifier.weight(1f),
                            text = "Nền"
                    )
                    SecondaryButton(
                            onClick = onExportGif,
                            modifier = Modifier.weight(1f),
                            text = if (isExportingGif) "Đang xuất GIF..." else "Lưu GIF",
                            enabled = hasImages && !isExportingGif
                    )
                }

                if (showInstagramShare && firstPath != null) {
                    SecondaryButton(
                            onClick = { onShareInstagram(firstPath) },
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
                            .clip(RoundedCornerShape(Radius.sm))
                            .border(Stroke.hairline, AppColors.outline, RoundedCornerShape(Radius.sm))
    ) {
        AsyncImage(
                model = imagePath,
                contentDescription = "Photo booth image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
        )
    }
}

/** Shares one image through the app FileProvider with ACTION_SEND. Throws if the file is not shareable. */
private fun shareImage(context: Context, imagePath: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(imagePath))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Chia sẻ ảnh"))
}

@Preview(showBackground = true)
@Composable
private fun PhotoBoothResultPreview() {
    PhotogetherTheme {
        PhotoBoothResultContent(
                imagePaths = null,
                backgroundImage = null,
                isSaving = false,
                isExportingGif = false,
                showInstagramShare = false,
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onShare = {},
                onShareInstagram = {},
                onSave = {},
                onRetake = {},
                onPickBackground = {},
                onExportGif = {}
        )
    }
}
