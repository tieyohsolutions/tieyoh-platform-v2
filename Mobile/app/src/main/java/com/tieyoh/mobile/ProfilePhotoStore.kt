package com.tieyoh.mobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

// Caches the signed-in user's profile picture locally on-device as a
// resized JPEG, scoped by user id -- purely so the avatar shows instantly
// (no network round trip) right after picking a new photo or reopening
// the app. The backend is the real source of truth per account
// (AuthResponse.profilePictureBase64, synced via
// UserApi.updateProfilePicture) since more than one Tieyoh account can be
// used on the same device; this cache used to be a single fixed filename
// shared by every account on the device, which meant switching accounts
// on the same phone kept showing whoever's photo was cached last. Keying
// the file by userId is what fixes that -- see ProfileScreen, which now
// falls back to decoding profilePictureBase64 when there's no local
// cache yet for the signed-in user's id (e.g. a first login on this
// device, or a photo set from a different device).
object ProfilePhotoStore {

    private const val MAX_DIMENSION = 512

    private fun photoFile(context: Context, userId: Long): File =
        File(context.filesDir, "profile_photo_$userId.jpg")

    fun save(context: Context, userId: Long, uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return false

            val scale = MAX_DIMENSION.toFloat() / maxOf(original.width, original.height)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }

            FileOutputStream(photoFile(context, userId)).use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun load(context: Context, userId: Long): Bitmap? {
        val file = photoFile(context, userId)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context, userId: Long) {
        photoFile(context, userId).delete()
    }

    // Raw JPEG bytes of the currently-saved photo (already resized/
    // compressed by save()), for base64-encoding and sending to the
    // backend so other users can see it in search results.
    fun readBytes(context: Context, userId: Long): ByteArray? {
        val file = photoFile(context, userId)
        if (!file.exists()) return null
        return try {
            file.readBytes()
        } catch (e: Exception) {
            null
        }
    }
}
