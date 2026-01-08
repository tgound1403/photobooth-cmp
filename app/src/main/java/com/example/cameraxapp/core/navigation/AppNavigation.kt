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
            val imagePath = backStackEntry.arguments?.getString("imagePath")
            EditImageScreen(imagePath ?: "", navController)
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
    const val IMAGE_DETAIL = "imageDetail/{imagePath}"
    
    fun photoBoothResult(photoBoothId: Long): String {
        return "photoBoothResult/$photoBoothId"
    }
    
    fun imageDetail(imagePath: String): String {
        return "imageDetail/$imagePath"
    }
}
