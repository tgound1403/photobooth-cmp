package com.example.photogether.ui.components

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.AppIcons

/**
 * Error screen component to display error messages
 * @param message Error message to display
 * @param onRetry Callback when retry button is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = AppIcons.Error,
            contentDescription = null,
            tint = AppColors.danger,
            modifier = Modifier.size(IconSize.hero)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        GlassButton(
            onClick = onRetry,
            text = stringResource(R.string.action_retry),
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    PhotogetherTheme {
        ErrorScreen(message = "Không thể kết nối camera", onRetry = {})
    }
}
