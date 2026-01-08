package com.example.cameraxapp.ui.view

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.ui.components.DefaultAppBar
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.DeepBlack
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.theme.NeonPurple
import com.example.cameraxapp.ui.viewmodel.CameraViewModel
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoBoothScreen(
    navController: NavController,
    photoBoothViewModel: PhotoBoothViewModel,
    cameraViewModel: CameraViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val capturedImages by photoBoothViewModel.capturedImages.collectAsState()
    val captureCount = capturedImages.size

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val TIME_TO_CAPTURE = 5
    val requiredPhotoCount by photoBoothViewModel.requiredPhotoCount.collectAsState()
    val PHOTO_LIMIT = requiredPhotoCount

    val isCapturing by cameraViewModel.isCapturing.collectAsState()
    val countdown by cameraViewModel.countdown.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaActionSound = remember { android.media.MediaActionSound() }
    val toneGenerator = remember { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100) }

    DisposableEffect(Unit) {
        mediaActionSound.load(android.media.MediaActionSound.SHUTTER_CLICK)
        onDispose {
            mediaActionSound.release()
            toneGenerator.release()
        }
    }

    LaunchedEffect(isCapturing) {
        while (isCapturing && captureCount < PHOTO_LIMIT) {

            while (countdown > 0) {
                toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150) // Beep
                delay(1000)
                cameraViewModel.updateCountdown(countdown - 1)
            }

            if (countdown == 0) {
                mediaActionSound.play(android.media.MediaActionSound.SHUTTER_CLICK) // Click
                cameraViewModel.takePhoto(
                    onPhotoCaptured = { uri ->
                        photoBoothViewModel.addCapturedImage(uri.path.toString())
                        cameraViewModel.updateCountdown(TIME_TO_CAPTURE)
                        // Check against live value or local constant
                        if (photoBoothViewModel.capturedImages.value.size == PHOTO_LIMIT) {
                            cameraViewModel.stopCapturing()
                            navController.navigate(AppRoutes.PHOTO_BOOTH_SELECTION) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    onError = { exception -> cameraViewModel.stopCapturing() }
                )
                delay(3000)
            }
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            cameraViewModel.initCamera(lifecycleOwner)
        }
    }


    Scaffold(
        topBar = {
            DefaultAppBar(navController, "Photogether")
        },
        containerColor = DeepBlack
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepBlack, Color(0xFF1a0b2e))
                    )
                )
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    if (cameraViewModel.preview.value != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Camera Preview Box
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
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            width = 4.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(NeonCyan, NeonPurple)
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .aspectRatio(4f / 3f)
                                )
                            }
                            
                            // Interaction Area
                            if (isCapturing) {
                                Text(
                                    text = if (countdown >= 0) countdown.toString() else "0",
                                    color = NeonCyan,
                                    fontSize = 48.sp,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                            
                            if (capturedImages.isNotEmpty()) {
                                CaptureImages(capturedImages, Modifier.height(120.dp))
                            } else {
                                CaptureButton(
                                    onClick = {
                                        cameraViewModel.startCapturing()
                                    },
                                    isEnabled = captureCount < PHOTO_LIMIT,
                                    isCapturing = isCapturing
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Đang khởi tạo camera...", color = Color.White)
                        }
                    }
                }
            }
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
                            color = NeonPurple, 
                            shape = RoundedCornerShape(8.dp)
                        )
                        .fillMaxHeight()
                        .aspectRatio(4f / 3f)
                ) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = "Captured image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaptureButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isCapturing: Boolean
) {
    if (!isCapturing) {
        GlassButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 32.dp),
            text = "Bắt đầu chụp",
            enabled = isEnabled
        )
    } else {
         // Show nothing or stop button? 
         // For now hiding button is fine as per logic
    }
}
