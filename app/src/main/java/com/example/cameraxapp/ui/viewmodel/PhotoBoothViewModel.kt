package com.example.cameraxapp.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.cameraxapp.domain.usecase.SaveImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Date

class PhotoBoothViewModel(
    private val repository: PhotoBoothRepository,
    private val application: Application,
    private val createPhotoBoothImageUseCase: CreatePhotoBoothImageUseCase
) : AndroidViewModel(application) {
    private val _capturedImages = MutableLiveData<List<String>>(emptyList())
    val capturedImages: LiveData<List<String>> = _capturedImages

    private val _selectedImages = MutableLiveData<Set<String>>(emptySet())
    val selectedImages: LiveData<Set<String>> = _selectedImages

    private val _photoBooth = MutableLiveData<PhotoBooth>()
    val photoBooth = _photoBooth

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    // New state for UI options
    private val _selectedLayout = MutableStateFlow(com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.GRID_2X2)
    val selectedLayout: StateFlow<com.example.cameraxapp.shared.domain.model.PhotoBoothLayout> = _selectedLayout

    private val _selectedFilter = MutableStateFlow(com.example.cameraxapp.shared.domain.model.ImageFilter.ORIGINAL)
    val selectedFilter: StateFlow<com.example.cameraxapp.shared.domain.model.ImageFilter> = _selectedFilter

    fun updateLayout(layout: com.example.cameraxapp.shared.domain.model.PhotoBoothLayout) {
        _selectedLayout.value = layout
    }

    fun updateFilter(filter: com.example.cameraxapp.shared.domain.model.ImageFilter) {
        _selectedFilter.value = filter
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
        viewModelScope.launch {
            _photoBooth.value = repository.getPhotoBoothById(id)
        }
    }

    fun saveImages(context: Context, imagePaths: List<String>) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                // Use current selected layout and filter
                createPhotoBoothImageUseCase.execute(
                    context, 
                    imagePaths, 
                    _selectedLayout.value, 
                    _selectedFilter.value
                ).getOrThrow()
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
                createPhotoBoothImageUseCase.execute(
                    context, 
                    imagePaths, 
                    _selectedLayout.value, 
                    _selectedFilter.value,
                    background
                ).getOrThrow()
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
