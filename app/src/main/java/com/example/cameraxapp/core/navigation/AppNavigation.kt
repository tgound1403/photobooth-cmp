package com.example.cameraxapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cameraxapp.ui.view.CameraScreen.CameraScreen
import com.example.cameraxapp.ui.view.EditImageScreen.EditImageScreen
import com.example.cameraxapp.ui.view.GaleryScreen.GalleryScreen
import com.example.cameraxapp.ui.view.NoPermissionGrantedScreen
import com.example.cameraxapp.ui.view.PhotoBoothEditScreen
import com.example.cameraxapp.ui.view.PhotoBoothResultScreen
import com.example.cameraxapp.ui.view.PhotoBoothScreen
import com.example.cameraxapp.ui.view.PhotoBoothSelectionScreen
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
            CameraScreen(navController) 
        }
        composable(AppRoutes.GALLERY) {
            val galleryViewModel =
                    koinViewModel<com.example.cameraxapp.ui.viewmodel.GalleryViewModel>()
            com.example.cameraxapp.ui.view.GalleryScreen(navController, galleryViewModel)
        }
        composable(AppRoutes.NO_PERMISSION_GRANTED) { 
            NoPermissionGrantedScreen() 
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
            EditImageScreen(imagePath, navController)
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
    const val NO_PERMISSION_GRANTED = "noPermissionGranted"
    const val PHOTO_BOOTH = "photoBooth"
    const val PHOTO_BOOTH_SELECTION = "photoBoothSelection"
    const val FRAME_SELECTION = "frameSelection"
    const val PHOTO_BOOTH_RESULT = "photoBoothResult/{photoBoothId}"
    const val PHOTO_BOOTH_EDIT = "photoBoothEdit/{imagePaths}"
    const val IMAGE_DETAIL = "imageDetail/{imagePath}"
    
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
