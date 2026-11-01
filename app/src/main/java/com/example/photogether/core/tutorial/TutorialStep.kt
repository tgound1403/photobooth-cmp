package com.example.photogether.core.tutorial

/** The screen a tutorial step is shown on. */
enum class TutorialScreen { HOME, CAMERA }

/**
 * The guided tour after onboarding, in the order the user walks through it. [target] names the control
 * the spotlight covers; null means the card has no control to point at.
 */
enum class TutorialStep(val screen: TutorialScreen, val target: String?) {
    HOME_CAMERA(TutorialScreen.HOME, "home_camera"),
    CAPTURE(TutorialScreen.CAMERA, "shutter"),
    FIRST_SHOT(TutorialScreen.CAMERA, null),
    LOOK(TutorialScreen.CAMERA, "picker"),
    ADJUST(TutorialScreen.CAMERA, "quickbar"),
    OPEN_GALLERY(TutorialScreen.CAMERA, "aperture"),
    SHOOT_MORE(TutorialScreen.CAMERA, "shutter"),
    OPEN_CALENDAR_DAY(TutorialScreen.HOME, "calendar"),
    FINISH(TutorialScreen.HOME, null);

    /** Steps with nothing to tap: a reward card, a tip that times out, the closing card. */
    val isPassive: Boolean
        get() = this == FIRST_SHOT || this == ADJUST || this == FINISH

    /** Tips that point at a control but leave the screen usable and dismiss themselves. */
    val isTip: Boolean
        get() = this == ADJUST

    /** The step after this one, or null after [FINISH]. */
    val next: TutorialStep?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        /** How long a passive tip stays up before the tour moves on by itself. */
        const val TIP_MILLIS = 7000L

        fun fromName(name: String?): TutorialStep? = entries.firstOrNull { it.name == name }
    }
}
