package com.example.photogether.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke

/**
 * A solid card: surface fill with a hairline outline. Name kept from the earlier glass style
 * so existing callers compile.
 */
@Composable
fun GlassBox(
        modifier: Modifier = Modifier,
        cornerRadius: Dp = Radius.md,
        backgroundColor: Color = AppColors.surface,
        content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
            modifier =
                    modifier.clip(shape)
                            .background(backgroundColor)
                            .border(width = Stroke.border, color = AppColors.outline, shape = shape),
            content = content
    )
}

/** Primary action: solid accent fill with dark text. */
@Composable
fun GlassButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        text: String,
        containerColor: Color = AppColors.accent,
        enabled: Boolean = true
) {
    val contentColor = if (containerColor == AppColors.accent) AppColors.onAccent else AppColors.textPrimary
    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(Radius.sm))
                            .background(
                                    if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
                            )
                            .clickable(enabled = enabled, onClick = onClick)
                            .padding(vertical = Spacing.md, horizontal = Spacing.xl),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
        )
    }
}

/** Secondary action: elevated surface with a hairline outline. */
@Composable
fun SecondaryButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        text: String,
        enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.5f
    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(Radius.sm))
                            .background(AppColors.surfaceElevated.copy(alpha = alpha))
                            .border(
                                    width = Stroke.border,
                                    color = AppColors.outline.copy(alpha = alpha),
                                    shape = RoundedCornerShape(Radius.sm)
                            )
                            .clickable(enabled = enabled, onClick = onClick)
                            .padding(vertical = Spacing.md, horizontal = Spacing.xl),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = text,
                color = AppColors.textPrimary.copy(alpha = alpha),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
        )
    }
}
