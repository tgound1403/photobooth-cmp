package com.example.photogether.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.photogether.data.settings.CameraSettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.get
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.ui.camera.CameraPickerScreen
import com.example.photogether.ui.colors.ColorStoryScreen
import com.example.photogether.ui.colors.PhotoCreditsScreen
import com.example.photogether.core.moments.MomentDrawer
import com.example.photogether.ui.moments.MomentsScreen
import com.example.photogether.ui.moments.MomentsViewModel
import com.example.photogether.ui.view.CameraScreen.CameraScreen
import com.example.photogether.ui.view.GaleryScreen.GalleryScreen
import com.example.photogether.ui.view.LegalScreen
import com.example.photogether.ui.view.NoPermissionGrantedScreen
import com.example.photogether.ui.view.PhotoBoothEditScreen
import com.example.photogether.ui.view.PhotoBoothResultScreen
import com.example.photogether.ui.view.PhotoBoothScreen
import com.example.photogether.ui.view.PhotoBoothSelectionScreen
import com.example.photogether.ui.theme.InkTheme
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel
import com.tgound.photogether.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewmodel = koinViewModel<PhotoBoothViewModel>()

    // Camera and camera picker follow the sensor; everything else is portrait. Decided from the destination here,
    // not per screen: with two screens alive during a transition, a leaving screen's cleanup used to overwrite the
    // entering screen's setting and flip it to portrait for a moment.
    val context = androidx.compose.ui.platform.LocalContext.current
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    androidx.compose.runtime.LaunchedEffect(route) {
        if (route == null) return@LaunchedEffect
        val free = route == AppRoutes.MAIN || route == AppRoutes.CAMERA_PICKER
        (context as? android.app.Activity)?.requestedOrientation =
            if (free) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
        // One soft transition for every screen change: the new screen fades in with a small rise, the old one fades out.
        enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 24 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutVertically(tween(260)) { it / 24 } },
    ) {
        composable(AppRoutes.HOME) { 
            com.example.photogether.ui.view.HomeScreen(navController) 
        }
        composable(AppRoutes.NOTIFICATIONS) {
            com.example.photogether.ui.view.NotificationsScreen(
                onBack = { navController.popBackStackOnce() },
                onStoryClick = { navController.navigate(AppRoutes.colorStory(it.id)) },
            )
        }
        composable(AppRoutes.SETTINGS) { 
            com.example.photogether.ui.view.SettingsScreen(navController) 
        }
        composable(AppRoutes.MAIN) { 
            InkTheme { CameraScreen(navController) } 
        }
        composable(AppRoutes.GALLERY) {
            val galleryViewModel =
                    koinViewModel<com.example.photogether.ui.viewmodel.GalleryViewModel>()
            com.example.photogether.ui.view.GalleryScreen(navController, galleryViewModel)
        }
        // Device photos (camera captures and imports) for editing. GALLERY is the PhotoBooth history.
        composable(AppRoutes.PHOTO_PICKER) {
            GalleryScreen(navController)
        }
        // Photos taken on the camera screen, opened from its gallery button.
        composable(AppRoutes.CAMERA_GALLERY) {
            GalleryScreen(navController, cameraOnly = true)
        }
        // Replay of the how-to pages from Settings (the first run is handled by AppRoot).
        composable(AppRoutes.ONBOARDING) {
            com.example.photogether.ui.onboarding.OnboardingScreen(
                replay = true,
                onFinished = { navController.popBackStackOnce() },
            )
        }
        composable(AppRoutes.NO_PERMISSION_GRANTED) {
            NoPermissionGrantedScreen()
        }
        composable(
            route = AppRoutes.MOMENTS,
            arguments = listOf(navArgument("drawer") { type = NavType.StringType }),
        ) { backStackEntry ->
            val momentsViewModel = koinViewModel<MomentsViewModel>()
            val drawer = MomentDrawer.entries.firstOrNull { it.name == backStackEntry.arguments?.getString("drawer") }
                ?: MomentDrawer.SHOTS
            MomentsScreen(
                viewModel = momentsViewModel,
                drawer = drawer,
                onBack = { navController.popBackStackOnce() },
                onEdit = { uri -> navController.navigate(AppRoutes.imageDetail(uri)) },
            )
        }
        composable(
            route = AppRoutes.COLOR_STORY,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val story = ColorStories.byId(backStackEntry.arguments?.getString("storyId").orEmpty())
            if (story != null) {
                ColorStoryScreen(
                    story = story,
                    onBack = { navController.popBackStackOnce() },
                    onOpenCredits = { navController.navigate(AppRoutes.PHOTO_CREDITS) },
                    onTryNow = { navController.navigate(AppRoutes.MAIN) },
                )
            }
        }
        composable(AppRoutes.PHOTO_CREDITS) {
            PhotoCreditsScreen(onBack = { navController.popBackStackOnce() })
        }
        composable(AppRoutes.TERMS) {
            LegalScreen(
                titleRes = R.string.legal_terms_title,
                bodyRes = R.string.legal_terms_body,
                onBack = { navController.popBackStackOnce() },
            )
        }
        composable(AppRoutes.PRIVACY) {
            LegalScreen(
                titleRes = R.string.legal_privacy_title,
                bodyRes = R.string.legal_privacy_body,
                onBack = { navController.popBackStackOnce() },
            )
        }
        composable(AppRoutes.LICENSES) {
            LegalScreen(
                titleRes = R.string.licenses_title,
                bodyRes = R.string.licenses_body,
                onBack = { navController.popBackStackOnce() },
            )
        }
        composable(AppRoutes.CAMERA_PICKER) {
            InkTheme {
                val momentsViewModel = koinViewModel<MomentsViewModel>()
                val moments by momentsViewModel.moments.collectAsStateWithLifecycle()
                // The camera is stored in DataStore, so the picker shows the same choice from Home and from the camera.
                val cameraSettings = get<CameraSettingsRepository>()
                val currentCameraId by remember(cameraSettings) { cameraSettings.settings.map { it.cameraId } }
                    .collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                val tutorial = koinViewModel<com.example.photogether.ui.tutorial.TutorialViewModel>()
                CameraPickerScreen(
                    currentCameraId = currentCameraId,
                    moments = moments,
                    onConfirm = { profile ->
                        scope.launch { cameraSettings.setCameraId(profile.id) }
                        tutorial.advance(com.example.photogether.core.tutorial.TutorialStep.LOOK)
                        navController.popBackStackOnce()
                    },
                    onBack = { navController.popBackStackOnce() },
                )
            }
        }
        composable(AppRoutes.PHOTO_BOOTH) { 
            PhotoBoothScreen(navController, viewmodel) 
        }
        composable(AppRoutes.PHOTO_BOOTH_SELECTION) { 
            PhotoBoothSelectionScreen(navController, viewmodel) 
        }
        composable(AppRoutes.FRAME_SELECTION) {
            com.example.photogether.ui.view.FrameSelectionScreen(navController, viewmodel)
        }
        composable(
                route = AppRoutes.PHOTO_BOOTH_RESULT,
                arguments = listOf(navArgument("photoBoothId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoBoothId = backStackEntry.arguments?.getLong("photoBoothId") ?: 0L
            PhotoBoothResultScreen(photoBoothId, navController, viewmodel)
        }
        composable(
                route = AppRoutes.IMAGE_DETAIL,
                arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("imagePath") ?: ""
            // Decode URI if it was encoded
            val imagePath = try {
                android.net.Uri.decode(encodedPath)
            } catch (e: Exception) {
                encodedPath
            }
            com.example.photogether.ui.editor.EditorScreen(imagePath, navController)
        }
        composable(
                route = AppRoutes.PHOTO_BOOTH_EDIT,
                arguments = listOf(navArgument("imagePaths") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPaths = backStackEntry.arguments?.getString("imagePaths") ?: ""
            // Decode và parse danh sách ảnh (comma-separated hoặc JSON)
            val imagePaths = try {
                if (encodedPaths.contains(",")) {
                    encodedPaths.split(",").map { android.net.Uri.decode(it) }
                } else {
                    listOf(android.net.Uri.decode(encodedPaths))
                }
            } catch (e: Exception) {
                emptyList()
            }
            PhotoBoothEditScreen(imagePaths, navController, viewmodel)
        }
    }
}

object AppRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val MAIN = "main"
    const val GALLERY = "gallery"
    const val PHOTO_PICKER = "photo_picker"
    const val CAMERA_GALLERY = "camera_gallery"
    const val NO_PERMISSION_GRANTED = "noPermissionGranted"
    const val PHOTO_BOOTH = "photoBooth"
    const val PHOTO_BOOTH_SELECTION = "photoBoothSelection"
    const val FRAME_SELECTION = "frameSelection"
    const val PHOTO_BOOTH_RESULT = "photoBoothResult/{photoBoothId}"
    const val PHOTO_BOOTH_EDIT = "photoBoothEdit/{imagePaths}"
    const val IMAGE_DETAIL = "imageDetail/{imagePath}"
    const val MOMENTS = "moments/{drawer}"
    const val CAMERA_PICKER = "camera_picker"
    const val COLOR_STORY = "colorStory/{storyId}"
    const val PHOTO_CREDITS = "photoCredits"
    const val TERMS = "terms"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"
    const val ONBOARDING = "onboarding"

    fun moments(drawer: MomentDrawer): String = "moments/${drawer.name}"

    fun colorStory(storyId: String): String = "colorStory/$storyId"

    fun photoBoothResult(photoBoothId: Long): String {
        return "photoBoothResult/$photoBoothId"
    }
    
    fun photoBoothEdit(imagePaths: List<String>): String {
        // Encode danh sách ảnh thành comma-separated string
        val encoded = imagePaths.joinToString(",") { android.net.Uri.encode(it) }
        return "photoBoothEdit/$encoded"
    }
    
    fun imageDetail(imagePath: String): String {
        // Encode the URI path to handle special characters like : and /
        val encoded = android.net.Uri.encode(imagePath)
        return "imageDetail/$encoded"
    }
}
