package com.example.cameraxapp.ui.view

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.ui.viewmodel.CameraViewModel
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoBoothScreen(
    navController: NavController,
    photoBoothViewModel: PhotoBoothViewModel,
    cameraViewModel: CameraViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val capturedImages by photoBoothViewModel.capturedImages.observeAsState(initial = emptyList())
    val captureCount = capturedImages.size

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val TIME_TO_CAPTURE = 5
    val PHOTO_LIMIT = 5
    
    val isCapturing by cameraViewModel.isCapturing.collectAsState()
    val countdown by cameraViewModel.countdown.collectAsState()

    LaunchedEffect(isCapturing) {
        while (isCapturing && captureCount < PHOTO_LIMIT) {

            while (countdown > 0) {
                delay(1000)
                cameraViewModel.updateCountdown(countdown - 1)
            }

            if (countdown == 0) {
                cameraViewModel.takePhoto(
                    onPhotoCaptured = { uri ->
                        photoBoothViewModel.addCapturedImage(uri.toString())
                        cameraViewModel.updateCountdown(TIME_TO_CAPTURE)
                        if (photoBoothViewModel.capturedImages.value?.size == PHOTO_LIMIT) {
                            cameraViewModel.stopCapturing()
                            navController.navigate("photoBoothSelection") {
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
        } else {
            Log.d("PhotoBoothScreen", "Camera permission not granted")
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    if (cameraViewModel.preview.value != null) {
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .aspectRatio(4f / 3f)
                            )
                            if (isCapturing) {
                                Text(
                                    text = if (countdown >= 0) countdown.toString() else "0",
                                    color = Color.White,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            Text("Đang khởi tạo camera...")
                        }
                    }
                }

                cameraPermissionState.status.shouldShowRationale -> {
                    PermissionRationale(modifier = Modifier.weight(1f)) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }

                else -> {
                    RequestPermission(modifier = Modifier.weight(1f)) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }
            }

            if (capturedImages.isNotEmpty()) {
                CaptureImages(capturedImages, Modifier.height(140.dp))
            }

            CaptureButton(
                onClick = {
                    if (!isCapturing) {
                        cameraViewModel.startCapturing()
                    } else {
                        cameraViewModel.stopCapturing()
                    }
                },
                isEnabled =  captureCount < 8,
                isCapturing = isCapturing
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
fun PermissionRationale(modifier: Modifier = Modifier, onRequestPermission: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Ứng dụng cần quyền truy cập Camera để chụp ảnh.\n" +
                    "Vui lòng cấp quyền để tiếp tục.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Yêu cầu lại quyền")
        }
    }
}

@Composable
fun RequestPermission(modifier: Modifier = Modifier, onRequestPermission: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Vui lòng cấp quyền truy cập Camera trong cài đặt ứng dụng.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Cấp quyền")
        }
    }
}

@Composable
fun CaptureImages(capturedImages: List<String>?, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(capturedImages ?: emptyList()) { imagePath ->
            Box(modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxHeight()
                .aspectRatio(4f / 3f)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
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

@Composable
fun CaptureButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isCapturing: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        enabled = isEnabled
    ) {
        Text(if (isCapturing) "Dừng chụp" else "Bắt đầu chụp")
    }
}


@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CaptureImagesPreview() {
    CaptureImages(listOf("", "", ""), Modifier.height(140.dp))
}


@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CaptureButtonPreview() {
    CaptureButton(
        onClick = { },
        isEnabled = true,
        isCapturing = false
    )
}