package com.example.cameraxapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
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
 * Empty state component to display when there's no data
 * @param title Title text to display
 * @param message Message text to display
 * @param actionLabel Optional action button label
 * @param onAction Optional action callback
 * @param modifier Modifier for the component
 */
@Composable
fun EmptyState(
    title: String = "Chưa có dữ liệu",
    message: String = "Hãy thử lại sau",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
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
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = "Empty",
            tint = NeonCyan.copy(alpha = 0.5f),
            modifier = Modifier.size(IconSize.emptyState)
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = title,
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
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            GlassButton(
                onClick = onAction,
                text = actionLabel,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }
    }
}
