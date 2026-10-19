package com.example.photogether.ui.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.components.GlassBox
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.components.SecondaryButton
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.viewmodel.CameraViewModel
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.tgound.photogether.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoBoothScreen(
    navController: NavController,
    photoBoothViewModel: PhotoBoothViewModel,
    cameraViewModel: CameraViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val capturedImages by photoBoothViewModel.capturedImages.collectAsStateWithLifecycle()
    val requiredCount by photoBoothViewModel.requiredPhotoCount.collectAsStateWithLifecycle()
    val captureCount = capturedImages.size

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    // Set once the system prompt has been shown, so a permanent denial can send the user to settings.
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    // Same 3 s countdown as the viewfinder default, for every shot.
    val TIME_TO_CAPTURE = 3
    val POST_SHOT_DELAY = 1000L
    // Shoot more than the layout keeps, so the user can choose which shots to use.
    val PHOTO_LIMIT = PhotoBoothViewModel.CAPTURE_COUNT

    val isCapturing by cameraViewModel.isCapturing.collectAsStateWithLifecycle()
    val countdown by cameraViewModel.countdown.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val flash = remember { Animatable(0f) }
    val mediaActionSound = remember { android.media.MediaActionSound() }
    val toneGenerator = remember { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100) }

    DisposableEffect(Unit) {
        mediaActionSound.load(android.media.MediaActionSound.SHUTTER_CLICK)
        onDispose {
            // Leaving mid-countdown must not keep the timer running.
            cameraViewModel.stopCapturing()
            mediaActionSound.release()
            toneGenerator.release()
        }
    }

    fun goToSelection() {
        navController.navigate(AppRoutes.PHOTO_BOOTH_SELECTION) {
            // Keep Home under Selection so back returns to Home instead of exiting the app.
            popUpTo(AppRoutes.HOME) {
                inclusive = false
            }
        }
    }

    LaunchedEffect(isCapturing) {
        // Read the live count: a value captured at composition time would go stale inside this loop.
        while (isCapturing && photoBoothViewModel.capturedImages.value.size < PHOTO_LIMIT) {
            while (countdown > 0) {
                toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150) // Beep
                delay(1000)
                cameraViewModel.updateCountdown(countdown - 1)
            }

            mediaActionSound.play(android.media.MediaActionSound.SHUTTER_CLICK) // Click
            scope.launch {
                flash.snapTo(0.85f)
                flash.animateTo(0f, tween(250))
            }
            // Reset the countdown now, not in the callback, so the next round cannot start at 0.
            cameraViewModel.updateCountdown(TIME_TO_CAPTURE)
            cameraViewModel.takePhoto(
                onPhotoCaptured = { uri ->
                    // Get file path from URI - handle both file:// and content:// URIs
                    val imagePath = when {
                        uri.scheme == "file" -> uri.path ?: uri.toString()
                        uri.scheme == "content" -> uri.toString() // Keep full URI for content://
                        else -> uri.path ?: uri.toString()
                    }
                    photoBoothViewModel.addCapturedImage(imagePath)
                    if (photoBoothViewModel.capturedImages.value.size >= PHOTO_LIMIT) {
                        cameraViewModel.stopCapturing()
                        goToSelection()
                    }
                },
                onError = {
                    cameraViewModel.stopCapturing()
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.booth_capture_failed))
                    }
                }
            )
            delay(POST_SHOT_DELAY)
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            cameraViewModel.initCamera(lifecycleOwner)
        }
    }

    val view = LocalView.current

    DisposableEffect(isCapturing) {
        view.keepScreenOn = isCapturing
        onDispose {
            view.keepScreenOn = false
        }
    }

    Scaffold(
        topBar = {
            DefaultAppBar(navController, stringResource(R.string.app_name))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = InkColors.background
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!cameraPermissionState.status.isGranted) {
                PermissionPrompt(
                    showSettings = permissionRequested && !cameraPermissionState.status.shouldShowRationale,
                    onRequest = {
                        permissionRequested = true
                        cameraPermissionState.launchPermissionRequest()
                    },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
            } else if (cameraViewModel.preview.value == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.booth_camera_starting), color = InkColors.textPrimary)
                }
            } else {
                // Camera preview, with a white flash layered over it on each shot.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .aspectRatio(4f / 3f)
                ) {
                    CameraView(
                        previewUseCase = cameraViewModel.preview.value!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = 2.dp,
                                color = InkColors.accent,
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = flash.value))
                    )
                }

                if (isCapturing) {
                    Text(
                        text = countdown.coerceAtLeast(0).toString(),
                        color = InkColors.textPrimary,
                        fontSize = 48.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.displayLarge
                    )
                }

                Text(
                    text = stringResource(R.string.booth_capture_progress, captureCount, PHOTO_LIMIT),
                    style = MaterialTheme.typography.titleSmall,
                    color = InkColors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Thumbnails stay on screen so the user can see what is already taken.
                if (capturedImages.isNotEmpty()) {
                    CaptureImages(capturedImages, Modifier.height(120.dp))
                }

                CaptureControls(
                    isCapturing = isCapturing,
                    captureCount = captureCount,
                    photoLimit = PHOTO_LIMIT,
                    requiredCount = requiredCount,
                    onStart = {
                        cameraViewModel.updateCountdown(TIME_TO_CAPTURE)
                        cameraViewModel.startCapturing()
                    },
                    onCancel = { cameraViewModel.stopCapturing() },
                    onFinish = { goToSelection() }
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(
    showSettings: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.permission_camera_rationale),
            style = MaterialTheme.typography.bodyLarge,
            color = InkColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (showSettings) {
            GlassButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.7f),
                text = stringResource(R.string.permission_open_settings)
            )
        } else {
            GlassButton(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(0.7f),
                text = stringResource(R.string.booth_permission_allow)
            )
        }
    }
}

@Composable
fun CameraView(
    previewUseCase: Preview,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val previewView = PreviewView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            previewUseCase.surfaceProvider = previewView.surfaceProvider
            Log.d("CameraView", "PreviewView created and SurfaceProvider set.")
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun CaptureImages(capturedImages: List<String>?, modifier: Modifier = Modifier) {
    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 16.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(capturedImages ?: emptyList()) { imagePath ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = InkColors.accent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .fillMaxHeight()
                        .aspectRatio(4f / 3f)
                ) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = stringResource(R.string.booth_captured_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun CaptureControls(
    isCapturing: Boolean,
    captureCount: Int,
    photoLimit: Int,
    requiredCount: Int,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        if (isCapturing) {
            SecondaryButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.6f),
                text = stringResource(R.string.booth_capture_cancel)
            )
        } else {
            if (captureCount < photoLimit) {
                GlassButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    text = stringResource(
                        if (captureCount == 0) R.string.start_capture else R.string.booth_capture_resume
                    )
                )
            }
            // Only offer to finish once there are enough shots for the chosen layout.
            if (captureCount >= requiredCount) {
                SecondaryButton(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    text = stringResource(R.string.booth_capture_done)
                )
            }
        }
    }
}
