package com.example.cameraxapp.ui.view

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cameraxapp.ui.components.GlassBox
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.theme.NeonPurple

@Composable
fun HomeScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animValue by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = "move"
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme
                                            .background,
                                        MaterialTheme.colorScheme
                                            .primaryContainer
                                            .copy(alpha = 0.1f)
                                    )
                            )
                    )
        ) {
            // Animated background circles
            Box(
                modifier =
                    Modifier
                        .size(300.dp)
                        .offset(
                            x = (-50).dp + (100.dp * animValue),
                            y = 100.dp + (50.dp * animValue)
                        )
                        .alpha(0.1f)
                        .blur(100.dp)
                        .background(NeonPurple, shape = RoundedCornerShape(150.dp))
            )

            Box(
                modifier =
                    Modifier
                        .size(250.dp)
                        .align(Alignment.BottomEnd)
                        .offset(
                            x = 50.dp - (80.dp * animValue),
                            y = (-100).dp - (40.dp * animValue)
                        )
                        .alpha(0.1f)
                        .blur(80.dp)
                        .background(NeonCyan, shape = RoundedCornerShape(125.dp))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Photogether",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 64.dp)
                )

                HomeButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Chụp Ảnh",
                    onClick = { navController.navigate("frameSelection") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                HomeButton(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Thư Viện",
                    onClick = { navController.navigate("gallery") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                HomeButton(
                    icon = Icons.Default.Settings,
                    label = "Cài Đặt",
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    }
}

@Composable
fun HomeButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    GlassBox(
        modifier =
            Modifier
                .fillMaxWidth(0.8f)
                .height(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onClick),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
