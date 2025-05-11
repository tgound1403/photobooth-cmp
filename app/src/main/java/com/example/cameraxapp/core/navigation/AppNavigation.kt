package com.example.cameraxapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cameraxapp.ui.view.CameraScreen.CameraScreen
import com.example.cameraxapp.ui.view.GaleryScreen.GalleryScreen
import com.example.cameraxapp.ui.view.EditImageScreen.EditImageScreen
import com.example.cameraxapp.ui.view.PhotoBoothScreen
import com.example.cameraxapp.ui.view.PhotoBoothResultScreen
import com.example.cameraxapp.ui.view.PhotoBoothSelectionScreen
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewmodel = koinViewModel<PhotoBoothViewModel>()

    NavHost(navController = navController, startDestination = "photoBooth") {
        composable("main") {
            CameraScreen(navController)
        }
        composable("gallery") {
            GalleryScreen(navController)
        }
        composable("photoBooth") {
            PhotoBoothScreen(navController, viewmodel)
        }
        composable(
            route = "photoBoothSelection",
        ) {
            PhotoBoothSelectionScreen(navController, viewmodel)
        }
        composable(
            route = "photoBoothResult/{photoBoothId}",
            arguments = listOf(navArgument("photoBoothId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoBoothId = backStackEntry.arguments?.getLong("photoBoothId") ?: 0L
            PhotoBoothResultScreen(photoBoothId, navController, viewmodel)
        }
        composable(
            route = "imageDetail/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath")
            EditImageScreen(imagePath ?: "", navController)
        }
    }
}