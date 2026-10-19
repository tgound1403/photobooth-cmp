package com.example.photogether.ui.moments

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photogether.data.moments.DeleteResult
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Stroke
import kotlinx.coroutines.launch
import com.example.photogether.ui.theme.AppIcons

/** A delete the UI has asked for. Plain class, so every request is a new key for LaunchedEffect. */
private class PendingDelete(val uri: String, val afterConsent: Boolean)

/**
 * Full-screen pager over the Moments list.
 * Delete: API 30+ goes straight to the system dialog; below 30 an app dialog comes first.
 * [onEdit] receives the content uri of the current page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentViewerScreen(
    viewModel: MomentsViewModel,
    startIndex: Int,
    onBack: () -> Unit,
    onEdit: (uri: String) -> Unit,
) {
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var confirmUri by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<String?>(null) }
    var deleteRequest by remember { mutableStateOf<PendingDelete?>(null) }
    var sawItems by remember { mutableStateOf(false) }

    // Result of the system delete dialog (API 29 RecoverableSecurityException, API 30+ createDeleteRequest).
    val confirmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val uri = pendingUri
        pendingUri = null
        if (uri != null && result.resultCode == Activity.RESULT_OK) {
            deleteRequest = PendingDelete(uri, afterConsent = true)
        }
    }

    LaunchedEffect(deleteRequest) {
        val request = deleteRequest ?: return@LaunchedEffect
        val result = if (request.afterConsent) {
            viewModel.deleteAfterConsent(request.uri)
        } else {
            viewModel.delete(request.uri)
        }
        when (result) {
            DeleteResult.Deleted -> Unit
            is DeleteResult.NeedsConfirmation -> {
                pendingUri = request.uri
                confirmLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            }
            DeleteResult.Failed -> snackbar.showSnackbar("Không xóa được ảnh")
        }
    }

    // Close when the last image is gone. Not on first open, while the list is still loading.
    LaunchedEffect(moments) {
        if (moments.isNotEmpty()) {
            sawItems = true
        } else if (sawItems) {
            onBack()
        }
    }

    fun requestDelete(uri: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            confirmUri = uri
        } else {
            deleteRequest = PendingDelete(uri, afterConsent = false)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(InkColors.background)) {
        if (moments.isEmpty()) {
            Text(
                text = stringResource(R.string.moments_viewer_empty),
                style = BinsooType.body,
                color = InkColors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val pagerState = rememberPagerState(
                initialPage = startIndex.coerceIn(0, moments.lastIndex),
            ) { moments.size }
            val current = moments.getOrNull(pagerState.currentPage)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AsyncImage(
                    model = moments[page].uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Close button, top left.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Close, contentDescription = stringResource(R.string.action_close), tint = InkColors.textPrimary)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(moments, key = { _, moment -> moment.uri }) { index, moment ->
                        val selected = index == pagerState.currentPage
                        AsyncImage(
                            model = moment.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(Radius.md))
                                .border(
                                    width = if (selected) Stroke.border else 0.dp,
                                    color = if (selected) InkColors.textPrimary else InkColors.background,
                                    shape = RoundedCornerShape(Radius.md),
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(
                        onClick = { current?.let { onEdit(it.uri) } },
                        enabled = current != null,
                    ) {
                        Text(stringResource(R.string.action_edit), style = BinsooType.label, color = InkColors.textPrimary)
                    }
                    TextButton(
                        onClick = { current?.let { requestDelete(it.uri) } },
                        enabled = current != null,
                    ) {
                        Text(stringResource(R.string.action_delete), style = BinsooType.label, color = InkColors.danger)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 120.dp),
        )
    }

    confirmUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmUri = null },
            title = { Text(stringResource(R.string.dialog_delete_photo_title)) },
            text = { Text(stringResource(R.string.dialog_delete_photo_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmUri = null
                        deleteRequest = PendingDelete(uri, afterConsent = false)
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUri = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
