package com.example.photogether.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photogether.data.prefs.OnboardingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the current step. Lives in a ViewModel so a language change, which recreates the
 * activity, does not send the user back to the first screen.
 */
class OnboardingViewModel(private val status: OnboardingStatus) : ViewModel() {

    private val _step = MutableStateFlow<OnboardingStep?>(null)
    val step: StateFlow<OnboardingStep?> = _step.asStateFlow()

    private var replay = false

    /** Starts the flow once; later calls (recomposition, recreation) are ignored. */
    fun begin(replay: Boolean) {
        if (_step.value != null) return
        this.replay = replay
        _step.value = OnboardingFlow.start(replay)
    }

    fun next(onFinished: () -> Unit) = go(OnboardingFlow.next(requireStep(), replay), onFinished)

    fun skip(onFinished: () -> Unit) = go(OnboardingFlow.skip(replay), onFinished)

    private fun requireStep(): OnboardingStep = _step.value ?: OnboardingFlow.start(replay)

    private fun go(target: OnboardingStep?, onFinished: () -> Unit) {
        if (target != null) {
            _step.value = target
            return
        }
        viewModelScope.launch {
            if (!replay) status.markCompleted()
            onFinished()
        }
    }
}
