package com.example.cameraxapp.ui.view.CameraScreen

import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.core.render.FilterCycle
import com.example.cameraxapp.ui.components.FilterStrip
import com.example.cameraxapp.ui.components.ManualControls
import com.example.cameraxapp.ui.components.ZoomControl
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Motion
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.viewmodel.CameraViewModel
import kotlin.math.abs
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

private const val TAG = "CameraScreen"
private const val FILTER_NAME_HOLD_MS = 900L

/** One filter-name flash. A new instance per selection, so the same name flashes again. */
class NameFlash(val name: String)

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
    val filters = remember { cameraViewModel.catalog.filters }
    val selectedId = cameraViewModel.editParams.value.filterId
    var flash by remember { mutableStateOf<NameFlash?>(null) }

    val onSelect: (String) -> Unit = { id ->
        cameraViewModel.selectFilter(id)
        flash = NameFlash(filters.firstOrNull { it.id == id }?.name.orEmpty())
    }
    val onSwipe: (Int) -> Unit = { delta ->
        onSelect(FilterCycle.next(filters.map { it.id }, selectedId, delta))
    }

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
                            width = 2.dp, color = Color.White, shape = RoundedCornerShape(Radius.md)
                        ),
                    previewUseCase = cameraViewModel.preview.value,
                    camera = cameraViewModel.camera.value,
                    flash = flash,
                    onSwipe = onSwipe,
                )
            }
            FilterStrip(
                filters = filters,
                selectedId = selectedId,
                onSelect = onSelect,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
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
    flash: NameFlash?,
    onSwipe: (Int) -> Unit,
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    LaunchedEffect(previewUseCase) {
        previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)
    }

    Column {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(Radius.md))
                .pointerInput(Unit) {
                    val threshold = Spacing.xxl.toPx()
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (abs(total) >= threshold) currentOnSwipe(if (total < 0f) 1 else -1)
                        },
                        onDragCancel = { total = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            total += dragAmount
                        },
                    )
                },
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            FilterNameFlash(flash)
        }
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Photo,
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape = RoundedCornerShape(Radius.sm))
                    .clickable { navController.navigate(AppRoutes.GALLERY) },
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.height(Spacing.md))
        camera?.let { bound ->
            ZoomControl(cameraControl = bound.cameraControl, cameraInfo = bound.cameraInfo)
        }
    }
}

/** Centre-of-preview filter name that fades out after a short hold. */
@Composable
private fun BoxScope.FilterNameFlash(flash: NameFlash?) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash == null) return@LaunchedEffect
        fade.snapTo(1f)
        delay(FILTER_NAME_HOLD_MS)
        fade.animateTo(0f, tween(Motion.DurationLong))
    }
    Text(
        text = flash?.name.orEmpty(),
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer { alpha = fade.value },
        style = MaterialTheme.typography.headlineSmall,
        color = AppColors.textPrimary,
    )
}

@ComposePreview
@Composable
fun ManualControlsPreview() {
    ManualControls(onExposureChange = {}, onISOChange = {}, onClickCapture = {})
}
// endregion
