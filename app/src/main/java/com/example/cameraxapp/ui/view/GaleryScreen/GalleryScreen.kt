package com.example.cameraxapp.ui.view.GaleryScreen

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.ui.components.DefaultAppBar
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.PhotogetherTheme
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    var images by remember { mutableStateOf(listOf<String>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(Unit) {
        images = fetchImages(context)
    }

    GalleryPickerContent(
        images = images,
        onBack = { navController.popBackStack() },
        onImageClick = { imagePath ->
            navController.navigate(AppRoutes.imageDetail(Uri.encode(imagePath)))
        }
    )
}

/** Stateless picker grid. */
@Composable
fun GalleryPickerContent(
    images: List<String>,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = "Chọn ảnh") },
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
            model = imagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun fetchImages(context: Context): List<String> {
    val images = mutableListOf<String>()
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        MediaStore.Images.Media.DATE_ADDED + " DESC"
    )

    cursor?.use {
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        while (cursor.moveToNext()) {
            images.add(cursor.getString(columnIndex))
        }
    }
    return images
}

@Preview(showBackground = true)
@Composable
private fun GalleryPickerPreview() {
    PhotogetherTheme {
        GalleryPickerContent(images = emptyList(), onBack = {}, onImageClick = {})
    }
}
