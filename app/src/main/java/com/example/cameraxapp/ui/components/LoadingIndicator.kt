package com.example.cameraxapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.NeonCyan

/**
 * Loading indicator component
 * @param modifier Modifier for the component
 * @param size Size of the loading indicator
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = IconSize.xxl
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = NeonCyan
        )
    }
}
