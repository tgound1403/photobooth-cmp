package com.example.cameraxapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.data.api.UnsplashPhoto
import com.example.cameraxapp.data.repository.UnsplashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackgroundSelectorViewModel : ViewModel() {
    private val repository = UnsplashRepository()

    private val _backgrounds = MutableStateFlow<List<UnsplashPhoto>>(emptyList())
    val backgrounds: StateFlow<List<UnsplashPhoto>> = _backgrounds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadRandomBackgrounds() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _backgrounds.value = repository.getRandomBackgrounds()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchBackgrounds(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _backgrounds.value = repository.searchBackgrounds(query)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}