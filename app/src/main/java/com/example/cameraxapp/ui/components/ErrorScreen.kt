package com.example.cameraxapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.NeonCyan
import com.example.cameraxapp.ui.theme.Spacing

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
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = NeonCyan,
            modifier = Modifier.size(IconSize.errorState)
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "Đã xảy ra lỗi",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Text(
            text = message,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        GlassButton(
            onClick = onRetry,
            text = "Thử lại",
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}
