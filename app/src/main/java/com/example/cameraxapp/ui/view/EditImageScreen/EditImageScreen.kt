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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
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
import com.example.cameraxapp.core.enum.EditType
import com.example.cameraxapp.core.enum.ImageInfo
import com.example.cameraxapp.core.enum.TabType
import com.example.cameraxapp.data.model.Preset
import com.example.cameraxapp.viewmodel.EditImageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val viewmodel = EditImageViewModel()

val photoEditFeatures = viewmodel.photoEditFeatures
val filters = viewmodel.filters
val tabs = viewmodel.tabs
val frames = viewmodel.frames
val lightLeaks = viewmodel.lightLeaks
val distortions = viewmodel.distortions
val scratches = viewmodel.scratches

val job = Job()
val coroutineScope = CoroutineScope(Dispatchers.IO + job)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImageScreen(imagePath: String, navController: NavController) {
    val context = LocalContext.current

    val originalBitmap by remember { mutableStateOf<Bitmap>(BitmapFactory.decodeFile(imagePath)) }
    var temporaryBitmap: Bitmap = originalBitmap
    var filteredBitmap: Bitmap = originalBitmap

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
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(8.dp),
                                color = if (tabs[it] == selection) selectedColor.copy(alpha = 0.5f) else Color.Transparent
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
                    modifier = Modifier
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
                        Icons.Default.AcUnit,
                        modifier = Modifier.size(48.dp),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        it.name,
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
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
            ImageInfo.BRIGHTNESS -> EditSlider(
                value = brightness,
                title = selectedInfo.name,
                onValueChange = { change ->
                    brightness = change
                    coroutineScope.launch {
                        temporaryBitmap = viewmodel.applyBrightness(filteredBitmap, brightness)
                    }
                },
                range = -1f..1f,
                onCancel = { onCancelChangeInfo() },
                onAccept = { onAcceptChangeInfo() }
            )

            ImageInfo.CONTRAST -> EditSlider(
                value = contrast,
                title = selectedInfo.name,
                onValueChange = { change ->
                    contrast = change
                    coroutineScope.launch {
                        temporaryBitmap = viewmodel.applyContrast(filteredBitmap, contrast)
                    }
                },
                range = -1f..1f,
                onCancel = { onCancelChangeInfo() },
                onAccept = { onAcceptChangeInfo() }
            )

            ImageInfo.SATURATION -> EditSlider(
                value = saturation,
                title = selectedInfo.name,
                onValueChange = { change ->
                    saturation = change
                    coroutineScope.launch {
                        temporaryBitmap = viewmodel.applySaturation(filteredBitmap, saturation)
                    }
                },
                range = -1f..1f,
                onCancel = { onCancelChangeInfo() },
                onAccept = { onAcceptChangeInfo() }
            )

            ImageInfo.TEMPERATURE -> EditSlider(
                value = temperature,
                title = selectedInfo.name,
                onValueChange = { change ->
                    temperature = change
                    coroutineScope.launch {
                        temporaryBitmap = viewmodel.applyTemperature(filteredBitmap, temperature)
                    }
                },
                range = -3f..3f,
                onCancel = { onCancelChangeInfo() },
                onAccept = { onAcceptChangeInfo() }
            )

            ImageInfo.SHARPNESS -> TODO()
            ImageInfo.HIGHLIGHT -> TODO()
            ImageInfo.SHADOW -> TODO()
            ImageInfo.NONE -> Unit
        }
    }

    @Composable
    fun StyledEdit(imagePath: String, editType: EditType) {
        val selection = when (editType) {
            EditType.FILTER -> selectedFilter
            EditType.FRAME -> selectedFrame
            EditType.LIGHT_LEAK -> selectedLightLeak
            EditType.DISTORTION -> selectedDistortion
            EditType.SCRATCH -> selectedScratch
        }

        val items = when (editType) {
            EditType.FILTER -> filters
            EditType.FRAME -> frames
            EditType.LIGHT_LEAK -> lightLeaks
            EditType.DISTORTION -> distortions
            EditType.SCRATCH -> scratches
        }

        fun onSelectItem(selection: Any) {
            when (editType) {
                EditType.FILTER -> {
                    selectedFilter = selection as Preset
                    filteredBitmap = viewmodel.applyPresetFilter(originalBitmap, selectedFilter.matrix)
                }

                EditType.FRAME -> {
                    selectedFrame = (selection as String)
                    filteredBitmap = viewmodel.createPolaroidFrameWithDate(context, originalBitmap)
                }
                EditType.LIGHT_LEAK -> selectedLightLeak = (selection as String)
                EditType.DISTORTION -> selectedDistortion = (selection as String)
                EditType.SCRATCH -> selectedScratch = (selection as String)
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
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selection == items[it]) Color.Yellow.copy(alpha = 0.8f) else Color.Transparent
                            )
                            .padding(if (selection == items[it]) 4.dp else 0.dp)
                            .clickable {
                                onSelectItem(items[it])
                            }) {
                        when (editType) {
                            EditType.FILTER -> FilterItem(
                                filter = items[it] as Preset,
                                imagePath = imagePath
                            )

                            EditType.FRAME -> Box(
                                modifier = Modifier
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
                                        modifier = Modifier
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
                                        frames[0],
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            EditType.LIGHT_LEAK -> TODO()
                            EditType.DISTORTION -> TODO()
                            EditType.SCRATCH -> TODO()
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
                colors = TopAppBarDefaults.topAppBarColors(
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
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(8.dp), color = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                coroutineScope.launch {
                                    viewmodel
                                        .saveBitmapToDevice(
                                            filteredBitmap, imagePath
                                        )
                                        .apply {
                                            val result = this
                                            withContext(Dispatchers.Main) {
                                                if (result) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Successfully Saved",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                } else {
                                                    Toast
                                                        .makeText(
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
                    model = if (selectedInfo == ImageInfo.NONE) (filteredBitmap) else temporaryBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )

            }
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedTab) {
                TabType.CUSTOMIZE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(.8f)
                    ) {
                        TabSelection(
                            tabs = photoEditFeatures.map { it.name },
                            selection = selectedFeature.name,
                            selectedColor = Color.Cyan,
                            onSelect = { selectedFeature = EditType.valueOf(it) }
                        )
                        StyledEdit(imagePath = imagePath, editType = selectedFeature)
                    }
                }

                TabType.BASIC -> if (selectedInfo == ImageInfo.NONE) BasicEditGroup() else BasicEditDetail()
                TabType.FAVOURITE -> TODO()
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
                .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Cancel, modifier = Modifier.clickable {
                onCancel.invoke()
            }, contentDescription = null, tint = Color.White)
            Icon(Icons.Default.Check, modifier = Modifier.clickable {
                onAccept.invoke()
            }, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(32.dp))
        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Color.Gray
            ),
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
        modifier = Modifier
            .width(96.dp)
            .height(120.dp)
            .background(color = Color.White, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(shape = RoundedCornerShape(8.dp), color = Color.Black)
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