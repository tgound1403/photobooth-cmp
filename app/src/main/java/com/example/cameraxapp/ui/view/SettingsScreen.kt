package com.example.cameraxapp.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.theme.AppTheme
import com.example.cameraxapp.ui.theme.NeonPurple
import com.example.cameraxapp.ui.theme.PastelPink
import com.example.cameraxapp.ui.viewmodel.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = koinViewModel()
) {
    val currentTheme by themeViewModel.theme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài Đặt", color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Giao diện",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ThemeOption(
                        theme = AppTheme.DARK_NEON,
                        currentTheme = currentTheme,
                        label = "Dark Neon (Premium)",
                        colorPreview = NeonPurple,
                        onSelect = { themeViewModel.setTheme(AppTheme.DARK_NEON) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ThemeOption(
                        theme = AppTheme.KOREAN_PASTEL,
                        currentTheme = currentTheme,
                        label = "Korean Pastel (Cute)",
                        colorPreview = PastelPink,
                        onSelect = { themeViewModel.setTheme(AppTheme.KOREAN_PASTEL) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ThemeOption(
                        theme = AppTheme.BLACK_AND_WHITE,
                        currentTheme = currentTheme,
                        label = "Black & White (Minimal)",
                        colorPreview = Color.White,
                        onSelect = { themeViewModel.setTheme(AppTheme.BLACK_AND_WHITE) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOption(
    theme: AppTheme,
    currentTheme: AppTheme,
    label: String,
    colorPreview: Color,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorPreview)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = theme == currentTheme,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
