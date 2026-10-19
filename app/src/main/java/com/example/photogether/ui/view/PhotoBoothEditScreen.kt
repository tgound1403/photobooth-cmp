package com.example.photogether.ui.view

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import org.koin.androidx.compose.get
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.photogether.ui.theme.AppIcons

private const val PREVIEW_DEBOUNCE_MS = 250L
private val PREVIEW_HEIGHT = 360.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothEditScreen(
    imagePaths: List<String>,
    navController: NavController,
    viewModel: PhotoBoothViewModel,
) {
    val context = LocalContext.current
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val selectedLayout by viewModel.selectedLayout.collectAsStateWithLifecycle()
    
    // Order of the strip. Starts as the pick order and changes only through the reorder row.
    var currentImagePaths by remember(imagePaths) { mutableStateOf(imagePaths) }

    // Frame background state
    // Start on the default theme so the highlighted chip matches the strip that gets rendered.
    var selectedFrameTheme by remember { mutableStateOf<FrameTheme?>(FrameThemes.CLASSIC_WHITE) }
    var selectedBackgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    
    // Text overlay state
    // The text starts as the current date, so it is printed unless the user clears it.
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val defaultText = remember { dateFormat.format(Date()) }
    var textFieldValue by remember { mutableStateOf(defaultText) }
    var customText by remember { mutableStateOf<String?>(defaultText) }
    
    // Filter choice made on the previous screen. Changing it must refresh the preview too.
    val editParams by viewModel.editParams.collectAsStateWithLifecycle()

    // Preview state. Kept in memory: previews are not written to the gallery.
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf<String?>(null) }

    // Regenerate the preview whenever anything that affects the strip changes. The effect's
    // coroutine is cancelled when a key changes, so only the latest render can land.
    LaunchedEffect(
        selectedFrameTheme,
        selectedBackgroundImageUri,
        textFieldValue,
        currentImagePaths,
        selectedLayout,
        editParams
    ) {
        if (currentImagePaths.isEmpty()) return@LaunchedEffect
        // Wait for typing to pause; a new key cancels this render before it starts.
        delay(PREVIEW_DEBOUNCE_MS)
        isGeneratingPreview = true
        val backgroundSource = selectedBackgroundImageUri?.toString()
        val themeToUse = if (backgroundSource != null) null else selectedFrameTheme
        val textToUse = textFieldValue.takeIf { it.isNotBlank() }

        viewModel.createPhotoBoothPreview(
            context,
            currentImagePaths,
            selectedLayout,
            backgroundSource,
            themeToUse,
            textToUse
        ).onSuccess { bitmap ->
            previewBitmap = bitmap
            previewError = null
        }.onFailure { e ->
            previewError = e.message ?: ""
        }
        isGeneratingPreview = false
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
    
    val isSaving = saveState is PhotoBoothViewModel.SaveState.Saving

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.booth_edit_title), color = InkColors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(AppIcons.Back, contentDescription = stringResource(R.string.action_back), tint = InkColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = InkColors.background,
                    titleContentColor = InkColors.textPrimary
                ),
            )
        },
        bottomBar = {
            // Save is the primary action, so it stays reachable at the bottom while the controls scroll.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InkColors.background)
                    .padding(16.dp)
            ) {
                GlassButton(
                    onClick = {
                        val backgroundSource = selectedBackgroundImageUri?.toString()
                        val themeToUse = if (backgroundSource != null) null else selectedFrameTheme
                        viewModel.updateCustomText(customText)
                        viewModel.updateBackgroundImage(selectedBackgroundImageUri)
                        if (themeToUse != null) {
                            viewModel.updateTheme(themeToUse)
                        }
                        viewModel.saveImages(context, currentImagePaths)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = if (isSaving) stringResource(R.string.booth_saving) else stringResource(R.string.booth_save),
                    enabled = !isSaving && previewBitmap != null
                )
            }
        },
        containerColor = InkColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(InkColors.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Preview Section. Fixed height so the controls below can scroll on small screens.
            Box(
                modifier = Modifier
                    .height(PREVIEW_HEIGHT)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = previewBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo booth preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (isGeneratingPreview) {
                        CircularProgressIndicator(color = InkColors.accent)
                    }
                } else if (previewError != null) {
                    Text(
                        stringResource(R.string.booth_preview_failed, previewError.orEmpty()),
                        color = InkColors.textPrimary
                    )
                } else if (isGeneratingPreview) {
                    CircularProgressIndicator(color = InkColors.accent)
                } else {
                    Text(stringResource(R.string.booth_generating_preview), color = InkColors.textPrimary)
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        PhotoOrderRow(
            paths = currentImagePaths,
            onMove = { from, to ->
                currentImagePaths = currentImagePaths.toMutableList().apply { add(to, removeAt(from)) }
            }
        )

        // Background selection
        GlassButton(
            onClick = { showImagePicker = true },
            modifier = Modifier.fillMaxWidth(),
            text = if (selectedBackgroundImageUri != null) stringResource(R.string.booth_change_background) else stringResource(R.string.booth_choose_background),
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
                        AppIcons.Close,
                        contentDescription = stringResource(R.string.booth_remove_background),
                        tint = InkColors.textPrimary
                    )
                }
            }
        }

        // Theme selection
        Text(
            stringResource(R.string.booth_or_theme),
            color = InkColors.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            items(FrameThemes.ALL_THEMES, key = { it.id }) { theme ->
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
                // A blank field prints no text; anything else is printed as typed.
                customText = newValue.takeIf { it.isNotBlank() }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { 
                Text(
                    stringResource(R.string.booth_edit_text_label),
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
 * Chosen photos in strip order. Each photo can move one place left or right; the strip, the
 * preview and the saved result all follow this order. The layout itself does not change.
 */
@Composable
private fun PhotoOrderRow(
    paths: List<String>,
    onMove: (from: Int, to: Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.booth_reorder_title),
            color = InkColors.textPrimary,
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paths.forEachIndexed { index, path ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(Stroke.border, InkColors.outline, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = path,
                            contentDescription = stringResource(R.string.booth_captured_image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .size(20.dp)
                                .background(InkColors.accent, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = InkColors.onAccent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        IconButton(
                            onClick = { onMove(index, index - 1) },
                            enabled = index > 0
                        ) {
                            Icon(
                                AppIcons.Back,
                                contentDescription = stringResource(R.string.booth_move_left),
                                tint = if (index > 0) InkColors.textPrimary else InkColors.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { onMove(index, index + 1) },
                            enabled = index < paths.lastIndex
                        ) {
                            Icon(
                                AppIcons.ChevronRight,
                                contentDescription = stringResource(R.string.booth_move_right),
                                tint = if (index < paths.lastIndex) InkColors.textPrimary else InkColors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Paywall for a photobooth save blocked by a premium filter. Same screen as the editor uses.
 * Closes itself once Premium turns on.
 */
@Composable
fun PhotoBoothPaywallOverlay(viewModel: PhotoBoothViewModel) {
    val showPaywall by viewModel.showPaywall.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val billing: BillingRepository = get()
    val prices by billing.prices.collectAsStateWithLifecycle()
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
