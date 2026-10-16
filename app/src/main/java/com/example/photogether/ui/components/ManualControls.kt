package com.example.photogether.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.isSpecified
import com.example.photogether.core.render.ViewfinderSkin
import com.example.photogether.ui.view.CameraScreen.shutterColor
import com.example.photogether.ui.view.CameraScreen.shutterRingColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing

/**
 * Bottom row of the camera screen: the gallery box (left), the shutter pill (centre) and the
 * camera selector (right, shows the profile name and opens the picker).
 *
 * The left slot shows [lastPhoto] once one has been taken in this session. [onGalleryBoundsChanged]
 * reports its position in the window, so a freshly taken photo can fly into it.
 */
@Composable
fun ManualControls(
    onExposureChange: (Long) -> Unit,
    onISOChange: (Int) -> Unit,
    onClickCapture: () -> Unit,
    skin: ViewfinderSkin = ViewfinderSkin.NONE,
    onOpenGallery: () -> Unit = {},
    onOpenPicker: () -> Unit = {},
    pickerName: String? = null,
    lastPhoto: Uri? = null,
    onGalleryBoundsChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isoValue by remember { mutableIntStateOf(100) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InkColors.background)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
//        if (minExposure != null && maxExposure != null) {
//            DialControl(
//                label = "Exposure",
//                initialValue = minExposure,
//                valueRange = minExposure..maxExposure,
//                step = 10_000f,
//                onValueChange = { value -> onExposureChange(value.toLong()) }
//            )
//        }
//
//        if (minISO != null && maxISO != null) {
//            DialControl(
//                label = "ISO",
//                initialValue = minISO,
//                valueRange = minISO..maxISO,
//                step = isoValue.toFloat() * 2,
//                onValueChange = { value ->
//                    onISOChange(value.toInt())
//                    isoValue = value.toInt()
//                }
//            )
//        }

        GalleryButton(
            lastPhoto = lastPhoto,
            onClick = onOpenGallery,
            onBoundsChanged = onGalleryBoundsChanged,
        )
        Shutter(onClick = onClickCapture, skin = skin)
        PickerButton(name = pickerName, onClick = onOpenPicker)
    }
}

/** Gallery box in the thumbnail slot (36x56 rounded rectangle). Shows the last photo once taken. */
@Composable
private fun GalleryButton(lastPhoto: Uri?, onClick: () -> Unit, onBoundsChanged: (Rect) -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 56.dp)
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
            .clip(RoundedCornerShape(Radius.md))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Mở thư viện" },
        contentAlignment = Alignment.Center,
    ) {
        if (lastPhoto != null) {
            AsyncImage(
                model = lastPhoto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = null,
                tint = InkColors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Cream pill shutter, about 40% of the screen width and 64dp high, with six dots in a 2x3 grid. */
@Composable
private fun Shutter(onClick: () -> Unit, skin: ViewfinderSkin = ViewfinderSkin.NONE) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Box(
        modifier = Modifier
            .width(screenWidth * 0.4f)
            .height(64.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(skin.shutterColor.takeOrElse { InkColors.accent })
            .then(if (skin.shutterRingColor.isSpecified) Modifier.border(3.dp, skin.shutterRingColor, RoundedCornerShape(percent = 50)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Chụp ảnh" },
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(InkColors.onAccent, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

/** Camera selector: a camera icon with the current profile name under it. Opens the picker. */
@Composable
private fun PickerButton(name: String?, onClick: () -> Unit) {
    val label = name?.uppercase() ?: "CHỌN MÁY"
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Chọn máy: $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = InkColors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = BinsooType.label,
            color = InkColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
