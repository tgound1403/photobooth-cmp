package com.example.photogether.ui.view.GaleryScreen

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photogether.core.moments.CameraShot
import com.example.photogether.core.moments.CameraShotQuery
import com.example.photogether.core.moments.MomentQuery
import com.example.photogether.core.moments.displaySize
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.data.moments.DeleteResult
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.moments.MomentsViewModel
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * [cameraOnly] lists just the photos taken on the camera screen; otherwise every image on the device.
 * In camera mode a tap opens the shot's detail dialog. Otherwise a tap opens the editor.
 */
@Composable
fun GalleryScreen(navController: NavController, cameraOnly: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val momentsViewModel: MomentsViewModel = koinViewModel()
    var shots by remember { mutableStateOf(listOf<CameraShot>()) }
    val images = remember(shots) { shots.map { it.path } }
    var selected by remember { mutableStateOf<CameraShot?>(null) }
    var confirmDelete by remember { mutableStateOf<CameraShot?>(null) }
    // The shot whose delete is waiting on the system dialog (API 29 and up).
    var pendingConsent by remember { mutableStateOf<CameraShot?>(null) }

    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    // Tracked so the list loads only after the read permission is actually granted. Querying
    // MediaStore before the grant returns nothing, and the list was never reloaded afterwards.
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(readPermission)
    }

    LaunchedEffect(granted) {
        if (granted) shots = fetchShots(context, cameraOnly)
    }

    fun removeShot(shot: CameraShot) {
        shots = shots.filterNot { it.path == shot.path }
        if (selected?.path == shot.path) selected = null
    }

    // Result of the system delete dialog. Only a confirmed delete removes the shot from the list.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val shot = pendingConsent
        pendingConsent = null
        if (shot != null && result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                if (momentsViewModel.deleteAfterConsent(shot.uri) == DeleteResult.Deleted) removeShot(shot)
            }
        }
    }

    fun deleteShot(shot: CameraShot) {
        scope.launch {
            when (val result = momentsViewModel.delete(shot.uri)) {
                DeleteResult.Deleted -> removeShot(shot)
                is DeleteResult.NeedsConfirmation -> {
                    pendingConsent = shot
                    consentLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                }
                DeleteResult.Failed -> Toast.makeText(context, "Không xóa được ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    GalleryPickerContent(
        images = images,
        onBack = { navController.popBackStack() },
        onImageClick = { imagePath ->
            if (cameraOnly) {
                selected = shots.firstOrNull { it.path == imagePath }
            } else {
                navController.navigate(AppRoutes.imageDetail(Uri.encode(imagePath)))
            }
        }
    )

    selected?.let { shot ->
        ShotDetailDialog(
            uri = shot.uri,
            takenAt = shot.takenAt,
            width = shot.width,
            height = shot.height,
            onDismiss = { selected = null },
            onEdit = {
                selected = null
                navController.navigate(AppRoutes.imageDetail(Uri.encode(shot.path)))
            },
            onDelete = {
                // API 30+: the system dialog is the only confirmation. Older versions ask in the app first.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    selected = null
                    deleteShot(shot)
                } else {
                    confirmDelete = shot
                }
            },
        )
    }

    confirmDelete?.let { shot ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_photo_title)) },
            text = { Text(stringResource(R.string.dialog_delete_photo_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = null
                        deleteShot(shot)
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Stateless picker grid. */
@Composable
fun GalleryPickerContent(
    images: List<String>,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = stringResource(R.string.picker_choose_photo)) },
        containerColor = AppColors.background
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            columns = GridCells.Adaptive(IconSize.hero * 2),
            contentPadding = PaddingValues(Spacing.sm),
            content = {
                items(images) { imagePath ->
                    ImageItem(imagePath = imagePath, onClick = { onImageClick(imagePath) })
                }
            }
        )
    }
}

@Composable
private fun ImageItem(imagePath: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(Spacing.xs)
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            // A File, not the raw string: Coil reads a bare string as a URL, not a file path.
            model = File(imagePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Lists the images on the device, newest first. [cameraOnly] limits the list to the camera shots. */
fun fetchShots(context: Context, cameraOnly: Boolean = false): List<CameraShot> {
    val shots = mutableListOf<CameraShot>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.ORIENTATION,
        MediaStore.Images.Media.DISPLAY_NAME,
    )
    val selection = if (cameraOnly) CameraShotQuery.selection(Build.VERSION.SDK_INT) else null
    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection?.first,
        selection?.second,
        MediaStore.Images.Media.DATE_ADDED + " DESC"
    )

    cursor?.use {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val orientationCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val path = cursor.getString(dataCol) ?: continue
            val (width, height) = displaySize(
                width = cursor.getInt(widthCol),
                height = cursor.getInt(heightCol),
                orientation = cursor.getInt(orientationCol),
            )
            shots += CameraShot(
                path = path,
                uri = android.content.ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idCol),
                ).toString(),
                takenAt = cursor.getLong(dateCol) * 1000L,
                width = width,
                height = height,
                cameraId = MomentQuery.cameraIdOf(cursor.getString(nameCol)),
            )
        }
    }
    return shots
}

@Preview(showBackground = true)
@Composable
private fun GalleryPickerPreview() {
    PhotogetherTheme {
        GalleryPickerContent(images = emptyList(), onBack = {}, onImageClick = {})
    }
}
