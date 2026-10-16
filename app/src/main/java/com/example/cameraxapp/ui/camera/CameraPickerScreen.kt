package com.example.cameraxapp.ui.camera

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cameraxapp.core.moments.Moment
import com.example.cameraxapp.core.render.CameraProfile
import com.example.cameraxapp.core.render.CameraProfiles
import com.example.cameraxapp.ui.theme.BinsooType
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.InkColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Stroke
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * Camera picker: a carousel of camera bodies on the ink (black) palette. Each body maps to a filter
 * in the catalog. The page at [currentFilterId] is shown first; the selected body is drawn larger.
 * The body art is [CameraBodyArt]. Thumbnails under the carousel jump the pager to that body.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CameraPickerScreen(
    profiles: List<CameraProfile> = CameraProfiles.ALL,
    currentFilterId: String? = null,
    moments: List<Moment> = emptyList(),
    onConfirm: (CameraProfile) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initialPage = profiles.indexOfFirst { it.filterId == currentFilterId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { profiles.size }
    val selected = profiles.getOrNull(pagerState.currentPage)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(InkColors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = InkColors.textPrimary,
                )
            }
            Text(
                text = "CHỌN MÁY",
                color = InkColors.textPrimary,
                style = BinsooType.label,
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            FloatingPolaroids(moments = moments, modifier = Modifier.matchParentSize())

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 100.dp),
            ) { page ->
                val profile = profiles[page]
                // 0 when this page is centred, 1 when it is a full page away.
                val distance =
                    (pagerState.currentPage - page).toFloat() + pagerState.currentPageOffsetFraction
                val fraction = distance.absoluteValue.coerceIn(0f, 1f)
                val scale = 1f - 0.45f * fraction
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - 0.4f * fraction
                            }
                            .clickable { scope.launch { pagerState.animateScrollToPage(page) } },
                    contentAlignment = Alignment.Center,
                ) {
                    CameraBodyArt(profile = profile, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Model name and tagline. CameraProfile has no separate tagline field, so the subtitle
        // (the film or brand label) is used.
        Text(
            text = selected?.name?.uppercase() ?: "",
            color = InkColors.textPrimary,
            style = BinsooType.display.copy(fontSize = 28.sp, lineHeight = 32.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Text(
            text = selected?.subtitle?.uppercase() ?: "",
            color = InkColors.textSecondary,
            style = BinsooType.label,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))

        CameraThumbnailRow(
            profiles = profiles,
            currentPage = pagerState.currentPage,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        ) {
            ConfirmButton(
                enabled = selected != null,
                onClick = { selected?.let(onConfirm) },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * One 64dp thumbnail per camera with its name underneath. The selected camera has a 2dp white
 * outline and a white label; the others are drawn at alpha 0.5.
 */
@Composable
private fun CameraThumbnailRow(
    profiles: List<CameraProfile>,
    currentPage: Int,
    onSelect: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        profiles.forEachIndexed { index, profile ->
            val isSelected = index == currentPage
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .alpha(if (isSelected) 1f else 0.5f)
                        .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(shape)
                            .background(Color(profile.bodyColor))
                            .then(
                                if (isSelected) {
                                    Modifier.border(Stroke.border, InkColors.textPrimary, shape)
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    // Top band in the profile's top colour, a flat stand-in for the body art.
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(Color(profile.topColor)),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.name.uppercase(),
                    color = InkColors.textPrimary,
                    style = BinsooType.label,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Round white 56dp button with a chevron down. Confirms the camera on the pager. */
@Composable
private fun ConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(56.dp)
                .alpha(if (enabled) 1f else 0.4f)
                .clip(CircleShape)
                .background(InkColors.textPrimary)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Chọn máy này",
            tint = InkColors.onAccent,
            modifier = Modifier.size(IconSize.md),
        )
    }
}
