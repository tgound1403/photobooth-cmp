package com.example.photogether.ui.onboarding

/** Top-level stages of the first-run flow. */
enum class OnboardingStep { LANGUAGE, INTRO, PERMISSION }

/** Number of animated how-to pages inside [OnboardingStep.INTRO]. */
const val INTRO_PAGE_COUNT = 4

/** Pure navigation rules, kept apart from Compose so they can be unit tested. */
object OnboardingFlow {
    /** The first step. A replay from Settings starts at the how-to pages. */
    fun start(replay: Boolean): OnboardingStep =
        if (replay) OnboardingStep.INTRO else OnboardingStep.LANGUAGE

    /** The step after [step], or null when the flow is over. */
    fun next(step: OnboardingStep, replay: Boolean): OnboardingStep? = when (step) {
        OnboardingStep.LANGUAGE -> OnboardingStep.INTRO
        OnboardingStep.INTRO -> if (replay) null else OnboardingStep.PERMISSION
        OnboardingStep.PERMISSION -> null
    }

    /** "Skip" jumps over the how-to pages, to the permission step or out of a replay. */
    fun skip(replay: Boolean): OnboardingStep? = if (replay) null else OnboardingStep.PERMISSION

    /** Page to show after a forward press, or null when the last page was already showing. */
    fun nextPage(page: Int): Int? = if (page < INTRO_PAGE_COUNT - 1) page + 1 else null
}
