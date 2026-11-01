package com.example.photogether.ui.view.GaleryScreen

import com.example.photogether.core.navigation.popBackStackOnce
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.ui.theme.BinsooType
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
import org.koin.androidx.compose.koinViewModel

/**
 * [cameraOnly] lists just the photos taken on the camera screen; otherwise every image on the device.
 * In camera mode a tap opens the shot's detail (swipe to move between shots) and a long press starts
 * selecting several shots to delete. Otherwise a tap opens the editor.
 */
@Composable
fun GalleryScreen(navController: NavController, cameraOnly: Boolean = false) {
    val context = LocalContext.current
    val momentsViewModel: MomentsViewModel = koinViewModel()
    var shots by remember { mutableStateOf(listOf<CameraShot>()) }
    val images = remember(shots) { shots.map { it.path } }
    // Index in [shots] of the shot whose detail is open.
    var detailIndex by remember { mutableStateOf<Int?>(null) }
    // Paths picked in selection mode. Empty means the grid is not in selection mode.
    var checked by remember { mutableStateOf(setOf<String>()) }
    // Uris waiting for the app's own confirm dialog. Only used below API 30; API 30+ asks through the system dialog.
    var confirmUris by remember { mutableStateOf<List<String>?>(null) }
    // A delete asked for by the UI. A new instance each time, so LaunchedEffect runs again.
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    // The delete waiting on the system dialog (API 29 and up).
    var pendingConsent by remember { mutableStateOf<DeleteRequest?>(null) }

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

    // Result of the system delete dialog. A confirmed one deletes the rest; a cancel keeps the selection.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val request = pendingConsent
        pendingConsent = null
        if (request != null && result.resultCode == Activity.RESULT_OK) {
            deleteRequest = DeleteRequest(request.all, request.pending, afterConsent = true)
        }
    }

    LaunchedEffect(deleteRequest) {
        val request = deleteRequest ?: return@LaunchedEffect
        val result = if (request.afterConsent) {
            momentsViewModel.deleteAfterConsent(request.pending)
        } else {
            momentsViewModel.delete(request.pending)
        }
        when (result) {
            DeleteResult.Deleted -> {
                shots = shots.filterNot { it.uri in request.all }
                checked = emptySet()
                detailIndex = null
            }
            is DeleteResult.NeedsConfirmation -> {
                pendingConsent = DeleteRequest(request.all, result.pending, afterConsent = false)
                consentLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            }
            DeleteResult.Failed -> Toast.makeText(context, "Không xóa được ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * The only confirmation the user sees. API 30+ goes straight to the system dialog, which already asks.
     * Older versions ask in the app first, then delete.
     */
    fun requestDelete(uris: List<String>) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteRequest = DeleteRequest(uris, uris, afterConsent = false)
        } else {
            confirmUris = uris
        }
    }

    BackHandler(enabled = checked.isNotEmpty()) { checked = emptySet() }

    GalleryPickerContent(
        images = images,
        onBack = { navController.popBackStackOnce() },
        onImageClick = { imagePath ->
            when {
                cameraOnly && checked.isNotEmpty() -> checked = checked.toggle(imagePath)
                cameraOnly -> detailIndex = shots.indexOfFirst { it.path == imagePath }.takeIf { it >= 0 }
                else -> navController.navigate(AppRoutes.imageDetail(Uri.encode(imagePath)))
            }
        },
        checked = checked,
        onImageLongClick = if (cameraOnly) { imagePath -> checked = checked.toggle(imagePath) } else null,
        onClearSelection = { checked = emptySet() },
        onDeleteSelected = {
            requestDelete(shots.filter { it.path in checked }.map { it.uri })
        },
    )

    detailIndex?.let { index ->
        ShotDetailDialog(
            shots = shots.map { DetailShot(it.uri, it.takenAt, it.width, it.height) },
            initialIndex = index,
            onDismiss = { detailIndex = null },
            onEdit = { page ->
                val shot = shots[page]
                detailIndex = null
                navController.navigate(AppRoutes.imageDetail(Uri.encode(shot.path)))
            },
            onDelete = { page -> requestDelete(listOf(shots[page].uri)) },
        )
    }

    confirmUris?.let { uris ->
        AlertDialog(
            onDismissRequest = { confirmUris = null },
            title = {
                Text(
                    if (uris.size == 1) {
                        stringResource(R.string.dialog_delete_photo_title)
                    } else {
                        stringResource(R.string.dialog_delete_photos_title, uris.size)
                    },
                )
            },
            text = { Text(stringResource(R.string.dialog_delete_photo_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmUris = null
                        deleteRequest = DeleteRequest(uris, uris, afterConsent = false)
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUris = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * A delete the UI has asked for. [all] are the uris to drop from the list once it is done; [pending] are the
 * ones still to delete. Plain class, so every request is a new key for LaunchedEffect.
 */
private class DeleteRequest(val all: List<String>, val pending: List<String>, val afterConsent: Boolean)

private fun Set<String>.toggle(path: String): Set<String> = if (path in this) this - path else this + path

/**
 * Stateless picker grid. With [checked] not empty the grid is in selection mode and the top bar shows the count
 * with clear and delete actions. [onImageLongClick] null turns selecting off.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryPickerContent(
    images: List<String>,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    checked: Set<String> = emptySet(),
    onImageLongClick: ((String) -> Unit)? = null,
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            if (checked.isEmpty()) {
                DefaultAppBar(onBack = onBack, title = stringResource(R.string.picker_choose_photo))
            } else {
                SelectionBar(count = checked.size, onClose = onClearSelection, onDelete = onDeleteSelected)
            }
        },
        containerColor = AppColors.background
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            columns = GridCells.Adaptive(IconSize.hero * 2),
            contentPadding = PaddingValues(Spacing.sm),
            content = {
                items(images, key = { it }) { imagePath ->
                    ImageItem(
                        imagePath = imagePath,
                        isSelected = imagePath in checked,
                        onClick = { onImageClick(imagePath) },
                        onLongClick = onImageLongClick?.let { longClick -> { longClick(imagePath) } },
                    )
                }
            }
        )
    }
}

/** Top bar while selecting: clear the selection on the left, the count, and delete the selected photos on the right. */
@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(AppIcons.Close, contentDescription = stringResource(R.string.moments_deselect), tint = AppColors.textPrimary)
        }
        Text(
            text = stringResource(R.string.gallery_selected_count, count),
            style = BinsooType.title,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(AppIcons.Delete, contentDescription = stringResource(R.string.moments_delete_selected), tint = AppColors.danger)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageItem(
    imagePath: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .padding(Spacing.xs)
            .clip(RoundedCornerShape(Radius.sm))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            // A File, not the raw string: Coil reads a bare string as a URL, not a file path.
            model = File(imagePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AppColors.textPrimary.copy(alpha = 0.25f)),
            )
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = stringResource(R.string.moments_selected),
                tint = AppColors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.sm)
                    .size(IconSize.md)
                    .background(AppColors.accent, CircleShape)
                    .padding(Spacing.xs),
            )
        }
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
