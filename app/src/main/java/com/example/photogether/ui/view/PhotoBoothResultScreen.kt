package com.example.photogether.ui.view

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
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
import com.example.photogether.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothResultScreen(
        photoBoothId: Long,
        navController: NavController,
        viewModel: PhotoBoothViewModel,
) {
    val photoBooth by viewModel.photoBooth.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val gifExportState by viewModel.gifExportState.collectAsStateWithLifecycle()
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val requiredCount by viewModel.requiredPhotoCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeviceImagePicker by remember { mutableStateOf(false) }
    var selectedDeviceImage by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(photoBoothId) { viewModel.getPhotoBoothById(photoBoothId) }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is PhotoBoothViewModel.SaveState.Success ->
                snackbarHostState.showSnackbar(context.getString(R.string.booth_saved_toast))
            is PhotoBoothViewModel.SaveState.Error ->
                snackbarHostState.showSnackbar(context.getString(R.string.booth_save_error, state.message))
            else -> {}
        }
    }

    LaunchedEffect(gifExportState) {
        when (val state = gifExportState) {
            is PhotoBoothViewModel.GifExportState.Success ->
                snackbarHostState.showSnackbar(context.getString(R.string.booth_gif_saved))
            is PhotoBoothViewModel.GifExportState.Error ->
                snackbarHostState.showSnackbar(context.getString(R.string.booth_gif_error, state.message))
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
            // A new background re-composes from the original shots of this session, not from the saved strip.
            canSaveWithBackground = selectedImages.size == requiredCount,
            snackbarHostState = snackbarHostState,
            onBack = { navController.navigateUp() },
            onShare = { path ->
                runCatching { shareImage(context, path) }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.booth_share_failed)) }
                }
            },
            onShareInstagram = { path -> viewModel.shareToInstagramStories(context, path) },
            onSaveWithBackground = {
                selectedDeviceImage?.let { background ->
                    viewModel.saveImageWithBg(context, selectedImages.toList(), background.toString())
                }
            },
            onRetake = {
                // Start over: drop the old shots so the capture screen shows its shutter again.
                viewModel.clearCapturedImages()
                navController.navigate(AppRoutes.PHOTO_BOOTH) {
                    popUpTo(AppRoutes.HOME)
                }
            },
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
 *
 * The saved result is already composed, so it is never composed again. Only a new background
 * (picked here) produces a new image, from the original shots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothResultContent(
        imagePaths: List<String>?,
        backgroundImage: Uri?,
        isSaving: Boolean,
        isExportingGif: Boolean,
        showInstagramShare: Boolean,
        canSaveWithBackground: Boolean,
        snackbarHostState: SnackbarHostState,
        onBack: () -> Unit,
        onShare: (String) -> Unit,
        onShareInstagram: (String) -> Unit,
        onSaveWithBackground: () -> Unit,
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
                            Text(stringResource(R.string.booth_result_title), style = MaterialTheme.typography.titleLarge)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        AppIcons.Back,
                                        contentDescription = stringResource(R.string.action_back),
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
                        // A chosen background is shown on its own until it is composed on save.
                        backgroundImage != null -> AsyncImage(
                                model = ImageRequest.Builder(context).data(backgroundImage).crossfade(true).build(),
                                contentDescription = stringResource(R.string.booth_background_preview),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                        firstPath != null -> AsyncImage(
                                model = firstPath,
                                contentDescription = stringResource(R.string.booth_result_image),
                                modifier = Modifier.fillMaxSize().padding(Spacing.md),
                                contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Controls section
            Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                GlassButton(
                        onClick = { firstPath?.let(onShare) },
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.booth_share),
                        enabled = hasImages
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    SecondaryButton(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.booth_retake)
                    )
                    SecondaryButton(
                            onClick = onPickBackground,
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.booth_background)
                    )
                }

                if (backgroundImage != null) {
                    Text(
                            text = stringResource(R.string.booth_background_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary
                    )
                    GlassButton(
                            onClick = onSaveWithBackground,
                            modifier = Modifier.fillMaxWidth(),
                            text = if (isSaving) stringResource(R.string.booth_saving)
                                   else stringResource(R.string.booth_save_background),
                            enabled = canSaveWithBackground && !isSaving
                    )
                }

                SecondaryButton(
                        onClick = onExportGif,
                        modifier = Modifier.fillMaxWidth(),
                        text = if (isExportingGif) stringResource(R.string.booth_gif_exporting)
                               else stringResource(R.string.booth_gif_save),
                        enabled = hasImages && !isExportingGif
                )

                if (showInstagramShare && firstPath != null) {
                    SecondaryButton(
                            onClick = { onShareInstagram(firstPath) },
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.booth_share_instagram)
                    )
                }
            }
        }
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.booth_share_chooser)))
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
                canSaveWithBackground = false,
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onShare = {},
                onShareInstagram = {},
                onSaveWithBackground = {},
                onRetake = {},
                onPickBackground = {},
                onExportGif = {}
        )
    }
}
