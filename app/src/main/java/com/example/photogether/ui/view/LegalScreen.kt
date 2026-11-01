package com.example.photogether.ui.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Spacing

/** Plain text page for terms, privacy policy and open-source licenses. */
@Composable
fun LegalScreen(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = stringResource(titleRes)) },
        containerColor = AppColors.background,
    ) { padding ->
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textPrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        )
    }
}
