package com.example.photogether.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Pops the current screen only while it is fully shown. A second tap during the leave transition would otherwise pop
 * the screen below as well, and popping the last screen leaves a blank window.
 */
fun NavController.popBackStackOnce(): Boolean =
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack() else false

/** Navigates only while the current screen is fully shown, so a double tap does not open the target twice. */
fun NavController.navigateOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}
