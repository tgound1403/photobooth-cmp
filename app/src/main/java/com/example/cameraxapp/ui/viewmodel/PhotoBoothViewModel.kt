package com.example.cameraxapp.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.core.render.FilterCatalog
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoBoothViewModel(
        private val repository: PhotoBoothRepository,
        private val application: Application,
        private val createPhotoBoothImageUseCase: CreatePhotoBoothImageUseCase,
        private val gifExportUseCase: com.example.cameraxapp.domain.usecase.GifExportUseCase
) : AndroidViewModel(application) {

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
            MutableStateFlow(com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.GRID_2X2)
    val selectedLayout: StateFlow<com.example.cameraxapp.shared.domain.model.PhotoBoothLayout> =
            _selectedLayout

    private val _selectedFilterId = MutableStateFlow(FilterCatalog.ORIGINAL_ID)
    val selectedFilterId: StateFlow<String> = _selectedFilterId

    private val _selectedTheme =
            MutableStateFlow(com.example.cameraxapp.data.model.FrameThemes.CLASSIC_WHITE)
    val selectedTheme: StateFlow<com.example.cameraxapp.data.model.FrameTheme> = _selectedTheme

    private val _customText = MutableStateFlow<String?>(null)
    val customText: StateFlow<String?> = _customText.asStateFlow()

    private val _backgroundImageUri = MutableStateFlow<Uri?>(null)
    val backgroundImageUri: StateFlow<Uri?> = _backgroundImageUri.asStateFlow()

    fun updateLayout(layout: com.example.cameraxapp.shared.domain.model.PhotoBoothLayout) {
        _selectedLayout.value = layout
        _requiredPhotoCount.value =
                when (layout) {
                    com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.SINGLE -> 1
                    com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.STRIP_1X2 -> 2
                    com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.STRIP_1X3 -> 3
                    com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.GRID_2X2,
                    com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.STRIP_1X4 -> 4
                }
    }

    private val _requiredPhotoCount = MutableStateFlow(4)
    val requiredPhotoCount: StateFlow<Int> = _requiredPhotoCount

    fun clearCapturedImages() {
        _capturedImages.value = emptyList()
        _selectedImages.value = emptySet()
        _backgroundImageUri.value = null
        _customText.value = null
    }

    fun updateFilter(filterId: String) {
        _selectedFilterId.value = filterId
    }

    fun updateTheme(theme: com.example.cameraxapp.data.model.FrameTheme) {
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
        } else if (currentSelected.size < 4) {
            currentSelected.add(imagePath)
        }
        _selectedImages.value = currentSelected
    }

    fun getPhotoBoothById(id: Long) {
        viewModelScope.launch { 
            _photoBooth.value = repository.getPhotoBoothById(id) 
        }
    }

    fun saveImages(context: Context, imagePaths: List<String>) {
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
                                        _selectedFilterId.value,
                                        backgroundSource,
                                        themeToUse,
                                        _customText.value
                                )
                                .getOrThrow()

                // Save to database
                val photoBooth =
                        com.example.cameraxapp.shared.domain.model.PhotoBooth(
                                imagePaths = listOf(resultPath),
                                createdAt = System.currentTimeMillis()
                        )
                val photoBoothId = repository.insertPhotoBooth(photoBooth)
                val savedPhotoBooth = repository.getPhotoBoothById(photoBoothId)
                _photoBooth.value = savedPhotoBooth

                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    suspend fun createPhotoBoothPreview(
        context: Context,
        imagePaths: List<String>,
        layout: com.example.cameraxapp.shared.domain.model.PhotoBoothLayout,
        backgroundSource: String?,
        frameTheme: com.example.cameraxapp.data.model.FrameTheme?,
        customText: String?
    ): Result<String> = runCatching {
        val themeToUse = if (backgroundSource != null) null else frameTheme
        createPhotoBoothImageUseCase
            .execute(
                context,
                imagePaths,
                layout,
                _selectedFilterId.value,
                backgroundSource,
                themeToUse,
                customText
            )
            .getOrThrow()
    }

    fun saveImageWithBg(context: Context, imagePaths: List<String>, background: String) {
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
                                        _selectedFilterId.value,
                                        background,
                                        null, // frameTheme
                                        _customText.value
                                )
                                .getOrThrow()

                // Save to database
                val photoBooth =
                        com.example.cameraxapp.shared.domain.model.PhotoBooth(
                                imagePaths = listOf(resultPath),
                                createdAt = System.currentTimeMillis()
                        )
                repository.insertPhotoBooth(photoBooth)

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
}
