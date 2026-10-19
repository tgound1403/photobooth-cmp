package com.example.photogether.ui.components

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photogether.core.share.ShareTarget
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.example.photogether.ui.theme.AppIcons

/** Các đích hiển thị trên sheet. OTHER không hiện, vì đã có chooser chung. */
private val SHARE_SHEET_TARGETS: List<ShareTarget> =
    ShareTarget.entries.filter { it != ShareTarget.OTHER }

/**
 * Sheet chia sẻ sau khi lưu ảnh. Tái dùng được: không biết về ViewModel,
 * chỉ gọi lại [onTarget], [onSaveRecipe] và [onDismiss].
 *
 * [previewUri] là content URI của ảnh vừa lưu (null thì ẩn ảnh preview).
 * [recipeCode] là mã công thức, hiển thị ở ô chỉ đọc (chọn được chữ) và sao chép bằng nút riêng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    previewUri: String?,
    recipeCode: String,
    onTarget: (ShareTarget) -> Unit,
    onSaveRecipe: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    // skipPartiallyExpanded: the content is tall, so open fully instead of halfway.
    // The column scrolls and keeps clear of the navigation bar so the download button and the
    // hashtag footer are never cut off.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.background,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Ảnh vừa lưu (preview) và dòng trạng thái.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (previewUri != null) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = stringResource(R.string.share_saved_photo),
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier.size(72.dp)
                                .clip(RoundedCornerShape(Radius.sm))
                                .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm)),
                    )
                }
                Text(
                    text = stringResource(R.string.share_saved),
                    style = BinsooType.label,
                    color = AppColors.textSecondary,
                )
            }

            // Tiêu đề, nút đóng và vạch kẻ mảnh.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.share_title),
                        style = BinsooType.title,
                        color = AppColors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(AppColors.surfaceElevated)
                                .clickable(onClick = onDismiss),
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = AppColors.textPrimary,
                            // 22dp theo spec mục 3, chưa có token icon tương ứng.
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(Stroke.hairline)
                            .background(AppColors.outline),
                )
            }

            // Năm đích chia sẻ, mỗi đích chiếm đều một phần chiều ngang.
            Row(modifier = Modifier.fillMaxWidth()) {
                SHARE_SHEET_TARGETS.forEach { target ->
                    ShareTargetButton(
                        target = target,
                        onClick = { onTarget(target) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Mã công thức: chỉ đọc, chọn được chữ bằng thao tác giữ.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = stringResource(R.string.share_recipe_code),
                    style = BinsooType.label,
                    color = AppColors.textSecondary,
                )
                SelectionContainer {
                    Text(
                        text = recipeCode,
                        style = BinsooType.body,
                        color = AppColors.textPrimary,
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(AppColors.surface)
                                .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm))
                                .padding(Spacing.md),
                    )
                }
            }

            // Sao chép mã công thức (nút viền).
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.sm))
                        .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm))
                        .clickable { clipboard.setText(AnnotatedString(recipeCode)) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.share_recipe),
                    style = BinsooType.label,
                    color = AppColors.textPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
                Icon(
                    imageVector = AppIcons.Document,
                    contentDescription = null,
                    tint = AppColors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterEnd).size(IconSize.sm),
                )
            }

            // Lưu công thức (nút đặc).
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(AppColors.accent)
                        .clickable(onClick = onSaveRecipe)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.share_download),
                    style = BinsooType.title,
                    color = AppColors.onAccent,
                    modifier = Modifier.align(Alignment.Center),
                )
                Icon(
                    imageVector = AppIcons.Download,
                    contentDescription = null,
                    tint = AppColors.onAccent,
                    modifier = Modifier.align(Alignment.CenterEnd).size(IconSize.sm),
                )
            }

            // Chú thích hashtag.
            Text(
                text =
                    buildAnnotatedString {
                        append("HÃY DÙNG HASHTAG ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppColors.textPrimary)) {
                            append("#PHOTOGETHER")
                        }
                        append(" KHI ĐĂNG LÊN MẠNG XÃ HỘI ĐỂ MÌNH CÒN BIẾT ĐƯỜNG HÓNG VỚI NHA")
                    },
                style = BinsooType.micro,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Nút tròn có chữ cái đại diện. Không dùng logo thương hiệu. */
@Composable
private fun ShareTargetButton(
    target: ShareTarget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .clip(RoundedCornerShape(Radius.sm))
                .clickable(onClick = onClick)
                .padding(vertical = Spacing.xs),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(56.dp)
                    .clip(CircleShape)
                    .background(AppColors.surfaceElevated),
        ) {
            Text(
                text = initialOf(target),
                style = BinsooType.title,
                color = AppColors.textPrimary,
            )
        }
        Text(
            text = target.label,
            style = BinsooType.label,
            color = AppColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        )
    }
}

private fun initialOf(target: ShareTarget): String =
    when (target) {
        ShareTarget.TIKTOK -> "T"
        ShareTarget.INSTAGRAM -> "I"
        ShareTarget.IG_STORY -> "S"
        ShareTarget.FACEBOOK -> "f"
        ShareTarget.MESSENGER -> "M"
        ShareTarget.OTHER -> "?"
    }
