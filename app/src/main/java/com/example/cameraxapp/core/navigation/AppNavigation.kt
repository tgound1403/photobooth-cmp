package com.example.cameraxapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cameraxapp.core.render.CameraProfiles
import com.example.cameraxapp.ui.camera.CameraPickerScreen
import com.example.cameraxapp.ui.moments.MomentViewerScreen
import com.example.cameraxapp.ui.moments.MomentsScreen
import com.example.cameraxapp.ui.moments.MomentsViewModel
import com.example.cameraxapp.ui.view.CameraScreen.CameraScreen
import com.example.cameraxapp.ui.view.GaleryScreen.GalleryScreen
import com.example.cameraxapp.ui.view.NoPermissionGrantedScreen
import com.example.cameraxapp.ui.view.PhotoBoothEditScreen
import com.example.cameraxapp.ui.view.PhotoBoothResultScreen
import com.example.cameraxapp.ui.view.PhotoBoothScreen
import com.example.cameraxapp.ui.view.PhotoBoothSelectionScreen
import com.example.cameraxapp.ui.theme.InkTheme
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewmodel = koinViewModel<PhotoBoothViewModel>()

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) { 
            com.example.cameraxapp.ui.view.HomeScreen(navController) 
        }
        composable(AppRoutes.SETTINGS) { 
            com.example.cameraxapp.ui.view.SettingsScreen(navController) 
        }
        composable(AppRoutes.MAIN) { 
            InkTheme { CameraScreen(navController) } 
        }
        composable(AppRoutes.GALLERY) {
            val galleryViewModel =
                    koinViewModel<com.example.cameraxapp.ui.viewmodel.GalleryViewModel>()
            com.example.cameraxapp.ui.view.GalleryScreen(navController, galleryViewModel)
        }
        // Device photos (camera captures and imports) for editing. GALLERY is the PhotoBooth history.
        composable(AppRoutes.PHOTO_PICKER) {
            GalleryScreen(navController)
        }
        composable(AppRoutes.NO_PERMISSION_GRANTED) {
            NoPermissionGrantedScreen()
        }
        composable(AppRoutes.MOMENTS) {
            val momentsViewModel = koinViewModel<MomentsViewModel>()
            MomentsScreen(
                viewModel = momentsViewModel,
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate(AppRoutes.momentViewer(it)) },
            )
        }
        composable(
                route = AppRoutes.MOMENT_VIEWER,
                arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val startIndex = backStackEntry.arguments?.getInt("index") ?: 0
            // Share the grid's ViewModel when its entry is still on the stack, so both see the same list.
            val momentsEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry(AppRoutes.MOMENTS) }.getOrNull()
            }
            val momentsViewModel = koinViewModel<MomentsViewModel>(
                viewModelStoreOwner = momentsEntry ?: backStackEntry
            )
            MomentViewerScreen(
                viewModel = momentsViewModel,
                startIndex = startIndex,
                onBack = { navController.popBackStack() },
                onEdit = { uri -> navController.navigate(AppRoutes.imageDetail(uri)) },
            )
        }
        composable(AppRoutes.CAMERA_PICKER) {
            InkTheme {
                val momentsViewModel = koinViewModel<MomentsViewModel>()
                val moments by momentsViewModel.moments.collectAsState()
                val currentCameraId =
                    navController.previousBackStackEntry?.savedStateHandle?.get<String>(CameraProfiles.CURRENT_KEY)
                CameraPickerScreen(
                    currentCameraId = currentCameraId,
                    moments = moments,
                    onConfirm = { profile ->
                        // CameraScreen reads this on its own back-stack entry when the picker pops.
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(CameraProfiles.RESULT_KEY, profile.id)
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
            com.example.cameraxapp.ui.view.FrameSelectionScreen(navController, viewmodel)
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
            com.example.cameraxapp.ui.editor.EditorScreen(imagePath, navController)
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
    const val NO_PERMISSION_GRANTED = "noPermissionGranted"
    const val PHOTO_BOOTH = "photoBooth"
    const val PHOTO_BOOTH_SELECTION = "photoBoothSelection"
    const val FRAME_SELECTION = "frameSelection"
    const val PHOTO_BOOTH_RESULT = "photoBoothResult/{photoBoothId}"
    const val PHOTO_BOOTH_EDIT = "photoBoothEdit/{imagePaths}"
    const val IMAGE_DETAIL = "imageDetail/{imagePath}"
    const val MOMENTS = "moments"
    const val MOMENT_VIEWER = "moment_viewer/{index}"
    const val CAMERA_PICKER = "camera_picker"

    fun momentViewer(index: Int): String {
        return "moment_viewer/$index"
    }

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
