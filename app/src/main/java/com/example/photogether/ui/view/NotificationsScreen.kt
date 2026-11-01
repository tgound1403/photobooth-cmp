package com.example.photogether.ui.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.core.colors.ColorStory
import com.example.photogether.data.colors.ColorPhoto
import com.example.photogether.ui.colors.ColorStoriesViewModel
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.tgound.photogether.R
import org.koin.androidx.compose.koinViewModel

/** Remembers how many colour stories the user has already looked at, so the bell on Home can show a dot. */
object WhatsNewSeen {
    private const val PREFS = "home_notifications"
    private const val KEY = "seen_story_count"

    fun hasUnread(context: Context, storyCount: Int): Boolean =
        storyCount > context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0)

    fun markSeen(context: Context, storyCount: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, storyCount).apply()
    }
}

/** Inbox opened from the bell on Home. For now it holds one entry: "What's new", the colour stories. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onStoryClick: (ColorStory) -> Unit,
    colorStoriesViewModel: ColorStoriesViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val covers by colorStoriesViewModel.covers.collectAsStateWithLifecycle()
    // Keyed on the configuration so a language switch reloads the covers for that language.
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) { colorStoriesViewModel.loadCovers() }
    LaunchedEffect(Unit) { WhatsNewSeen.markSeen(context, ColorStories.ALL.size) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.action_back),
                    tint = AppColors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.notifications_title),
                style = BinsooType.title,
                color = AppColors.textPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            WhatsNewEntry(stories = ColorStories.ALL, covers = covers, onStoryClick = onStoryClick)
        }
    }
}

private val GROUP_THUMB_SIZE = 72.dp
private val STORY_TILE_HEIGHT = 124.dp
private val STORY_GRID_HEIGHT = STORY_TILE_HEIGHT * 2 + Spacing.lg

/** The "What's new" notification: a title and the colour groups, two rows scrolling sideways. */
@Composable
private fun WhatsNewEntry(
    stories: List<ColorStory>,
    covers: Map<String, ColorPhoto>,
    onStoryClick: (ColorStory) -> Unit,
) {
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(Stroke.border, AppColors.outline, shape)
            .background(AppColors.surface, shape)
            .padding(Spacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.notifications_whats_new),
                style = BinsooType.title,
                color = AppColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${stories.size} MÀU",
                style = BinsooType.micro,
                color = AppColors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(STORY_GRID_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            contentPadding = PaddingValues(end = Spacing.lg),
        ) {
            items(stories, key = { it.id }) { story ->
                ColorStoryTile(story = story, cover = covers[story.id], onClick = { onStoryClick(story) })
            }
        }
    }
}

/** Colour group entry: the place's photo graded with its filter, then its name and photo count. */
@Composable
private fun ColorStoryTile(
    story: ColorStory,
    cover: ColorPhoto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbShape = RoundedCornerShape(Radius.sm)
    Column(modifier = modifier.width(GROUP_THUMB_SIZE).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(GROUP_THUMB_SIZE)
                .border(Stroke.border, AppColors.outline, thumbShape)
                .padding(Stroke.border)
                .background(AppColors.surfaceElevated),
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover.path,
                    contentDescription = story.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(AppColors.accent)
                    .padding(horizontal = Spacing.xs, vertical = 2.dp),
            ) {
                Text(text = "VIBE", style = BinsooType.micro, color = AppColors.onAccent)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = story.name.uppercase(),
            style = BinsooType.label.copy(fontWeight = FontWeight.Bold),
            color = AppColors.textPrimary,
            maxLines = 1,
        )
        Text(
            text = "${ColorStories.PHOTOS_PER_STORY} ẢNH | →",
            style = BinsooType.micro,
            color = AppColors.textSecondary,
        )
    }
}
