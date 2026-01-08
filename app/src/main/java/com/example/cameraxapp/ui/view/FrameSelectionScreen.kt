package com.example.cameraxapp.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.shared.domain.model.PhotoBoothLayout
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.theme.DeepBlack
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.theme.NeonPurple
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameSelectionScreen(navController: NavController, viewModel: PhotoBoothViewModel) {
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Chọn Khung Ảnh", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                )
                )
            },
            containerColor = DeepBlack
    ) { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(DeepBlack, Color(0xFF1a0b2e))
                                                )
                                )
                                .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    "Chọn kiểu khung bạn muốn chụp",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
            ) {
                item {
                    FrameOption(
                            label = "1 Ảnh (Single)",
                            description = "Chụp 1 ảnh duy nhất",
                            color = NeonCyan,
                            onClick = {
                                viewModel.updateLayout(PhotoBoothLayout.SINGLE)
                                viewModel.clearCapturedImages()
                                navController.navigate(AppRoutes.PHOTO_BOOTH)
                            }
                    )
                }
                item {
                    FrameOption(
                            label = "2 Ảnh (Film Strip)",
                            description = "Chụp 2 ảnh - Dọc",
                            color = NeonPurple,
                            onClick = {
                                viewModel.updateLayout(PhotoBoothLayout.STRIP_1X2)
                                viewModel.clearCapturedImages()
                                navController.navigate(AppRoutes.PHOTO_BOOTH)
                            }
                    )
                }
                item {
                    FrameOption(
                            label = "3 Ảnh (Film Strip)",
                            description = "Chụp 3 ảnh - Dọc",
                            color = NeonCyan,
                            onClick = {
                                viewModel.updateLayout(PhotoBoothLayout.STRIP_1X3)
                                viewModel.clearCapturedImages()
                                navController.navigate(AppRoutes.PHOTO_BOOTH)
                            }
                    )
                }
                item {
                    FrameOption(
                            label = "4 Ảnh (Grid 2x2)",
                            description = "Chụp 4 ảnh - Khung vuông cổ điển",
                            color = NeonPurple,
                            onClick = {
                                viewModel.updateLayout(PhotoBoothLayout.GRID_2X2)
                                viewModel.clearCapturedImages()
                                navController.navigate(AppRoutes.PHOTO_BOOTH)
                            }
                    )
                }
                item {
                    FrameOption(
                            label = "4 Ảnh (Film Strip)",
                            description = "Chụp 4 ảnh - Dọc",
                            color = NeonCyan,
                            onClick = {
                                viewModel.updateLayout(PhotoBoothLayout.STRIP_1X4)
                                viewModel.clearCapturedImages()
                                navController.navigate(AppRoutes.PHOTO_BOOTH)
                            }
                    )
                }

                // Theme Selection
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                            "Chọn giao diện khung",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(com.example.cameraxapp.data.model.FrameThemes.ALL_THEMES.chunked(2)) {
                        themeRow ->
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themeRow.forEach { theme ->
                            ThemeOption(
                                    theme = theme,
                                    isSelected = viewModel.selectedTheme.value.id == theme.id,
                                    onClick = { viewModel.updateTheme(theme) },
                                    modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrameOption(label: String, description: String, color: Color, onClick: () -> Unit) {
    GlassBox(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleLarge, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
            )
        }
    }
}

@Composable
fun ThemeOption(
        theme: com.example.cameraxapp.data.model.FrameTheme,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(
            modifier =
                    modifier.aspectRatio(1.5f)
                            .background(
                                    color = Color(theme.backgroundColor),
                                    shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onClick)
                            .padding(12.dp),
            contentAlignment = Alignment.BottomStart
    ) {
        if (isSelected) {
            Box(
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(
                                            NeonCyan,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                    contentAlignment = Alignment.Center
            ) { Text("✓", color = Color.White) }
        }
        Text(
                text = theme.name,
                style = MaterialTheme.typography.labelSmall,
                color =
                        if (theme.backgroundColor == android.graphics.Color.WHITE ||
                                        theme.backgroundColor ==
                                                android.graphics.Color.parseColor("#F5E6D3")
                        ) {
                            Color.Black
                        } else {
                            Color.White
                        }
        )
    }
}
