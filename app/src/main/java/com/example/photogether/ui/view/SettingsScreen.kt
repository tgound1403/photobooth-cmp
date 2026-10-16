package com.example.photogether.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.components.GlassBox
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing

@Composable
fun SettingsScreen(navController: NavController) {
    SettingsContent(onBack = { navController.navigateUp() })
}

/** Stateless settings body. The app uses a single dark theme, so the appearance row is informational. */
@Composable
fun SettingsContent(onBack: () -> Unit) {
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = "Cài Đặt") },
        containerColor = AppColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg)
        ) {
            Text(
                text = "Giao diện",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(bottom = Spacing.md)
            )

            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = Radius.lg
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = AppColors.accent,
                        modifier = Modifier.padding(end = Spacing.md)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chủ đề tối",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Giao diện tối cố định cho toàn bộ ứng dụng",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PhotogetherTheme {
        SettingsContent(onBack = {})
    }
}
