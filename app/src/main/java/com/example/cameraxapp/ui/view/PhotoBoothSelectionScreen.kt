package com.example.cameraxapp.ui.view

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBoothSelectionScreen(
    navController: NavController,
    viewModel: PhotoBoothViewModel,
) {
    val selectedImages by viewModel.selectedImages.observeAsState()
    val capturedImages by viewModel.capturedImages.observeAsState()
    val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Photos") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.Main) {
                                viewModel.saveSelectedImages()
                                val id = viewModel.result.value
                                Log.d("SelectionScreen", "result id: $id")
                                if (id != null) {
                                    navController.navigate("photoBoothResult/$id")
                                }
                            }
                        },
                        enabled = selectedImages?.size == 4
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(capturedImages ?: emptyList()) { imagePath ->
                Box(
                    modifier = Modifier
                        .aspectRatio(4f / 3f)
                        .border(
                            width = 2.dp,
                            color = if (selectedImages?.contains(imagePath) == true)
                                Color.Blue else Color.White
                        )
                        .clickable { viewModel.toggleImageSelection(imagePath) }
                ) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = "Captured image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}