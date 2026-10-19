package com.example.photogether.data.moments

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.moments.MomentQuery
import com.example.photogether.core.moments.displaySize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Outcome of a delete request made from the Moments screen. */
sealed class DeleteResult {
    data object Deleted : DeleteResult()

    /**
     * The user must confirm first. Launch [intentSender] with StartIntentSenderForResult.
     * [pending] are the uris still to delete. After a confirmed dialog, pass them to [MomentsRepository.deleteAfterConsent].
     */
    class NeedsConfirmation(val intentSender: IntentSender, val pending: List<String>) : DeleteResult()

    data object Failed : DeleteResult()
}

/**
 * Reads the app's images from MediaStore (no Room: photobooth saves go to MediaStore too)
 * and deletes them. Needs READ_MEDIA_IMAGES or READ_EXTERNAL_STORAGE to see anything.
 */
class MomentsRepository(private val contentResolver: ContentResolver) {

    private val imagesUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.ORIENTATION,
        MediaStore.Images.Media.DISPLAY_NAME,
    )

    /** Emits the current list at once, then again whenever the images table changes. */
    fun observe(): Flow<List<Moment>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserver(imagesUri, true, observer)
        trySend(Unit)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }
        .conflate()
        .map { queryAll() }
        .flowOn(Dispatchers.IO)

    private fun queryAll(): List<Moment> {
        val (selection, args) = MomentQuery.selection(Build.VERSION.SDK_INT)
        return try {
            contentResolver.query(imagesUri, projection, selection, args, MomentQuery.SORT)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val orientationCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val out = ArrayList<Moment>(cursor.count)
                while (cursor.moveToNext()) {
                    val (w, h) = displaySize(
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        orientation = cursor.getInt(orientationCol),
                    )
                    out += Moment(
                        uri = ContentUris.withAppendedId(imagesUri, cursor.getLong(idCol)).toString(),
                        takenAt = cursor.getLong(dateCol) * 1000L,
                        width = w,
                        height = h,
                        cameraId = MomentQuery.cameraIdOf(cursor.getString(nameCol)),
                    )
                }
                out
            }.orEmpty()
        } catch (e: SecurityException) {
            // Read permission not granted yet: show nothing instead of crashing.
            emptyList()
        }
    }

    suspend fun delete(uri: String): DeleteResult = delete(listOf(uri))

    /**
     * Deletes every uri in [uris] with at most one confirmation for the user.
     * API 30+: one system dialog (createDeleteRequest) covers the whole list.
     * API 29: direct delete; a RecoverableSecurityException stops the batch as NeedsConfirmation.
     * Below 29: direct delete.
     */
    suspend fun delete(uris: List<String>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Deleted
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                DeleteResult.NeedsConfirmation(createDeleteRequest(uris), pending = uris)
            } else {
                deleteDirectUntilConsent(uris)
            }
        } catch (e: IllegalArgumentException) {
            DeleteResult.Failed
        }
    }

    suspend fun deleteAfterConsent(uri: String): DeleteResult = deleteAfterConsent(listOf(uri))

    /**
     * Call after the user accepted a NeedsConfirmation dialog, with its [DeleteResult.NeedsConfirmation.pending].
     * API 30+: the system already deleted the items, so this only reports success.
     * API 29: the first item now has permission, so it is deleted, then the rest of the batch.
     */
    suspend fun deleteAfterConsent(pending: List<String>): DeleteResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return@withContext DeleteResult.Deleted
        val first = pending.firstOrNull() ?: return@withContext DeleteResult.Deleted
        if (deleteDirect(Uri.parse(first)) != DeleteResult.Deleted) return@withContext DeleteResult.Failed
        deleteDirectUntilConsent(pending.drop(1))
    }

    /** Deletes in order. Stops at the first item that needs consent and returns the rest as pending. */
    private fun deleteDirectUntilConsent(uris: List<String>): DeleteResult {
        var anyFailed = false
        for ((index, uri) in uris.withIndex()) {
            when (val result = deleteDirect(Uri.parse(uri))) {
                DeleteResult.Deleted -> Unit
                DeleteResult.Failed -> anyFailed = true
                is DeleteResult.NeedsConfirmation ->
                    return DeleteResult.NeedsConfirmation(result.intentSender, pending = uris.subList(index, uris.size))
            }
        }
        return if (anyFailed) DeleteResult.Failed else DeleteResult.Deleted
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createDeleteRequest(uris: List<String>): IntentSender =
        MediaStore.createDeleteRequest(contentResolver, uris.map { Uri.parse(it) }).intentSender

    private fun deleteDirect(uri: Uri): DeleteResult =
        try {
            if (contentResolver.delete(uri, null, null) > 0) DeleteResult.Deleted else DeleteResult.Failed
        } catch (e: SecurityException) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) recoverableIntent(e) else null
            if (intent != null) DeleteResult.NeedsConfirmation(intent, pending = listOf(uri.toString())) else DeleteResult.Failed
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun recoverableIntent(e: SecurityException): IntentSender? =
        (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
}
