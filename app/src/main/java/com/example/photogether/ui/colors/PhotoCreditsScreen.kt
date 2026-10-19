package com.example.photogether.ui.colors

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.photogether.data.colors.SampleCredit
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import com.example.photogether.ui.theme.AppIcons

/** Attribution for every bundled sample photo. Each row opens the photographer and the photo on Unsplash. */
@Composable
fun PhotoCreditsScreen(
    onBack: () -> Unit,
    viewModel: ColorStoriesViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.loadCredits() }
    val credits by viewModel.credits.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(containerColor = AppColors.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            item {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = stringResource(R.string.action_back),
                        tint = AppColors.textPrimary,
                        modifier = Modifier.size(IconSize.md),
                    )
                }
                Text(
                    text = stringResource(R.string.credits_title),
                    style = BinsooType.display.copy(fontSize = 36.sp, lineHeight = 36.sp),
                    color = AppColors.textPrimary,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.credits_body),
                    style = BinsooType.body,
                    color = AppColors.textSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            items(credits, key = { it.file }) { credit ->
                CreditRow(credit = credit, onOpen = { url -> runCatching { uriHandler.openUri(url) } })
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }
    }
}

@Composable
private fun CreditRow(credit: SampleCredit, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = credit.file.uppercase(),
            style = BinsooType.label,
            fontWeight = FontWeight.Bold,
            color = AppColors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.credits_photo_label),
                style = BinsooType.body,
                color = AppColors.textSecondary,
            )
            Text(
                text = credit.photographer,
                style = BinsooType.body,
                color = AppColors.textPrimary,
                modifier = Modifier.clickable { onOpen(credit.photographerUrl) },
            )
            Text(text = "·", style = BinsooType.body, color = AppColors.textSecondary)
            Text(
                text = "Unsplash",
                style = BinsooType.body,
                color = AppColors.textPrimary,
                modifier = Modifier.clickable { onOpen(credit.photoUrl) },
            )
        }
    }
}
