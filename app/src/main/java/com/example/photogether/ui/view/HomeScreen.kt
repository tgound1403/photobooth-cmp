package com.example.photogether.ui.view

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.core.moments.MomentDrawer
import com.example.photogether.ui.moments.AjarDrawer
import com.example.photogether.ui.moments.PhotoCalendar
import com.example.photogether.ui.moments.MomentsViewModel
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.example.photogether.core.moments.CalendarColors
import com.example.photogether.data.prefs.ThemeRepository
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.core.tutorial.TutorialScreen
import com.example.photogether.core.tutorial.TutorialStep
import com.example.photogether.ui.tutorial.TutorialLayer
import com.example.photogether.ui.tutorial.TutorialViewModel
import com.example.photogether.ui.tutorial.tutorialTarget

@Composable
fun HomeScreen(
    navController: NavController,
    momentsViewModel: MomentsViewModel = koinViewModel(),
    themeRepository: ThemeRepository = koinInject(),
    tutorial: TutorialViewModel = koinViewModel(),
) {
    val tutorialStep by tutorial.step.collectAsStateWithLifecycle()
    val calendarColor by themeRepository.calendarColor.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val moments by momentsViewModel.moments.collectAsStateWithLifecycle()
    // The read permission may have been granted after the list was first loaded.
    LaunchedEffect(Unit) { momentsViewModel.refresh() }
    // Read here so it is fresh each time Home comes back from the notifications screen.
    val hasUnread = remember { WhatsNewSeen.hasUnread(context, ColorStories.ALL.size) }

    // Pick an image from the device and open it straight in the editor.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // The route is decoded twice (NavController, then the IMAGE_DETAIL handler), so the uri is
        // encoded once up front like the other callers; otherwise "%3A" in a document id becomes ":",
        // which no longer matches the picker's one-shot read grant.
        uri?.let { navController.navigate(AppRoutes.imageDetail(Uri.encode(it.toString()))) }
    }

    HomeContent(
        onPhotoBoothClick = { navController.navigate(AppRoutes.FRAME_SELECTION) },
        onPickImageClick = { imagePickerLauncher.launch("image/*") },
        onSettingsClick = { navController.navigate(AppRoutes.SETTINGS) },
        onCameraClick = {
            tutorial.advance(TutorialStep.HOME_CAMERA)
            navController.navigate(AppRoutes.MAIN)
        },
        moments = moments,
        onGalleryClick = { navController.navigate(AppRoutes.moments(MomentDrawer.SHOTS)) },
        onPhotoBoothGalleryClick = { navController.navigate(AppRoutes.moments(MomentDrawer.PHOTOBOOTH)) },
        onEditMoment = { uri -> navController.navigate(AppRoutes.imageDetail(uri)) },
        calendarColor = calendarColor,
        hasUnread = hasUnread,
        onNotificationsClick = { navController.navigate(AppRoutes.NOTIFICATIONS) },
        tutorialStep = tutorialStep,
        onCalendarDayClosed = { tutorial.advance(TutorialStep.OPEN_CALENDAR_DAY) },
        // Only FINISH has a button on Home; it ends the tour.
        onTutorialNext = tutorial::skip,
        onTutorialSkip = tutorial::skip,
    )
}

@Composable
fun HomeContent(
    onPhotoBoothClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCameraClick: () -> Unit = {},
    moments: List<Moment> = emptyList(),
    onGalleryClick: () -> Unit = {},
    onPhotoBoothGalleryClick: () -> Unit = {},
    onEditMoment: (uri: String) -> Unit = {},
    calendarColor: Int = CalendarColors.DEFAULT,
    hasUnread: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    tutorialStep: TutorialStep? = null,
    onCalendarDayClosed: () -> Unit = {},
    onTutorialNext: () -> Unit = {},
    onTutorialSkip: () -> Unit = {},
) {
    TutorialLayer(
        step = tutorialStep,
        screen = TutorialScreen.HOME,
        onNext = onTutorialNext,
        onSkip = onTutorialSkip,
    ) {
    Scaffold(containerColor = AppColors.background) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
            ) {
                // Bell and hamburger, top right. The bell opens the notifications, the hamburger Settings.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNotificationsClick) {
                        Box {
                            Icon(
                                imageVector = AppIcons.Bell,
                                contentDescription = stringResource(R.string.home_notifications),
                                tint = AppColors.textPrimary
                            )
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .background(AppColors.danger, CircleShape)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = AppIcons.Menu,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = AppColors.textPrimary
                        )
                    }
                }

                Text(
                    text = "PHOTO",
                    style = HOME_TITLE_STYLE,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "GETHER",
                    style = HOME_TITLE_STYLE,
                    color = AppColors.textPrimary
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                Text(
                    text = stringResource(R.string.calendar_title),
                    style = BinsooType.title,
                    color = AppColors.textPrimary,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                PhotoCalendar(
                    moments = moments,
                    onEdit = onEditMoment,
                    sheetColor = calendarColor,
                    onDayClosed = onCalendarDayClosed,
                    modifier = Modifier.tutorialTarget("calendar"),
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Shots and photobooth strips side by side, each a drawer ajar that opens its own photos.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    AjarDrawer(
                        label = "GALLERY",
                        moments = remember(moments) { MomentDrawer.SHOTS.filter(moments) },
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f),
                        height = FOLDER_HEIGHT,
                    )
                    AjarDrawer(
                        label = stringResource(R.string.home_photobooth),
                        moments = remember(moments) { MomentDrawer.PHOTOBOOTH.filter(moments) },
                        onClick = onPhotoBoothGalleryClick,
                        modifier = Modifier.weight(1f),
                        height = FOLDER_HEIGHT,
                    )
                }

                // Keeps the last row clear of the floating buttons.
                Spacer(modifier = Modifier.height(FLOAT_CLEARANCE))
            }

            // Three floating round buttons: pick an image from the device, start a photobooth, open the camera.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                FloatingRoundButton(
                    icon = AppIcons.AddPhoto,
                    contentDescription = stringResource(R.string.home_edit_from_device),
                    onClick = onPickImageClick,
                )
                FloatingRoundButton(
                    icon = AppIcons.FilmStrip,
                    contentDescription = stringResource(R.string.home_photobooth),
                    onClick = onPhotoBoothClick,
                )
                FloatingRoundButton(
                    icon = AppIcons.Camera,
                    contentDescription = stringResource(R.string.home_camera),
                    onClick = onCameraClick,
                    modifier = Modifier.tutorialTarget("home_camera"),
                )
            }
        }
    }
    }
}

private val HOME_TITLE_STYLE = BinsooType.display
private val FOLDER_HEIGHT = 168.dp
private val FLOAT_BUTTON_SIZE = 56.dp
private val FLOAT_CLEARANCE = 96.dp

/** Round black button with a white icon, over the paper background. */
@Composable
private fun FloatingRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(FLOAT_BUTTON_SIZE)
            .clip(CircleShape)
            .background(InkColors.background.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = InkColors.textPrimary,
            modifier = Modifier.size(IconSize.md),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PhotogetherTheme {
        HomeContent(
            onPhotoBoothClick = {},
            onPickImageClick = {},
            onSettingsClick = {},
        )
    }
}
