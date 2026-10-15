package com.example.cameraxapp.ui.view.CameraScreen

import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.ui.components.ManualControls
import com.example.cameraxapp.ui.components.ZoomControl
import com.example.cameraxapp.ui.viewmodel.CameraViewModel
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

private const val TAG = "CameraScreen"

// region Compose
@Composable
fun CameraScreen(
    navController: NavController,
    cameraViewModel: CameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exposureTime = remember { mutableLongStateOf(10_000_000L) }
    val isoValue = remember { mutableIntStateOf(1000) }

    LaunchedEffect(lifecycleOwner) {
        cameraViewModel.initCamera(lifecycleOwner, CameraSelector.LENS_FACING_BACK)
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(20.dp)) {
                CameraPreviewViewfinder(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .border(
                            width = 2.dp, color = Color.White, shape = RoundedCornerShape(16.dp)
                        ),
                    previewUseCase = cameraViewModel.preview.value,
                    camera = cameraViewModel.camera.value,
                )
            }
            ManualControls(
                onExposureChange = {
                    exposureTime.longValue = it
                    cameraViewModel.setManualExposure(it, isoValue.intValue)
                },
                onISOChange = {
                    isoValue.intValue = it
                    cameraViewModel.setManualExposure(exposureTime.longValue, it)
                },
                onClickCapture = {
                    cameraViewModel.takePhotoToGallery(
                        onSaved = { Toast.makeText(context, "Photo taken", Toast.LENGTH_SHORT).show() },
                        onError = { exception -> Log.e(TAG, "Capture failed", exception) },
                    )
                },
            )
        }
    }
}

@Composable
fun CameraPreviewViewfinder(
    navController: NavController,
    modifier: Modifier,
    previewUseCase: Preview?,
    camera: Camera?,
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    LaunchedEffect(previewUseCase) {
        previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)
    }

    Column {
        AndroidView(
            factory = { previewView },
            modifier = modifier.clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Photo,
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .clickable { navController.navigate(AppRoutes.GALLERY) },
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        camera?.let { bound ->
            ZoomControl(cameraControl = bound.cameraControl, cameraInfo = bound.cameraInfo)
        }
    }
}

@ComposePreview
@Composable
fun ManualControlsPreview() {
    ManualControls(onExposureChange = {}, onISOChange = {}, onClickCapture = {})
}
// endregion
