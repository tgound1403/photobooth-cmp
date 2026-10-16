package com.example.photogether.ui.view

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import org.koin.androidx.compose.get
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.extension.findActivity
import com.example.photogether.data.billing.BillingRepository
import com.example.photogether.data.model.FrameTheme
import com.example.photogether.data.model.FrameThemes
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Stroke
import com.example.photogether.ui.view.DeviceImagePicker
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveState by viewModel.saveState.collectAsState()
    val selectedLayout by viewModel.selectedLayout.collectAsState()
    
    val currentImagePaths = imagePaths

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
                title = { Text("Chỉnh Sửa Photo Booth", color = InkColors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = InkColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = InkColors.background,
                    titleContentColor = InkColors.textPrimary
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
                            color = if (saveState is PhotoBoothViewModel.SaveState.Saving) InkColors.textSecondary else InkColors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        containerColor = InkColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(InkColors.background)
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
                    CircularProgressIndicator(color = InkColors.accent)
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
                    Text("Đang tạo preview...", color = InkColors.textPrimary)
                }
            }
            
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
            text = if (selectedBackgroundImageUri != null) "Đổi ảnh nền" else "Chọn ảnh nền",
            containerColor = InkColors.accent
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
                        .border(Stroke.border, InkColors.outline, RoundedCornerShape(8.dp))
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
                        tint = InkColors.textPrimary
                    )
                }
            }
        }

        // Theme selection
        Text(
            "Hoặc chọn theme màu",
            color = InkColors.textPrimary,
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
                                Modifier.border(Stroke.border, InkColors.textPrimary, RoundedCornerShape(8.dp))
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
                                    InkColors.accent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = InkColors.onAccent, fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (theme.backgroundColor == android.graphics.Color.WHITE ||
                            theme.backgroundColor == android.graphics.Color.parseColor("#F5E6D3")
                        ) InkColors.onAccent else InkColors.textPrimary,
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
                    color = InkColors.textPrimary.copy(alpha = 0.7f)
                ) 
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InkColors.textPrimary,
                unfocusedTextColor = InkColors.textPrimary.copy(alpha = 0.8f),
                focusedBorderColor = InkColors.highlight,
                unfocusedBorderColor = InkColors.outline,
                focusedLabelColor = InkColors.highlight,
                unfocusedLabelColor = InkColors.textPrimary.copy(alpha = 0.7f)
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true
        )
            }
        }
    }
    PhotoBoothPaywallOverlay(viewModel)
}

/**
 * Paywall for a photobooth save blocked by a premium filter. Same screen as the editor uses.
 * Closes itself once Premium turns on.
 */
@Composable
fun PhotoBoothPaywallOverlay(viewModel: PhotoBoothViewModel) {
    val showPaywall by viewModel.showPaywall.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val billing: BillingRepository = get()
    val prices by billing.prices.collectAsState()
    val activity = LocalContext.current.findActivity()
    LaunchedEffect(isPremium) { if (isPremium) viewModel.dismissPaywall() }
    if (showPaywall) {
        PaywallScreen(
            before = null,
            after = null,
            monthlyPrice = prices[BillingRepository.MONTHLY_PRODUCT_ID],
            yearlyPrice = prices[BillingRepository.YEARLY_PRODUCT_ID],
            onPurchase = { productId -> activity?.let { billing.launchPurchase(it, productId) } },
            onDismiss = viewModel::dismissPaywall,
        )
    }
}
