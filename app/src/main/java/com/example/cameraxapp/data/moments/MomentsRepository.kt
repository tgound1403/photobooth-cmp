package com.example.cameraxapp.data.moments

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
import com.example.cameraxapp.core.moments.Moment
import com.example.cameraxapp.core.moments.MomentQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Outcome of a delete request made from the Moments viewer. */
sealed class DeleteResult {
    data object Deleted : DeleteResult()

    /** The user must confirm first. Launch [intentSender] with StartIntentSenderForResult. */
    class NeedsConfirmation(val intentSender: IntentSender) : DeleteResult()

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
                val out = ArrayList<Moment>(cursor.count)
                while (cursor.moveToNext()) {
                    out += Moment(
                        uri = ContentUris.withAppendedId(imagesUri, cursor.getLong(idCol)).toString(),
                        takenAt = cursor.getLong(dateCol) * 1000L,
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                    )
                }
                out
            }.orEmpty()
        } catch (e: SecurityException) {
            // Read permission not granted yet: show nothing instead of crashing.
            emptyList()
        }
    }

    /**
     * First delete request for [uri].
     * API 30+: always goes through the system confirmation dialog (createDeleteRequest).
     * API 29: direct delete; a RecoverableSecurityException becomes NeedsConfirmation.
     * Below 29: direct delete.
     */
    suspend fun delete(uri: String): DeleteResult = withContext(Dispatchers.IO) {
        val target = Uri.parse(uri)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                DeleteResult.NeedsConfirmation(createDeleteRequest(target))
            } else {
                deleteDirect(target)
            }
        } catch (e: IllegalArgumentException) {
            DeleteResult.Failed
        }
    }

    /**
     * Call after the user accepted a NeedsConfirmation dialog.
     * API 30+: the system already deleted the item, so this only reports success.
     * API 29: the delete is retried now that the permission has been granted.
     */
    suspend fun deleteAfterConsent(uri: String): DeleteResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DeleteResult.Deleted
        } else {
            deleteDirect(Uri.parse(uri))
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createDeleteRequest(uri: Uri): IntentSender =
        MediaStore.createDeleteRequest(contentResolver, listOf(uri)).intentSender

    private fun deleteDirect(uri: Uri): DeleteResult =
        try {
            if (contentResolver.delete(uri, null, null) > 0) DeleteResult.Deleted else DeleteResult.Failed
        } catch (e: SecurityException) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) recoverableIntent(e) else null
            if (intent != null) DeleteResult.NeedsConfirmation(intent) else DeleteResult.Failed
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun recoverableIntent(e: SecurityException): IntentSender? =
        (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
}
