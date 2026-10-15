package com.example.cameraxapp.ui.view

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.data.model.FrameTheme
import com.example.cameraxapp.data.model.FrameThemes
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.DeepBlack
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.view.DeviceImagePicker
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.viewmodel.EditImageViewModel
import com.example.cameraxapp.core.enum.TabType
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothEditScreen(
    imagePaths: List<String>,
    navController: NavController,
    viewModel: PhotoBoothViewModel,
    editImageViewModel: EditImageViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveState by viewModel.saveState.collectAsState()
    val selectedLayout by viewModel.selectedLayout.collectAsState()
    
    // Filter state
    var selectedFilter by remember { mutableStateOf(editImageViewModel.filters[0]) }
    var filteredImagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var isApplyingFilter by remember { mutableStateOf(false) }
    
    // Get current image paths (filtered or original)
    val currentImagePaths = remember(filteredImagePaths, imagePaths) {
        if (filteredImagePaths.isNotEmpty() && filteredImagePaths.size == imagePaths.size) {
            filteredImagePaths
        } else {
            imagePaths
        }
    }
    
    // Frame background state
    var selectedFrameTheme by remember { mutableStateOf<FrameTheme?>(null) }
    var selectedBackgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    
    // Text overlay state
    var customText by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val defaultText = remember { dateFormat.format(Date()) }
    var textFieldValue by remember { mutableStateOf(defaultText) }
    
    // Preview state
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    
    // Generate preview when settings change
    LaunchedEffect(selectedFrameTheme, selectedBackgroundImageUri, textFieldValue, currentImagePaths) {
        if (currentImagePaths.isNotEmpty()) {
            isGeneratingPreview = true
            scope.launch {
                try {
                    val backgroundSource = selectedBackgroundImageUri?.toString()
                    val themeToUse = if (backgroundSource != null) null else selectedFrameTheme
                    val textToUse = if (textFieldValue.isBlank() || textFieldValue == defaultText) null else textFieldValue
                    
                    val result = viewModel.createPhotoBoothPreview(
                        context,
                        currentImagePaths,
                        selectedLayout,
                        backgroundSource,
                        themeToUse,
                        textToUse
                    )
                    result.onSuccess { uri ->
                        previewImageUri = uri
                        isGeneratingPreview = false
                    }.onFailure {
                        isGeneratingPreview = false
                    }
                } catch (e: Exception) {
                    isGeneratingPreview = false
                }
            }
        }
    }
    
    // Navigate to result when save is successful
    LaunchedEffect(saveState) {
        if (saveState is PhotoBoothViewModel.SaveState.Success) {
            // Get the saved photo booth ID and navigate to result
            viewModel.photoBooth.value?.let { photoBooth ->
                navController.navigate(AppRoutes.photoBoothResult(photoBooth.id)) {
                    popUpTo(AppRoutes.PHOTO_BOOTH_SELECTION) { inclusive = true }
                }
            } ?: run {
                navController.navigate(AppRoutes.GALLERY) {
                    popUpTo(AppRoutes.PHOTO_BOOTH_SELECTION) { inclusive = true }
                }
            }
        }
    }
    
    // Image picker dialog
    if (showImagePicker) {
        DeviceImagePicker(
            onImageSelected = { uri ->
                selectedBackgroundImageUri = uri
                selectedFrameTheme = null
                showImagePicker = false
            },
            onDismiss = { showImagePicker = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh Sửa Photo Booth", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlack,
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(
                        onClick = {
                            val backgroundSource = selectedBackgroundImageUri?.toString()
                            val themeToUse = if (backgroundSource != null) null else selectedFrameTheme
                            viewModel.updateCustomText(customText)
                            viewModel.updateBackgroundImage(selectedBackgroundImageUri)
                            if (themeToUse != null) {
                                viewModel.updateTheme(themeToUse)
                            }
                            
                            scope.launch {
                                viewModel.saveImages(context, currentImagePaths)
                            }
                        },
                        enabled = saveState !is PhotoBoothViewModel.SaveState.Saving
                    ) {
                        Text(
                            if (saveState is PhotoBoothViewModel.SaveState.Saving) "Đang lưu..." else "Lưu",
                            color = if (saveState is PhotoBoothViewModel.SaveState.Saving) Color.Gray else NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepBlack, Color(0xFF1a0b2e))
                    )
                )
        ) {
            // Preview Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGeneratingPreview) {
                    CircularProgressIndicator(color = NeonCyan)
                } else if (previewImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo booth preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Đang tạo preview...", color = Color.White)
                }
            }
            
            // Tab selection
            var selectedTab by remember { mutableStateOf(TabType.CUSTOMIZE) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(TabType.BASIC, TabType.CUSTOMIZE).forEach { tab ->
                    TextButton(
                        onClick = { selectedTab = tab },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (selectedTab == tab) NeonCyan else Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            when (tab) {
                                TabType.BASIC -> "Filter"
                                TabType.CUSTOMIZE -> "Khung & Nền"
                                else -> ""
                            },
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            
            // Editing Options
            when (selectedTab) {
                TabType.BASIC -> {
                    // Filter selection
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Chọn filter màu (áp dụng cho tất cả ảnh)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (isApplyingFilter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan)
                            }
                        }
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                        ) {
                            items(editImageViewModel.filters) { filter ->
                                val isSelected = selectedFilter.id == filter.id
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(120.dp)
                                        .background(
                                            color = if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedFilter = filter
                                            // Apply filter to all images
                                            isApplyingFilter = true
                                            scope.launch {
                                                val filteredPaths = mutableListOf<String>()
                                                imagePaths.forEachIndexed { index, imagePath ->
                                                    try {
                                                        val bitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            android.graphics.BitmapFactory.decodeFile(imagePath)
                                                        }
                                                        if (bitmap != null) {
                                                            val filteredBitmap = editImageViewModel.renderEdit(bitmap, EditParams(filterId = filter.id))
                                                            val file = java.io.File(context.cacheDir, "filtered_${index}_${System.currentTimeMillis()}.jpg")
                                                            file.outputStream().use { out ->
                                                                filteredBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                                            }
                                                            filteredPaths.add(file.absolutePath)
                                                            filteredBitmap.recycle()
                                                            bitmap.recycle()
                                                        } else {
                                                            filteredPaths.add(imagePath)
                                                        }
                                                    } catch (e: Exception) {
                                                        filteredPaths.add(imagePath)
                                                    }
                                                }
                                                filteredImagePaths = filteredPaths
                                                isApplyingFilter = false
                                            }
                                        }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.dp, NeonCyan, RoundedCornerShape(8.dp))
                                            } else Modifier
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Preview với filter
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.3f))
                                        ) {
                                            AsyncImage(
                                                model = if (imagePaths.isNotEmpty()) imagePaths[0] else "",
                                                contentDescription = "Filter preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Text(
                                            text = filter.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TabType.CUSTOMIZE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // Background selection
                GlassButton(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    text = if (selectedBackgroundImageUri != null) "Đổi ảnh nền" else "Chọn ảnh nền"
                )
                
                if (selectedBackgroundImageUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = selectedBackgroundImageUri,
                                contentDescription = "Background preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedBackgroundImageUri = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Xóa ảnh nền",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Theme selection
                Text(
                    "Hoặc chọn theme màu",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                ) {
                    items(FrameThemes.ALL_THEMES) { theme ->
                        val isSelected = selectedFrameTheme?.id == theme.id && selectedBackgroundImageUri == null
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(100.dp)
                                .background(
                                    color = Color(theme.backgroundColor),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedFrameTheme = theme
                                    selectedBackgroundImageUri = null
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, NeonCyan, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(
                                            NeonCyan,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.Black, fontSize = 12.sp)
                                }
                            }
                            Text(
                                text = theme.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (theme.backgroundColor == android.graphics.Color.WHITE ||
                                    theme.backgroundColor == android.graphics.Color.parseColor("#F5E6D3")
                                ) Color.Black else Color.White,
                                maxLines = 2
                            )
                        }
                    }
                }
                
                // Text input
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        customText = if (newValue.isBlank() || newValue == defaultText) null else newValue
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { 
                        Text(
                            "Chỉnh sửa dòng chữ trên ảnh",
                            color = Color.White.copy(alpha = 0.7f)
                        ) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true
                )
                    }
                }
                else -> {}
            }
        }
    }
}
