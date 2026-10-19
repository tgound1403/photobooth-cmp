package com.example.photogether.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photogether.core.analytics.Analytics
import com.example.photogether.core.analytics.AnalyticsEvent
import com.example.photogether.core.analytics.NoOpAnalytics
import com.example.photogether.core.render.EditParams
import com.example.photogether.core.render.EditPipeline
import com.example.photogether.core.render.EditSnapshot
import com.example.photogether.ui.editor.AndroidEditorImageIo
import com.example.photogether.ui.editor.EditorImageIo
import com.example.photogether.core.render.FilterCatalog
import com.example.photogether.core.render.FilterInfo
import com.example.photogether.core.render.Recipe
import com.example.photogether.core.render.RecipeFilters
import com.example.photogether.data.billing.requiresPaywall
import com.example.photogether.data.repository.PhotoBoothRepository
import com.example.photogether.data.repository.RecipeRepository
import com.example.photogether.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.photogether.shared.domain.model.PhotoBooth
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoBoothViewModel(
        private val repository: PhotoBoothRepository,
        private val application: Application,
        private val createPhotoBoothImageUseCase: CreatePhotoBoothImageUseCase,
        private val gifExportUseCase: com.example.photogether.domain.usecase.GifExportUseCase,
        private val recipeRepository: RecipeRepository,
        /** Premium entitlement. Saving a premium filter while this is false opens the paywall. */
        val isPremium: StateFlow<Boolean> = MutableStateFlow(false),
        private val analytics: Analytics = NoOpAnalytics,
        catalog: FilterCatalog,
        /** Decodes captured shots for the selection thumbnails. */
        private val imageIo: EditorImageIo = AndroidEditorImageIo(application),
) : AndroidViewModel(application) {

    // Own GL pipeline for the selection thumbnails, created on first use and closed in onCleared.
    private val thumbRendererLazy = lazy { EditPipeline(catalog) }
    private val thumbRenderer by thumbRendererLazy
    private val _filteredThumbs = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    /** Captured shot path -> copy rendered with the current look. Missing keys are still rendering. */
    val filteredThumbs: StateFlow<Map<String, Bitmap>> = _filteredThumbs.asStateFlow()
    private var thumbParams: EditParams? = null
    private var thumbJob: Job? = null

    private val _showPaywall = MutableStateFlow(false)
    /** True after a save was blocked by a premium filter. The paywall closes on purchase or dismiss. */
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    /** Free users can pick and preview premium filters; the paywall is shown instead of saving. */
    private fun blockedByPaywall(): Boolean {
        if (!requiresPaywall(_editParams.value.filterId, filters, isPremium.value)) return false
        _showPaywall.value = true
        analytics.log(AnalyticsEvent.PAYWALL_SHOWN, mapOf("trigger" to "save"))
        return true
    }

    private val _gifExportState = MutableStateFlow<GifExportState>(GifExportState.Idle)
    val gifExportState: StateFlow<GifExportState> = _gifExportState

    fun exportGif(context: Context, imagePaths: List<String>) {
        viewModelScope.launch {
            _gifExportState.value = GifExportState.Exporting
            gifExportUseCase
                    .execute(context, imagePaths)
                    .onSuccess { path ->
                        // Save GIF to database as well
                        val photoBooth =
                                PhotoBooth(
                                        imagePaths = listOf(path),
                                        createdAt = System.currentTimeMillis()
                                )
                        repository.insertPhotoBooth(photoBooth)
                        _gifExportState.value = GifExportState.Success(path)
                    }
                    .onFailure { e ->
                        _gifExportState.value = GifExportState.Error(e.message ?: "Lỗi xuất GIF")
                    }
        }
    }

    sealed class GifExportState {
        object Idle : GifExportState()
        object Exporting : GifExportState()
        data class Success(val path: String) : GifExportState()
        data class Error(val message: String) : GifExportState()
    }
    fun shareToInstagramStories(context: Context, imagePath: String) {
        val file = File(imagePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent =
                Intent("com.instagram.share.ADD_TO_STORY").apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra("source_application", context.packageName)
                }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Instagram not installed, fallback to general share
            val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh"))
        }
    }
    private val _capturedImages = MutableStateFlow<List<String>>(emptyList())
    val capturedImages: StateFlow<List<String>> = _capturedImages.asStateFlow()

    private val _selectedImages = MutableStateFlow<Set<String>>(emptySet())
    val selectedImages: StateFlow<Set<String>> = _selectedImages.asStateFlow()

    private val _photoBooth = MutableStateFlow<PhotoBooth?>(null)
    val photoBooth: StateFlow<PhotoBooth?> = _photoBooth.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    // New state for UI options
    private val _selectedLayout =
            MutableStateFlow(com.example.photogether.shared.domain.model.PhotoBoothLayout.GRID_2X2)
    val selectedLayout: StateFlow<com.example.photogether.shared.domain.model.PhotoBoothLayout> =
            _selectedLayout

    val filters = catalog.filters

    private val recipes: StateFlow<List<Recipe>> =
            recipeRepository
                    .observeRecipes()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Catalog filters followed by saved recipes, as one list for the selection strip. */
    val filterSources: StateFlow<List<FilterInfo>> =
            recipes
                    .map { list -> filters + list.map(RecipeFilters::toFilterInfo) }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), filters)

    private val _editParams = MutableStateFlow(EditParams())
    val editParams: StateFlow<EditParams> = _editParams.asStateFlow()

    /** Id of the source picked in the strip. Null until the user picks one. */
    private val _selectedSourceId = MutableStateFlow<String?>(null)
    val selectedSourceId: StateFlow<String> =
            combine(_selectedSourceId, _editParams) { source, params -> source ?: params.filterId }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "original")

    private val _selectedTheme =
            MutableStateFlow(com.example.photogether.data.model.FrameThemes.CLASSIC_WHITE)
    val selectedTheme: StateFlow<com.example.photogether.data.model.FrameTheme> = _selectedTheme

    private val _customText = MutableStateFlow<String?>(null)
    val customText: StateFlow<String?> = _customText.asStateFlow()

    private val _backgroundImageUri = MutableStateFlow<Uri?>(null)
    val backgroundImageUri: StateFlow<Uri?> = _backgroundImageUri.asStateFlow()

    fun updateLayout(layout: com.example.photogether.shared.domain.model.PhotoBoothLayout) {
        _selectedLayout.value = layout
        _requiredPhotoCount.value =
                when (layout) {
                    com.example.photogether.shared.domain.model.PhotoBoothLayout.SINGLE -> 1
                    com.example.photogether.shared.domain.model.PhotoBoothLayout.STRIP_1X2 -> 2
                    com.example.photogether.shared.domain.model.PhotoBoothLayout.STRIP_1X3 -> 3
                    com.example.photogether.shared.domain.model.PhotoBoothLayout.GRID_2X2,
                    com.example.photogether.shared.domain.model.PhotoBoothLayout.STRIP_1X4 -> 4
                }
    }

    /** How many photos the user keeps for the layout. Always less than [CAPTURE_COUNT]. */
    private val _requiredPhotoCount = MutableStateFlow(4)
    val requiredPhotoCount: StateFlow<Int> = _requiredPhotoCount

    /** Shots taken per session, whatever the layout. Must stay above every layout's keep count. */
    val captureCount: Int = CAPTURE_COUNT

    /** Starts a new booth session. Drops the shots, the last saved result and any finished save/GIF state. */
    fun clearCapturedImages() {
        thumbJob?.cancel()
        thumbParams = null
        _filteredThumbs.value = emptyMap()
        _capturedImages.value = emptyList()
        _selectedImages.value = emptySet()
        _backgroundImageUri.value = null
        _customText.value = null
        _photoBooth.value = null
        _saveState.value = SaveState.Idle
        _gifExportState.value = GifExportState.Idle
    }

    /**
     * Renders every captured shot with the current look so the selection grid shows the filter.
     * Call again when the look or the shots change; only missing thumbnails are rendered.
     */
    fun refreshFilteredThumbs() {
        thumbJob?.cancel()
        val params = _editParams.value
        if (params != thumbParams) {
            thumbParams = params
            _filteredThumbs.value = emptyMap()
        }
        val pending = _capturedImages.value.filter { it !in _filteredThumbs.value }
        if (pending.isEmpty()) return
        val density = application.resources.displayMetrics.density
        thumbJob = viewModelScope.launch {
            for (path in pending) {
                val source = withContext(Dispatchers.IO) { imageIo.decode(path, THUMB_SOURCE_SIDE) } ?: continue
                val thumb = try {
                    thumbRenderer.render(source, EditSnapshot(params = params), null, density, THUMB_MAX_SIDE)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    continue
                } finally {
                    source.recycle()
                }
                _filteredThumbs.update { it + (path to thumb) }
            }
        }
    }

    override fun onCleared() {
        thumbJob?.cancel()
        if (thumbRendererLazy.isInitialized()) thumbRenderer.close()
        super.onCleared()
    }

    fun updateFilter(filterId: String) {
        _editParams.value = _editParams.value.copy(filterId = filterId)
    }

    /**
     * Picks a strip entry. A recipe replaces the edit values with its own; a catalog filter only
     * changes filterId, as before.
     */
    fun selectFilterSource(sourceId: String) {
        val recipe = RecipeFilters.recipeFor(sourceId, recipes.value)
        if (recipe != null) {
            _editParams.value = recipe.params
        } else {
            updateFilter(sourceId)
        }
        _selectedSourceId.value = sourceId
        // Tell free users about the premium filter now, not on save. Preview stays allowed.
        if (requiresPaywall(_editParams.value.filterId, filters, isPremium.value)) {
            _showPaywall.value = true
            analytics.log(AnalyticsEvent.PAYWALL_SHOWN, mapOf("trigger" to "pick"))
        }
    }

    fun updateTheme(theme: com.example.photogether.data.model.FrameTheme) {
        _selectedTheme.value = theme
    }

    fun updateCustomText(text: String?) {
        _customText.value = text
    }

    fun updateBackgroundImage(uri: Uri?) {
        _backgroundImageUri.value = uri
    }

    fun addCapturedImage(imagePath: String) {
        val currentList = _capturedImages.value.toMutableList()
        if (currentList.size < 8) {
            currentList.add(imagePath)
            _capturedImages.value = currentList
        }
    }

    fun toggleImageSelection(imagePath: String) {
        val currentSelected = _selectedImages.value.toMutableSet()
        if (currentSelected.contains(imagePath)) {
            currentSelected.remove(imagePath)
        } else if (currentSelected.size < _requiredPhotoCount.value) {
            currentSelected.add(imagePath)
        }
        _selectedImages.value = currentSelected
    }

    fun getPhotoBoothById(id: Long) {
        // Clear first so the result screen never shows the previous booth while this one loads.
        _photoBooth.value = null
        viewModelScope.launch {
            _photoBooth.value = repository.getPhotoBoothById(id)
        }
    }

    fun saveImages(context: Context, imagePaths: List<String>) {
        if (blockedByPaywall()) return
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                // Use background image if available, otherwise use theme
                val backgroundSource = _backgroundImageUri.value?.toString()
                val themeToUse = if (backgroundSource != null) null else _selectedTheme.value
                
                // Use current selected layout, filter, theme or background
                val resultPath =
                        createPhotoBoothImageUseCase
                                .execute(
                                        context,
                                        imagePaths,
                                        _selectedLayout.value,
                                        _editParams.value,
                                        backgroundSource,
                                        themeToUse,
                                        _customText.value
                                )
                                .getOrThrow()

                // Save to database
                val photoBooth =
                        com.example.photogether.shared.domain.model.PhotoBooth(
                                imagePaths = listOf(resultPath),
                                createdAt = System.currentTimeMillis()
                        )
                val photoBoothId = repository.insertPhotoBooth(photoBooth, _editParams.value)
                val savedPhotoBooth = repository.getPhotoBoothById(photoBoothId)
                _photoBooth.value = savedPhotoBooth

                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Renders the current strip for the edit screen without saving it. Nothing is written to the
     * gallery; only [saveImages] / [saveImageWithBg] create the final image.
     */
    suspend fun createPhotoBoothPreview(
        context: Context,
        imagePaths: List<String>,
        layout: com.example.photogether.shared.domain.model.PhotoBoothLayout,
        backgroundSource: String?,
        frameTheme: com.example.photogether.data.model.FrameTheme?,
        customText: String?
    ): Result<Bitmap> = try {
        val themeToUse = if (backgroundSource != null) null else frameTheme
        Result.success(
            createPhotoBoothImageUseCase.compose(
                context,
                imagePaths,
                layout,
                _editParams.value,
                backgroundSource,
                themeToUse,
                customText
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun saveImageWithBg(context: Context, imagePaths: List<String>, background: String) {
        if (blockedByPaywall()) return
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                // Use current selected layout and filter
                val resultPath =
                        createPhotoBoothImageUseCase
                                .execute(
                                        context,
                                        imagePaths,
                                        _selectedLayout.value,
                                        _editParams.value,
                                        background,
                                        null, // frameTheme
                                        _customText.value
                                )
                                .getOrThrow()

                // Save to database
                val photoBooth =
                        com.example.photogether.shared.domain.model.PhotoBooth(
                                imagePaths = listOf(resultPath),
                                createdAt = System.currentTimeMillis()
                        )
                val photoBoothId = repository.insertPhotoBooth(photoBooth, _editParams.value)
                // The result screen shows this one now, not the previous strip.
                _photoBooth.value = repository.getPhotoBoothById(photoBoothId)

                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    companion object {
        private const val THUMB_SOURCE_SIDE = 640
        private const val THUMB_MAX_SIDE = 320

        /** Shots per session. Layouts keep 1–4 of them, so the user always has a choice. */
        const val CAPTURE_COUNT = 8
    }
}
