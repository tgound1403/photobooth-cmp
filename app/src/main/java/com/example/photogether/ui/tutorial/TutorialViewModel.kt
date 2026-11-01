package com.example.photogether.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photogether.core.tutorial.TutorialStep
import com.example.photogether.data.prefs.TutorialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TutorialViewModel(private val repository: TutorialRepository) : ViewModel() {

    val step: StateFlow<TutorialStep?> = repository.step
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Starts the tour at its first step, after onboarding. */
    fun start() = set(TutorialStep.entries.first())

    /** Moves past [from] once the user has done it. Ignored when the tour is already on another step. */
    fun advance(from: TutorialStep) {
        viewModelScope.launch {
            if (repository.step.first() == from) repository.setStep(from.next)
        }
    }

    fun skip() = set(null)

    /** Restarts the tour from the first step, from Settings. */
    fun replay() = start()

    private fun set(step: TutorialStep?) {
        viewModelScope.launch { repository.setStep(step) }
    }
}
