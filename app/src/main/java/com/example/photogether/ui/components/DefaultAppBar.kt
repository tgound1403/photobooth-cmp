package com.example.photogether.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.PhotogetherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppBar(navController: NavController, title: String) {
    DefaultAppBar(onBack = { navController.navigateUp() }, title = title)
}

/** App bar with a back button. Use this overload where no NavController is available (previews). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppBar(onBack: () -> Unit, title: String) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(IconSize.xl)
                    .clip(CircleShape)
                    .background(color = AppColors.surfaceElevated)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppColors.textPrimary)
            }
        },
        actions = {
            // Mirrors the navigation icon so the title stays centred.
            Row(modifier = Modifier.size(IconSize.xl)) {}
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.background,
            titleContentColor = AppColors.textPrimary,
            navigationIconContentColor = AppColors.textPrimary,
            actionIconContentColor = AppColors.textPrimary
        )
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun DefaultAppBarPreview() {
    PhotogetherTheme {
        DefaultAppBar(onBack = {}, title = "Chọn Khung Ảnh")
    }
}
