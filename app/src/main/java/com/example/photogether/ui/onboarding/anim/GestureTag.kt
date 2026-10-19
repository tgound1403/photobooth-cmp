package com.example.photogether.ui.onboarding.anim

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke

/** Small mono label under an illustration. The one for the step currently playing turns yellow. */
@Composable
internal fun GestureTag(text: String, active: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.sm),
        color = if (active) AppColors.highlight else AppColors.surface,
        border = BorderStroke(Stroke.border, AppColors.outline),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.textPrimary,
            modifier = Modifier
                .alpha(if (active) 1f else 0.6f)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs + 2.dp),
        )
    }
}

@Composable
internal fun GestureTagRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(top = Spacing.lg),
    ) { content() }
}
