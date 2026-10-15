package com.example.cameraxapp.ui.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.data.billing.BillingRepository
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke
import com.tgound.cameraxapp.R

/**
 * Shown when a free user saves a premium filter. Before and after images come from the editor, so
 * the user sees what they would get. The yearly plan is highlighted.
 *
 * Prices are the formatted strings Play returns. While they are unknown the buttons stay disabled.
 */
@Composable
fun PaywallScreen(
    before: Bitmap?,
    after: Bitmap?,
    monthlyPrice: String?,
    yearlyPrice: String?,
    onPurchase: (productId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.paywall_title),
            style = MaterialTheme.typography.headlineSmall,
            color = AppColors.textPrimary,
        )
        Text(
            text = stringResource(R.string.paywall_body),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PreviewTile(label = stringResource(R.string.paywall_before), bitmap = before, modifier = Modifier.weight(1f))
            PreviewTile(label = stringResource(R.string.paywall_after), bitmap = after, modifier = Modifier.weight(1f))
        }
        PlanCard(
            title = stringResource(R.string.paywall_yearly),
            price = yearlyPrice,
            highlighted = true,
            onPurchase = { onPurchase(BillingRepository.YEARLY_PRODUCT_ID) },
        )
        PlanCard(
            title = stringResource(R.string.paywall_monthly),
            price = monthlyPrice,
            highlighted = false,
            onPurchase = { onPurchase(BillingRepository.MONTHLY_PRODUCT_ID) },
        )
        Text(
            text = stringResource(R.string.paywall_not_now),
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(Radius.sm))
                .clickable(onClick = onDismiss)
                .padding(Spacing.sm),
        )
    }
}

@Composable
private fun PreviewTile(label: String, bitmap: Bitmap?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(shape)
            .background(AppColors.surface)
            .border(Stroke.hairline, AppColors.outline, shape),
    ) {
        bitmap?.let {
            val image = remember(it) { it.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.textPrimary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.sm),
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String?,
    highlighted: Boolean,
    onPurchase: () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.surfaceElevated)
            .border(
                width = if (highlighted) Stroke.selected else Stroke.hairline,
                color = if (highlighted) AppColors.accent else AppColors.outline,
                shape = shape,
            )
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (highlighted) {
            Text(
                text = stringResource(R.string.paywall_best_value),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.accent,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textPrimary,
        )
        Text(
            text = price ?: stringResource(R.string.paywall_price_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.textSecondary,
        )
        GlassButton(
            onClick = onPurchase,
            text = stringResource(R.string.paywall_subscribe),
            enabled = price != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
