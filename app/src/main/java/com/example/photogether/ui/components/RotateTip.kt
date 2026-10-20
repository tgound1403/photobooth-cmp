package com.example.photogether.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.tgound.photogether.R

/** First-run card on the portrait camera: rotate the phone to reach the Pro (landscape) body. A tap anywhere on it dismisses it. */
@Composable
fun RotateTip(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = AppIcons.Reset,
            contentDescription = null,
            tint = InkColors.accent,
            modifier = Modifier.size(28.dp).rotate(90f),
        )
        Text(
            text = stringResource(R.string.camera_rotate_tip),
            style = BinsooType.label.copy(fontSize = 14.sp, lineHeight = 18.sp),
            color = InkColors.textPrimary,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = stringResource(R.string.camera_rotate_tip_dismiss).uppercase(),
            style = BinsooType.label,
            color = InkColors.accent,
        )
    }
}
