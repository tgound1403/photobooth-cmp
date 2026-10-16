package com.example.cameraxapp.ui.moments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.core.moments.Moment
import com.example.cameraxapp.data.moments.DeleteResult
import com.example.cameraxapp.data.moments.MomentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class MomentsViewModel(private val repository: MomentsRepository) : ViewModel() {

    /** Bumped by [refresh] so the observer is subscribed again, for example after a permission grant. */
    private val reload = MutableStateFlow(0)

    val moments: StateFlow<List<Moment>> = reload
        .flatMapLatest { repository.observe() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refresh() {
        reload.value = reload.value + 1
    }

    suspend fun delete(uri: String): DeleteResult = repository.delete(uri)

    suspend fun deleteAfterConsent(uri: String): DeleteResult = repository.deleteAfterConsent(uri)
}
