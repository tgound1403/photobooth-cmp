package com.example.cameraxapp.ui.view.EditImageScreen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.example.cameraxapp.core.enum.EditType
import com.example.cameraxapp.core.enum.ImageInfo
import com.example.cameraxapp.core.enum.TabType
import com.example.cameraxapp.data.model.FrameTheme
import com.example.cameraxapp.data.model.FrameThemes
import com.example.cameraxapp.data.model.Preset
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.view.DeviceImagePicker
import com.example.cameraxapp.viewmodel.EditImageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.cameraxapp.core.navigation.AppRoutes
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImageScreen(
    imagePath: String,
    navController: NavController,
    viewmodel: EditImageViewModel = koinViewModel()
) {
    val photoEditFeatures = viewmodel.photoEditFeatures
    val filters = viewmodel.filters
    val tabs = viewmodel.tabs
    val frames = viewmodel.frames
    val lightLeaks = viewmodel.lightLeaks
    val distortions = viewmodel.distortions
    val scratches = viewmodel.scratches

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Helper function to load bitmap from path or URI
    suspend fun loadBitmap(pathOrUri: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            when {
                // Check if it's a URI string (content:// or file://)
                pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") -> {
                    val uri = Uri.parse(pathOrUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
                // Check if it's a file path
                else -> {
                    // Try direct file path first
                    val file = java.io.File(pathOrUri)
                    if (file.exists() && file.canRead()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        // Try as file:// URI
                        try {
                            val fileUri = Uri.fromFile(file)
                            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            // Last resort: try parsing as URI string
                            try {
                                val uri = Uri.parse(pathOrUri)
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    BitmapFactory.decodeStream(inputStream)
                                }
                            } catch (e2: Exception) {
                                android.util.Log.e("EditImageScreen", "Failed to load bitmap: ${e2.message}", e2)
                                null
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EditImageScreen", "Error loading bitmap from: $pathOrUri", e)
            null
        }
    }

    // State Management Fix: Load bitmap asynchronously
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var temporaryBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filteredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(true) }
    
    // Load bitmap in coroutine
    LaunchedEffect(imagePath) {
        isLoadingBitmap = true
        originalBitmap = loadBitmap(imagePath)
        temporaryBitmap = originalBitmap
        filteredBitmap = originalBitmap
        isLoadingBitmap = false
    }
    
    // Show loading or error state
    if (isLoadingBitmap) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    title = { Text("Loading...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Loading image...",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
        return
    }
    
    // Early return if bitmap is null
    if (originalBitmap == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    title = { Text("Error") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Could not load image",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please check if the file exists",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        return
    }

    // Debounce Job
    var processingJob by remember { mutableStateOf<Job?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val lottieComposition by
    rememberLottieComposition(
        LottieCompositionSpec.Url(
            "https://assets2.lottiefiles.com/packages/lf20_m6cu9nha.json"
        )
    )
    val lottieProgress by
    animateLottieCompositionAsState(
        lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    var selectedFeature by remember { mutableStateOf(EditType.FILTER) }
    var selectedFilter by remember { mutableStateOf(filters[0]) }
    var selectedFrame by remember { mutableStateOf(frames[0]) }
    var selectedLightLeak by remember { mutableStateOf(lightLeaks[0]) }
    var selectedDistortion by remember { mutableStateOf(distortions[0]) }
    var selectedScratch by remember { mutableStateOf(scratches[0]) }
    var selectedTab by remember { mutableStateOf(tabs[0]) }
    var selectedInfo by remember { mutableStateOf(ImageInfo.NONE) }
    
    // Frame background state
    var selectedFrameTheme by remember { mutableStateOf<FrameTheme?>(null) }
    var selectedBackgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    
    // Text overlay state
    var customText by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val defaultText = remember { dateFormat.format(Date()) }
    var textFieldValue by remember { mutableStateOf(defaultText) }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var temperature by remember { mutableFloatStateOf(0f) }
    var sharpness by remember { mutableFloatStateOf(0f) }
    var highlight by remember { mutableFloatStateOf(0f) }
    var shadow by remember { mutableFloatStateOf(0f) }
    var grain by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var chromaticAberration by remember { mutableFloatStateOf(0f) }

    // Helper for Icons
    fun getIconForImageInfo(info: ImageInfo): androidx.compose.ui.graphics.vector.ImageVector {
        return when (info) {
            ImageInfo.BRIGHTNESS -> Icons.Default.Brightness6
            ImageInfo.CONTRAST -> Icons.Default.Tune // Tune as Contrast
            ImageInfo.SATURATION -> Icons.Default.Brush // Brush as Saturation
            ImageInfo.TEMPERATURE -> Icons.Default.DeviceThermostat // Thermostat
            ImageInfo.SHARPNESS -> Icons.Default.AutoFixHigh
            ImageInfo.HIGHLIGHT -> Icons.Default.WbSunny
            ImageInfo.SHADOW -> Icons.Default.Cloud
            ImageInfo.GRAIN -> Icons.Default.Brush
            ImageInfo.VIGNETTE -> Icons.Default.Adjust
            ImageInfo.CHROMATIC_ABERRATION -> Icons.Default.AutoFixHigh
            ImageInfo.NONE -> Icons.Default.Edit
        }
    }

    @Composable
    fun TabSelection(
        tabs: List<String>,
        selection: String,
        onSelect: (String) -> Unit,
        selectedColor: Color
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tabs.size) {
                Row {
                    Text(
                        tabs[it],
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        modifier =
                            Modifier
                                .background(
                                    shape = RoundedCornerShape(8.dp),
                                    color =
                                        if (tabs[it] == selection)
                                            selectedColor.copy(alpha = 0.5f)
                                        else Color.Transparent
                                )
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clickable { onSelect.invoke(tabs[it]) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }

    @Composable
    fun BasicEditGroup() {
        LazyRow {
            items(viewmodel.imageInfo) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(.8f)
                            .clickable {
                                selectedInfo = it
                                temporaryBitmap = filteredBitmap
                            },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        getIconForImageInfo(it),
                        modifier = Modifier.size(48.dp),
                        contentDescription = it.name,
                        tint = Color.White
                    )
                    Text(
                        it.name,
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        modifier =
                            Modifier
                                .background(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                )
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    fun onAcceptChangeInfo() {
        temporaryBitmap?.let { filteredBitmap = it }
        selectedInfo = ImageInfo.NONE
    }

    fun onCancelChangeInfo() {
        selectedInfo = ImageInfo.NONE
        filteredBitmap?.let { temporaryBitmap = it }
    }

    // Helper function to apply all basic edits (brightness, contrast, etc.)
    suspend fun applyAllBasicEdits(baseBitmap: Bitmap): Bitmap {
        var result = baseBitmap
        
        // Apply basic edits in order
        if (brightness != 0f) {
            result = viewmodel.applyBrightness(result, brightness)
        }
        if (contrast != 0f) {
            result = viewmodel.applyContrast(result, contrast)
        }
        if (saturation != 0f) {
            result = viewmodel.applySaturation(result, saturation)
        }
        if (temperature != 0f) {
            result = viewmodel.applyTemperature(result, temperature)
        }
        if (sharpness != 0f) {
            result = viewmodel.applySharpness(result, sharpness)
        }
        if (highlight != 0f) {
            result = viewmodel.applyHighlight(result, highlight)
        }
        if (shadow != 0f) {
            result = viewmodel.applyShadow(result, shadow)
        }
        if (grain != 0f) {
            result = viewmodel.applyGrain(result, grain)
        }
        if (vignette != 0f) {
            result = viewmodel.applyVignette(result, vignette)
        }
        if (chromaticAberration != 0f) {
            result = viewmodel.applyChromaticAberration(result, chromaticAberration)
        }
        
        return result
    }
    
    // Helper function to apply all selected edits sequentially
    suspend fun applyAllEdits(baseBitmap: Bitmap): Bitmap {
        var result = baseBitmap
        
        // Apply basic edits first (brightness, contrast, etc.)
        result = applyAllBasicEdits(result)
        
        // Apply filter (color correction)
        if (selectedFilter.name != "None") {
            result = viewmodel.applyPresetFilter(result, selectedFilter.matrix)
        }
        
        // Apply light leak
        if (selectedLightLeak != lightLeaks[0]) {
            result = viewmodel.applyLightLeak(result, selectedLightLeak)
        }
        
        // Apply distortion
        if (selectedDistortion != distortions[0]) {
            result = viewmodel.applyDistortion(result, selectedDistortion)
        }
        
        // Apply scratch
        if (selectedScratch != scratches[0]) {
            result = viewmodel.applyScratch(result, selectedScratch)
        }
        
        return result
    }

    @Composable
    fun BasicEditDetail() {
        val currentBitmap = filteredBitmap
        if (currentBitmap == null) {
            Text(
                "Error: Could not load image",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
            return
        }
        
        when (selectedInfo) {
            ImageInfo.BRIGHTNESS ->
                EditSlider(
                    value = brightness,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        brightness = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyBrightness(
                                        currentBitmap,
                                        brightness
                                    )
                                isProcessing = false
                            }
                    },
                    range = -1f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.CONTRAST ->
                EditSlider(
                    value = contrast,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        contrast = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyContrast(
                                        currentBitmap,
                                        contrast
                                    )
                                isProcessing = false
                            }
                    },
                    range = -1f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.SATURATION ->
                EditSlider(
                    value = saturation,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        saturation = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applySaturation(
                                        currentBitmap,
                                        saturation
                                    )
                                isProcessing = false
                            }
                    },
                    range = -1f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.TEMPERATURE ->
                EditSlider(
                    value = temperature,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        temperature = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyTemperature(
                                        currentBitmap,
                                        temperature
                                    )
                                isProcessing = false
                            }
                    },
                    range = -3f..3f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.SHARPNESS ->
                EditSlider(
                    value = sharpness,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        sharpness = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applySharpness(
                                        currentBitmap,
                                        sharpness
                                    )
                                isProcessing = false
                            }
                    },
                    range = 0f..1f, // Sharpness usually 0 to 1
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.HIGHLIGHT ->
                EditSlider(
                    value = highlight,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        highlight = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyHighlight(
                                        currentBitmap,
                                        highlight
                                    )
                                isProcessing = false
                            }
                    },
                    range = -1f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.SHADOW ->
                EditSlider(
                    value = shadow,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        shadow = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyShadow(currentBitmap, shadow)
                                isProcessing = false
                            }
                    },
                    range = -1f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.GRAIN ->
                EditSlider(
                    value = grain,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        grain = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyGrain(currentBitmap, grain)
                                isProcessing = false
                            }
                    },
                    range = 0f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.VIGNETTE ->
                EditSlider(
                    value = vignette,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        vignette = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyVignette(
                                        currentBitmap,
                                        vignette
                                    )
                                isProcessing = false
                            }
                    },
                    range = 0f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.CHROMATIC_ABERRATION ->
                EditSlider(
                    value = chromaticAberration,
                    title = selectedInfo.name,
                    onValueChange = { change ->
                        chromaticAberration = change
                        processingJob?.cancel()
                        processingJob =
                            scope.launch {
                                temporaryBitmap =
                                    viewmodel.applyChromaticAberration(
                                        currentBitmap,
                                        chromaticAberration
                                    )
                                isProcessing = false
                            }
                    },
                    range = 0f..1f,
                    onCancel = { onCancelChangeInfo() },
                    onAccept = { onAcceptChangeInfo() }
                )

            ImageInfo.NONE -> Unit
        }
    }

    @Composable
    fun FrameBackgroundSelector() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Upload image button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.weight(1f),
                    text = if (selectedBackgroundImageUri != null) "Đổi ảnh nền" else "Chọn ảnh nền"
                )
                if (selectedBackgroundImageUri != null) {
                    IconButton(
                        onClick = {
                            selectedBackgroundImageUri = null
                            selectedFrameTheme = null
                            // Reset frame
                            originalBitmap?.let { filteredBitmap = it }
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
            
            // Preview selected background image
            selectedBackgroundImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Background preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Hoặc chọn theme màu",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Theme selection
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
                                // Apply theme - margin = 0 để nền là nền của ảnh, không tạo frame
                                originalBitmap?.let { bitmap ->
                                    isProcessing = true
                                    scope.launch {
                                        filteredBitmap = viewmodel.applyFrameBackground(
                                            context,
                                            bitmap,
                                            theme,
                                            null,
                                            margin = 0f,
                                            customText = if (textFieldValue.isNotBlank() && textFieldValue != defaultText) textFieldValue else null
                                        )
                                        isProcessing = false
                                    }
                                }
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, Color.Cyan, RoundedCornerShape(8.dp))
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
                                        Color.Cyan,
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
            
            // Text input section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Chỉnh sửa dòng chữ trên ảnh",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    customText = if (newValue.isBlank() || newValue == defaultText) null else newValue
                    // Apply text to current bitmap
                    filteredBitmap?.let { bitmap ->
                        isProcessing = true
                        scope.launch {
                            val textToApply = if (newValue.isBlank() || newValue == defaultText) null else newValue
                            if (selectedFrameTheme != null || selectedBackgroundImageUri != null) {
                                // If background is applied, reapply with new text - margin = 0
                                originalBitmap?.let { original ->
                                    filteredBitmap = viewmodel.applyFrameBackground(
                                        context,
                                        original,
                                        selectedFrameTheme,
                                        selectedBackgroundImageUri,
                                        margin = 0f,
                                        customText = textToApply
                                    )
                                }
                            } else {
                                // Apply text directly to bitmap
                                filteredBitmap = if (textToApply != null) {
                                    viewmodel.applyTextToBitmap(bitmap, textToApply)
                                } else {
                                    originalBitmap
                                }
                            }
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { 
                    Text(
                        "Nhập text (để trống để dùng thời gian mặc định)",
                        color = Color.White.copy(alpha = 0.7f)
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color.Cyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Text is already updated via onValueChange
                    }
                ),
                singleLine = true
            )
        }
    }

    @Composable
    fun StyledEdit(imagePath: String, editType: EditType) {
        val selection =
            when (editType) {
                EditType.FILTER -> selectedFilter
                EditType.FRAME -> selectedFrame
                EditType.LIGHT_LEAK -> selectedLightLeak
                EditType.DISTORTION -> selectedDistortion
                EditType.SCRATCH -> selectedScratch
                EditType.TEXT -> ""
                EditType.STICKER -> ""
            }

        val items =
            when (editType) {
                EditType.FILTER -> filters
                EditType.FRAME -> frames
                EditType.LIGHT_LEAK -> lightLeaks
                EditType.DISTORTION -> distortions
                EditType.SCRATCH -> scratches
                EditType.TEXT -> emptyList<String>()
                EditType.STICKER -> emptyList<String>()
            }

        fun onSelectItem(selection: Any) {
            val baseBitmap = originalBitmap ?: return // Early return if bitmap is null
            
            when (editType) {
                EditType.FILTER -> {
                    selectedFilter = selection as Preset
                    isProcessing = true
                    scope.launch {
                        // Apply all edits from original bitmap to ensure all edits are included
                        filteredBitmap = applyAllEdits(baseBitmap)
                        isProcessing = false
                    }
                }

                EditType.FRAME -> {
                    // Frame background is handled separately in FrameBackgroundSelector
                }

                EditType.LIGHT_LEAK -> {
                    selectedLightLeak = (selection as String)
                    isProcessing = true
                    scope.launch {
                        // Apply all edits from original bitmap to ensure all edits are included
                        filteredBitmap = applyAllEdits(baseBitmap)
                        isProcessing = false
                    }
                }

                EditType.DISTORTION -> {
                    selectedDistortion = (selection as String)
                    isProcessing = true
                    scope.launch {
                        // Apply all edits from original bitmap to ensure all edits are included
                        filteredBitmap = applyAllEdits(baseBitmap)
                        isProcessing = false
                    }
                }

                EditType.SCRATCH -> {
                    selectedScratch = (selection as String)
                    isProcessing = true
                    scope.launch {
                        // Apply all edits from original bitmap to ensure all edits are included
                        filteredBitmap = applyAllEdits(baseBitmap)
                        isProcessing = false
                    }
                }

                EditType.TEXT -> {
                    /* Handled in DECOR tab */
                }

                EditType.STICKER -> {
                    /* Handled in DECOR tab */
                }
            }
        }

        if (editType == EditType.FRAME) {
            FrameBackgroundSelector()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(160.dp)
            ) {
            items(items.size) {
                Row {
                    Box(
                        modifier =
                            Modifier
                                .background(
                                    shape = RoundedCornerShape(12.dp),
                                    color =
                                        if (selection == items[it])
                                            Color.Yellow.copy(alpha = 0.8f)
                                        else Color.Transparent
                                )
                                .padding(if (selection == items[it]) 4.dp else 0.dp)
                                .clickable { onSelectItem(items[it]) }
                    ) {
                        when (editType) {
                            EditType.FILTER ->
                                FilterItem(filter = items[it] as Preset, imagePath = imagePath)

                            EditType.FRAME -> {
                                // Frame background selector is shown separately
                            }

                            EditType.LIGHT_LEAK, EditType.DISTORTION, EditType.SCRATCH ->
                                Box(
                                    modifier =
                                        Modifier
                                            .width(96.dp)
                                            .height(120.dp)
                                            .background(
                                                color = Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                ) {
                                    Column {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(80.dp)
                                                    .background(
                                                        shape =
                                                            RoundedCornerShape(
                                                                8.dp
                                                            ),
                                                        color = Color.Black
                                                    )
                                        ) {
                                            AsyncImage(
                                                contentScale = ContentScale.Crop,
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .clip(
                                                            shape =
                                                                RoundedCornerShape(
                                                                    8.dp
                                                                )
                                                        ),
                                                model = imagePath,
                                                contentDescription = null,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = items[it] as String,
                                            fontSize =
                                                MaterialTheme.typography
                                                    .bodyLarge
                                                    .fontSize,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1
                                        )
                                    }
                                }

                            EditType.TEXT, EditType.STICKER -> Unit // Handled in DECOR tab
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
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
                // Apply background image - margin = 0 để nền là nền của ảnh, không tạo frame
                originalBitmap?.let { bitmap ->
                    isProcessing = true
                    scope.launch {
                        filteredBitmap = viewmodel.applyFrameBackground(
                            context,
                            bitmap,
                            null,
                            uri,
                            margin = 0f,
                            customText = if (textFieldValue.isNotBlank() && textFieldValue != defaultText) textFieldValue else null
                        )
                        isProcessing = false
                    }
                }
            },
            onDismiss = { showImagePicker = false }
        )
    }

    Scaffold(
        modifier = Modifier.background(color = Color.Black),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(end = 16.dp),
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        "Save",
                        color = Color.Black,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        modifier =
                            Modifier
                                .background(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    val baseBitmap = originalBitmap
                                    if (baseBitmap == null) {
                                        Toast.makeText(
                                            context,
                                            "No image to save",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@clickable
                                    }
                                    isProcessing = true
                                    CoroutineScope(Dispatchers.IO).launch {
                                        // Apply all edits from original to ensure everything is included
                                        val bitmapToSave = applyAllEdits(baseBitmap)
                                        
                                        // Apply frame background if selected
                                        val finalBitmap = if (selectedFrameTheme != null || selectedBackgroundImageUri != null) {
                                            viewmodel.applyFrameBackground(
                                                context,
                                                bitmapToSave,
                                                selectedFrameTheme,
                                                selectedBackgroundImageUri,
                                                margin = 0f,
                                                customText = if (textFieldValue.isNotBlank() && textFieldValue != defaultText) textFieldValue else null
                                            )
                                        } else {
                                            bitmapToSave
                                        }
                                        
                                        // Apply overlays if any
                                        val bitmapWithOverlays = if (viewmodel.overlays.isNotEmpty()) {
                                            viewmodel.applyOverlaysToBitmap(finalBitmap)
                                        } else {
                                            finalBitmap
                                        }
                                        
                                        val savedUri = viewmodel.saveBitmapToDevice(
                                            context,
                                            bitmapWithOverlays,
                                            imagePath
                                        )
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            if (savedUri != null) {
                                                Toast.makeText(
                                                    context,
                                                    "Successfully Saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                // Navigate to Result screen (Image Detail) with saved image
                                                try {
                                                    val route = AppRoutes.imageDetail(savedUri)
                                                    android.util.Log.d("EditImageScreen", "Navigating to: $route with savedUri: $savedUri")
                                                    // Pop current screen and navigate to result
                                                    navController.popBackStack()
                                                    navController.navigate(route)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("EditImageScreen", "Navigation error: ${e.message}", e)
                                                    Toast.makeText(
                                                        context,
                                                        "Saved but navigation failed: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed on Save",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.6f)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    model =
                        if (selectedInfo == ImageInfo.NONE) (filteredBitmap)
                        else temporaryBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedTab) {
                TabType.CUSTOMIZE -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.8f)) {
                        TabSelection(
                            tabs = photoEditFeatures.map { it.name },
                            selection = selectedFeature.name,
                            selectedColor = Color.Cyan,
                            onSelect = { selectedFeature = EditType.valueOf(it) }
                        )
                        StyledEdit(imagePath = imagePath, editType = selectedFeature)
                    }
                }

                TabType.BASIC ->
                    if (selectedInfo == ImageInfo.NONE) BasicEditGroup() else BasicEditDetail()

                TabType.DECOR -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.8f)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Sticker Button
                            GlassButton(
                                onClick = {
                                    val currentBitmap = filteredBitmap ?: return@GlassButton
                                    // Add a random sticker to center
                                    val sticker = viewmodel.stickers.random()
                                    viewmodel.addOverlay(
                                        com.example.cameraxapp.data.model.StickerOverlay(
                                            type =
                                                com.example.cameraxapp.data.model
                                                    .OverlayType.STICKER,
                                            content = sticker,
                                            offsetX = currentBitmap.width / 2f,
                                            offsetY = currentBitmap.height / 2f
                                        )
                                    )
                                    scope.launch {
                                        filteredBitmap =
                                            viewmodel.applyOverlaysToBitmap(currentBitmap)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                text = "Add Sticker"
                            )

                            // Text Button
                            GlassButton(
                                onClick = {
                                    val currentBitmap = filteredBitmap ?: return@GlassButton
                                    // Add sample text
                                    viewmodel.addOverlay(
                                        com.example.cameraxapp.data.model.StickerOverlay(
                                            type =
                                                com.example.cameraxapp.data.model
                                                    .OverlayType.TEXT,
                                            content = "Your Text",
                                            offsetX = currentBitmap.width / 2f,
                                            offsetY = currentBitmap.height / 2f
                                        )
                                    )
                                    scope.launch {
                                        filteredBitmap =
                                            viewmodel.applyOverlaysToBitmap(currentBitmap)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                text = "Add Text"
                            )
                        }

                        // Show available stickers
                        Text(
                            "Available Stickers:",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewmodel.stickers) { sticker ->
                                Box(
                                    modifier =
                                        Modifier
                                            .size(60.dp)
                                            .background(
                                                color =
                                                    Color.White.copy(
                                                        alpha = 0.1f
                                                    ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                val currentBitmap = filteredBitmap ?: return@clickable
                                                viewmodel.addOverlay(
                                                    com.example.cameraxapp.data
                                                        .model.StickerOverlay(
                                                            type =
                                                                com.example
                                                                    .cameraxapp
                                                                    .data
                                                                    .model
                                                                    .OverlayType
                                                                    .STICKER,
                                                            content = sticker,
                                                            offsetX =
                                                                currentBitmap
                                                                    .width /
                                                                        2f,
                                                            offsetY =
                                                                currentBitmap
                                                                    .height /
                                                                        2f
                                                        )
                                                )
                                                scope.launch {
                                                    filteredBitmap =
                                                        viewmodel
                                                            .applyOverlaysToBitmap(
                                                                currentBitmap
                                                            )
                                                }
                                            },
                                    contentAlignment = Alignment.Center
                                ) { Text(text = sticker, fontSize = 32.sp) }
                            }
                        }
                    }
                }

                TabType.FAVOURITE ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Favorites Coming Soon",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
            }
            TabSelection(
                tabs = tabs.map { it.name },
                selection = selectedTab.name,
                selectedColor = Color.Green,
                onSelect = { selectedTab = TabType.valueOf(it) }
            )
        }
    }
}

@Composable
fun EditSlider(
    value: Float,
    title: String,
    onValueChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(.8f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Cancel,
                modifier = Modifier.clickable { onCancel.invoke() },
                contentDescription = null,
                tint = Color.White
            )
            Icon(
                Icons.Default.Check,
                modifier = Modifier.clickable { onAccept.invoke() },
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(title, color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(32.dp))
        Slider(
            colors = SliderDefaults.colors(thumbColor = Color.Gray),
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(28.dp))
        Text(value.toString().format("%.1f", value), color = Color.White, fontSize = 28.sp)
    }
}

@Composable
fun FilterItem(filter: Preset, imagePath: String) {
    return Box(
        modifier =
            Modifier
                .width(96.dp)
                .height(120.dp)
                .background(color = Color.White, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .background(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black
                        )
            ) {
                AsyncImage(
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = RoundedCornerShape(8.dp)),
                    model = imagePath,
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                filter.name,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Black
            )
        }
    }
}
