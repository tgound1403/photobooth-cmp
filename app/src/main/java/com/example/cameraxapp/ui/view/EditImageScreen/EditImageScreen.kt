package com.example.cameraxapp.ui.view.EditImageScreen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
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
import com.example.cameraxapp.data.model.Preset
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.viewmodel.EditImageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

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

    // State Management Fix: Use remember { mutableStateOf } for bitmaps to trigger recomposition
    val originalBitmap by remember { mutableStateOf<Bitmap>(BitmapFactory.decodeFile(imagePath)) }
    var temporaryBitmap by remember { mutableStateOf(originalBitmap) }
    var filteredBitmap by remember { mutableStateOf(originalBitmap) }

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
        filteredBitmap = temporaryBitmap
        selectedInfo = ImageInfo.NONE
    }

    fun onCancelChangeInfo() {
        selectedInfo = ImageInfo.NONE
        temporaryBitmap = filteredBitmap
    }

    @Composable
    fun BasicEditDetail() {
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
                                        filteredBitmap,
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
                                        filteredBitmap,
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
                                        filteredBitmap,
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
                                        filteredBitmap,
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
                                        filteredBitmap,
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
                                        filteredBitmap,
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
                                    viewmodel.applyShadow(filteredBitmap, shadow)
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
                                    viewmodel.applyGrain(filteredBitmap, grain)
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
                                        filteredBitmap,
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
                                        filteredBitmap,
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
            when (editType) {
                EditType.FILTER -> {
                    selectedFilter = selection as Preset
                    filteredBitmap =
                        viewmodel.applyPresetFilter(originalBitmap, selectedFilter.matrix)
                }

                EditType.FRAME -> {
                    selectedFrame = (selection as String)
                    filteredBitmap = viewmodel.createPolaroidFrameWithDate(context, originalBitmap)
                }

                EditType.LIGHT_LEAK -> {
                    selectedLightLeak = (selection as String)
                    isProcessing = true
                    scope.launch {
                        filteredBitmap = viewmodel.applyLightLeak(originalBitmap, selectedLightLeak)
                        isProcessing = false
                    }
                }

                EditType.DISTORTION -> {
                    selectedDistortion = (selection as String)
                    isProcessing = true
                    scope.launch {
                        filteredBitmap =
                            viewmodel.applyDistortion(originalBitmap, selectedDistortion)
                        isProcessing = false
                    }
                }

                EditType.SCRATCH -> {
                    selectedScratch = (selection as String)
                    isProcessing = true
                    scope.launch {
                        filteredBitmap = viewmodel.applyScratch(originalBitmap, selectedScratch)
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

                            EditType.FRAME ->
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
                                            frames[0],
                                            fontSize =
                                                MaterialTheme.typography
                                                    .bodyLarge
                                                    .fontSize,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
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
                                    CoroutineScope(Dispatchers.IO).launch {
                                        viewmodel.saveBitmapToDevice(
                                            filteredBitmap,
                                            imagePath
                                        )
                                            .apply {
                                                val result = this
                                                withContext(
                                                    Dispatchers.Main
                                                ) {
                                                    if (result) {
                                                        Toast.makeText(
                                                            context,
                                                            "Successfully Saved",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed on Save",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                    }
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
                                    // Add a random sticker to center
                                    val sticker = viewmodel.stickers.random()
                                    viewmodel.addOverlay(
                                        com.example.cameraxapp.data.model.StickerOverlay(
                                            type =
                                                com.example.cameraxapp.data.model
                                                    .OverlayType.STICKER,
                                            content = sticker,
                                            offsetX = filteredBitmap.width / 2f,
                                            offsetY = filteredBitmap.height / 2f
                                        )
                                    )
                                    scope.launch {
                                        filteredBitmap =
                                            viewmodel.applyOverlaysToBitmap(filteredBitmap)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                text = "Add Sticker"
                            )

                            // Text Button
                            GlassButton(
                                onClick = {
                                    // Add sample text
                                    viewmodel.addOverlay(
                                        com.example.cameraxapp.data.model.StickerOverlay(
                                            type =
                                                com.example.cameraxapp.data.model
                                                    .OverlayType.TEXT,
                                            content = "Your Text",
                                            offsetX = filteredBitmap.width / 2f,
                                            offsetY = filteredBitmap.height / 2f
                                        )
                                    )
                                    scope.launch {
                                        filteredBitmap =
                                            viewmodel.applyOverlaysToBitmap(filteredBitmap)
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
                                                                filteredBitmap
                                                                    .width /
                                                                        2f,
                                                            offsetY =
                                                                filteredBitmap
                                                                    .height /
                                                                        2f
                                                        )
                                                )
                                                scope.launch {
                                                    filteredBitmap =
                                                        viewmodel
                                                            .applyOverlaysToBitmap(
                                                                filteredBitmap
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
