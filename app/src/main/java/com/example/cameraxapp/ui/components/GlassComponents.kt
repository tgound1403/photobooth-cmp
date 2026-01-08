package com.example.cameraxapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.ui.theme.GlassWhite
import com.example.cameraxapp.ui.theme.NeonPurple

@Composable
fun GlassBox(
        modifier: Modifier = Modifier,
        cornerRadius: Dp = 16.dp,
        backgroundColor: Color = GlassWhite,
        content: @Composable BoxScope.() -> Unit
) {
    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(cornerRadius))
                            .background(backgroundColor)
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.linearGradient(
                                                    colors =
                                                            listOf(
                                                                    Color.White.copy(alpha = 0.3f),
                                                                    Color.White.copy(alpha = 0.05f),
                                                                    Color.White.copy(alpha = 0.2f)
                                                            )
                                            ),
                                    shape = RoundedCornerShape(cornerRadius)
                            ),
            content = content
    )
}

@Composable
fun GlassButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        text: String,
        containerColor: Color = NeonPurple,
        enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.5f
    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(24.dp))
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    containerColor.copy(
                                                                            alpha = 0.4f * alpha
                                                                    ),
                                                                    containerColor.copy(
                                                                            alpha = 0.2f * alpha
                                                                    )
                                                            )
                                            )
                            )
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.linearGradient(
                                                    colors =
                                                            listOf(
                                                                    Color.White.copy(
                                                                            alpha = 0.4f * alpha
                                                                    ),
                                                                    Color.Transparent,
                                                                    Color.White.copy(
                                                                            alpha = 0.2f * alpha
                                                                    )
                                                            )
                                            ),
                                    shape = RoundedCornerShape(24.dp)
                            )
                            .clickable(enabled = enabled, onClick = onClick)
                            .padding(vertical = 14.dp, horizontal = 28.dp),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = text,
                color = Color.White.copy(alpha = alpha),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
