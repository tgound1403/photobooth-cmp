package com.example.cameraxapp.ui.view

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cameraxapp.data.api.UnsplashPhoto
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import com.example.cameraxapp.ui.viewmodel.BackgroundSelectorViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothResultScreen(
    photoBoothId: Long,
    navController: NavController,
    viewModel: PhotoBoothViewModel,
    backgroundSelectorViewModel: BackgroundSelectorViewModel = koinViewModel<BackgroundSelectorViewModel>()
) {
    val photoBooth by viewModel.photoBooth.observeAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showBackgroundSelector by remember { mutableStateOf(false) }
    var showDeviceImagePicker by remember { mutableStateOf(false) }
    var selectedBackground by remember { mutableStateOf<UnsplashPhoto?>(null) }
    var selectedDeviceImage by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(photoBoothId) {
        viewModel.getPhotoBoothById(photoBoothId)
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is PhotoBoothViewModel.SaveState.Success -> {
                snackbarHostState.showSnackbar("Đã lưu ảnh thành công!")
            }
            is PhotoBoothViewModel.SaveState.Error -> {
                snackbarHostState.showSnackbar("Lỗi khi lưu ảnh: ${(saveState as PhotoBoothViewModel.SaveState.Error).message}")
            }
            else -> {}
        }
    }

    if (showBackgroundSelector) {
        BackgroundSelector(
            viewModel = backgroundSelectorViewModel,
            onBackgroundSelected = { photo ->
                selectedBackground = photo
                selectedDeviceImage = null
                showBackgroundSelector = false
            },
            onDismiss = { showBackgroundSelector = false }
        )
    }

    if (showDeviceImagePicker) {
        DeviceImagePicker(
            onImageSelected = { uri ->
                selectedDeviceImage = uri
                selectedBackground = null
                showDeviceImagePicker = false
            },
            onDismiss = { showDeviceImagePicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Photo Booth Result") },
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
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, Color.White)
            ) {
                selectedBackground?.let { background ->
                    AsyncImage(
                        model = background.urls.regular,
                        contentDescription = "Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                selectedDeviceImage?.let { uri ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Device background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    photoBooth?.imagePaths?.forEach { imagePath ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = "Photo booth image",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { navController.navigate("photoBooth") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take New Photos")
                }

                Button(
                    onClick = { showBackgroundSelector = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unsplash")
                }

                Button(
                    onClick = { showDeviceImagePicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thiết bị")
                }

                Button(
                    onClick = { 
                        photoBooth?.imagePaths?.let { imagePaths ->
                            when {
                                selectedBackground != null -> {
                                    viewModel.saveImageWithBg(
                                        context,
                                        imagePaths,
                                        selectedBackground!!.urls.regular
                                    )
                                }
                                selectedDeviceImage != null -> {
                                    viewModel.saveImageWithBg(
                                        context,
                                        imagePaths,
                                        selectedDeviceImage.toString()
                                    )
                                }
                                else -> {
                                    viewModel.saveImages(context, imagePaths)
                                }
                            }
                        }
                    },
                    enabled = saveState !is PhotoBoothViewModel.SaveState.Saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Result")
                }
            }
        }
    }
}