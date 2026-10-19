package com.example.photogether.core.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.photogether.data.settings.CameraSettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.get
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.ui.camera.CameraPickerScreen
import com.example.photogether.ui.colors.ColorStoryScreen
import com.example.photogether.ui.colors.PhotoCreditsScreen
import com.example.photogether.ui.moments.MomentsScreen
import com.example.photogether.ui.moments.MomentsViewModel
import com.example.photogether.ui.view.CameraScreen.CameraScreen
import com.example.photogether.ui.view.GaleryScreen.GalleryScreen
import com.example.photogether.ui.view.NoPermissionGrantedScreen
import com.example.photogether.ui.view.PhotoBoothEditScreen
import com.example.photogether.ui.view.PhotoBoothResultScreen
import com.example.photogether.ui.view.PhotoBoothScreen
import com.example.photogether.ui.view.PhotoBoothSelectionScreen
import com.example.photogether.ui.theme.InkTheme
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewmodel = koinViewModel<PhotoBoothViewModel>()

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) { 
            com.example.photogether.ui.view.HomeScreen(navController) 
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
                onFinished = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.NO_PERMISSION_GRANTED) {
            NoPermissionGrantedScreen()
        }
        composable(AppRoutes.MOMENTS) {
            val momentsViewModel = koinViewModel<MomentsViewModel>()
            MomentsScreen(
                viewModel = momentsViewModel,
                onBack = { navController.popBackStack() },
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
                    onBack = { navController.popBackStack() },
                    onOpenCredits = { navController.navigate(AppRoutes.PHOTO_CREDITS) },
                    onTryNow = { navController.navigate(AppRoutes.MAIN) },
                )
            }
        }
        composable(AppRoutes.PHOTO_CREDITS) {
            PhotoCreditsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.CAMERA_PICKER) {
            InkTheme {
                val momentsViewModel = koinViewModel<MomentsViewModel>()
                val moments by momentsViewModel.moments.collectAsStateWithLifecycle()
                // The camera is stored in DataStore, so the picker shows the same choice from Home and from the camera.
                val cameraSettings = get<CameraSettingsRepository>()
                val currentCameraId by cameraSettings.settings
                    .map { it.cameraId }
                    .collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                CameraPickerScreen(
                    currentCameraId = currentCameraId,
                    moments = moments,
                    onConfirm = { profile ->
                        scope.launch { cameraSettings.setCameraId(profile.id) }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
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
    const val MOMENTS = "moments"
    const val CAMERA_PICKER = "camera_picker"
    const val COLOR_STORY = "colorStory/{storyId}"
    const val PHOTO_CREDITS = "photoCredits"
    const val ONBOARDING = "onboarding"

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
