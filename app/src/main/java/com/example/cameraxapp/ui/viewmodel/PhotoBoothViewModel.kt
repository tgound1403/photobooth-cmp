package com.example.cameraxapp.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _capturedImages = MutableLiveData<List<String>>(emptyList())
    val capturedImages: LiveData<List<String>> = _capturedImages

    private val _selectedImages = MutableLiveData<Set<String>>(emptySet())
    val selectedImages: LiveData<Set<String>> = _selectedImages

    private val _photoBooth = MutableLiveData<PhotoBooth>()
    val photoBooth = _photoBooth

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    // New state for UI options
    private val _selectedLayout =
            MutableStateFlow(com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.GRID_2X2)
    val selectedLayout: StateFlow<com.example.cameraxapp.shared.domain.model.PhotoBoothLayout> =
            _selectedLayout

    private val _selectedFilter =
            MutableStateFlow(com.example.cameraxapp.shared.domain.model.ImageFilter.ORIGINAL)
    val selectedFilter: StateFlow<com.example.cameraxapp.shared.domain.model.ImageFilter> =
            _selectedFilter

    private val _selectedTheme =
            MutableStateFlow(com.example.cameraxapp.data.model.FrameThemes.CLASSIC_WHITE)
    val selectedTheme: StateFlow<com.example.cameraxapp.data.model.FrameTheme> = _selectedTheme

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
    }

    fun updateFilter(filter: com.example.cameraxapp.shared.domain.model.ImageFilter) {
        _selectedFilter.value = filter
    }

    fun updateTheme(theme: com.example.cameraxapp.data.model.FrameTheme) {
        _selectedTheme.value = theme
    }

    fun addCapturedImage(imagePath: String) {
        val currentList = _capturedImages.value?.toMutableList() ?: mutableListOf()
        if (currentList.size < 8) {
            currentList.add(imagePath)
            _capturedImages.value = currentList
        }
    }

    fun toggleImageSelection(imagePath: String) {
        val currentSelected = _selectedImages.value?.toMutableSet() ?: mutableSetOf()
        if (currentSelected.contains(imagePath)) {
            currentSelected.remove(imagePath)
        } else if (currentSelected.size < 4) {
            currentSelected.add(imagePath)
        }
        _selectedImages.value = currentSelected
    }

    fun getPhotoBoothById(id: Long) {
        viewModelScope.launch { _photoBooth.value = repository.getPhotoBoothById(id) }
    }

    fun saveImages(context: Context, imagePaths: List<String>) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                // Use current selected layout, filter, and theme
                val resultPath =
                        createPhotoBoothImageUseCase
                                .execute(
                                        context,
                                        imagePaths,
                                        _selectedLayout.value,
                                        _selectedFilter.value,
                                        null, // backgroundSource
                                        _selectedTheme.value
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
                                        _selectedFilter.value,
                                        background
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
