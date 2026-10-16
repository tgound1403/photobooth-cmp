package com.example.photogether.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photogether.data.repository.PhotoBoothRepository
import com.example.photogether.shared.domain.model.PhotoBooth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(private val repository: PhotoBoothRepository) : ViewModel() {

    val photoBooths: StateFlow<List<PhotoBooth>> =
            repository
                    .getAllPhotoBooths()
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = emptyList()
                    )

    fun deletePhotoBooth(photoBooth: PhotoBooth) {
        viewModelScope.launch { repository.deletePhotoBooth(photoBooth) }
    }
}
