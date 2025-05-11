package com.example.cameraxapp.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.data.model.PhotoBooth
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import com.example.cameraxapp.domain.usecase.SaveImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase

class PhotoBoothViewModel constructor(
    private val repository: PhotoBoothRepository,
    private val application: Application,
    private val saveImageUseCase: SaveImageUseCase,
    private val createPhotoBoothImageUseCase: CreatePhotoBoothImageUseCase
) : AndroidViewModel(application) {
    private val _capturedImages = MutableLiveData<List<String>>(emptyList())
    val capturedImages: LiveData<List<String>> = _capturedImages

    private val _selectedImages = MutableLiveData<Set<String>>(emptySet())
    val selectedImages: LiveData<Set<String>> = _selectedImages

    private val _photoBooth = MutableLiveData<PhotoBooth>()
    val photoBooth = _photoBooth

    private val _result = MutableLiveData<Long>()
    val result = _result

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

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

    suspend fun saveSelectedImages() {
        val selectedPaths = _selectedImages.value?.toList() ?: emptyList()
        if (selectedPaths.size == 4) {
            val photoBooth = PhotoBooth(imagePaths = selectedPaths, createdAt = Date())
            _result.value = repository.insertPhotoBooth(photoBooth)
            _capturedImages.value = emptyList()
            _selectedImages.value = emptySet()
        }
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
                createPhotoBoothImageUseCase.execute(context, imagePaths).getOrThrow()
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