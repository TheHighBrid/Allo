package com.kenza.callsim.ui

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages contact photos with iOS-style avatar generation.
 * Provides contact photo lookup from Android contacts and generates
 * iOS-style initial avatars when no photo is available.
 */
class ContactPhotoManager(
    private val context: Context,
    private val imageLoader: ImageLoader,
) {
    
    companion object {
        private const val TAG = "ContactPhotoManager"
        private const val DEFAULT_AVATAR_SIZE = 256
        private const val CACHE_SIZE = 100
    }
    
    // Memory cache for contact photos
    private val photoCache = ConcurrentHashMap<String, ImageBitmap?>(CACHE_SIZE.toInt())
    
    // Color palette for initial avatars (iOS-style)
    private val avatarColors = listOf(
        Color.parseColor("#FF34C759"), // Green
        Color.parseColor("#FF0A84FF"), // Blue
        Color.parseColor("#FFFF9500"), // Orange
        Color.parseColor("#FFFF2D55"), // Red
        Color.parseColor("#FFAF52DE"), // Purple
        Color.parseColor("#FF5AC8FA"), // Teal
        Color.parseColor("#FF5856D6"), // Indigo
        Color.parseColor("#FFFFD60A"), // Yellow
        Color.parseColor("#FF30D158"), // Brighter Green
        Color.parseColor("#FF007AFF"), // Brighter Blue
    )
    
    /**
     * Get contact photo as ImageBitmap for a given contact name or phone number.
     * Returns null if no photo is available and initial avatar generation is disabled.
     */
    suspend fun getContactPhoto(
        contactName: String,
        phoneNumber: String? = null,
        size: Int = DEFAULT_AVATAR_SIZE,
        generateInitialAvatar: Boolean = true,
    ): ImageBitmap? {
        // Check cache first
        val cacheKey = "${contactName}_${phoneNumber}_$size"
        photoCache[cacheKey]?.let { cached ->
            return cached
        }
        
        // Try to get photo from contacts
        val photoBitmap = if (phoneNumber != null) {
            getContactPhotoFromPhoneNumber(phoneNumber, size)
        } else {
            getContactPhotoFromName(contactName, size)
        }
        
        if (photoBitmap != null) {
            val imageBitmap = photoBitmap.asImageBitmap()
            photoCache[cacheKey] = imageBitmap
            return imageBitmap
        }
        
        // Generate initial avatar if requested
        if (generateInitialAvatar) {
            val avatar = generateInitialAvatar(contactName, size)
            photoCache[cacheKey] = avatar
            return avatar
        }
        
        photoCache[cacheKey] = null
        return null
    }
    
    /**
     * Get contact photo from phone number.
     */
    private suspend fun getContactPhotoFromPhoneNumber(
        phoneNumber: String,
        size: Int,
    ): Bitmap? {
        val contactId = getContactIdFromPhoneNumber(phoneNumber)
        return if (contactId != null) {
            getContactPhotoById(contactId, size)
        } else {
            null
        }
    }
    
    /**
     * Get contact photo from contact name.
     */
    private suspend fun getContactPhotoFromName(
        name: String,
        size: Int,
    ): Bitmap? {
        val contactId = getContactIdFromName(name)
        return if (contactId != null) {
            getContactPhotoById(contactId, size)
        } else {
            null
        }
    }
    
    /**
     * Get contact photo by contact ID.
     */
    private suspend fun getContactPhotoById(
        contactId: Long,
        size: Int,
    ): Bitmap? = runCatching {
        val photoUri = ContentUris.withAppendedId(
            ContactsContract.Contacts.CONTENT_URI,
            contactId
        )
        
        val request = ImageRequest.Builder(context)
            .data(photoUri)
            .size(size)
            .build()
        
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            result.drawable.toBitmap(size, size)
        } else {
            null
        }
    }.getOrNull()
    
    /**
     * Get contact ID from phone number.
     */
    private fun getContactIdFromPhoneNumber(phoneNumber: String): Long? = runCatching {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(0)
            } else {
                null
            }
        }
    }.getOrNull()
    
    /**
     * Get contact ID from name.
     */
    private fun getContactIdFromName(name: String): Long? = runCatching {
        val uri = ContactsContract.Contacts.CONTENT_URI
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(name)
        
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts._ID),
            selection,
            selectionArgs,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(0)
            } else {
                null
            }
        }
    }.getOrNull()
    
    /**
     * Generate iOS-style initial avatar from contact name.
     */
    fun generateInitialAvatar(
        name: String,
        size: Int = DEFAULT_AVATAR_SIZE,
    ): ImageBitmap {
        // Get initials
        val initials = getInitials(name)
        
        // Get color based on name hash
        val color = getColorForName(name)
        
        // Create bitmap
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Draw initials
        paint.color = Color.WHITE
        paint.textSize = (size * 0.4).toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        
        // Calculate text position
        val textBounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val textY = (size / 2f) - (textHeight / 2f) - textBounds.top
        
        canvas.drawText(initials, size / 2f, textY, paint)
        
        return bitmap.asImageBitmap()
    }
    
    /**
     * Get initials from name.
     */
    private fun getInitials(name: String): String {
        if (name.isBlank()) return "?"
        
        val words = name.trim().split("\\s+".toRegex())
        return when (words.size) {
            0 -> "?"
            1 -> words[0].take(1).uppercase()
            else -> (words[0].take(1) + words[words.size - 1].take(1)).uppercase()
        }
    }
    
    /**
     * Get color for name based on hash.
     */
    private fun getColorForName(name: String): Int {
        val hash = name.hashCode()
        return avatarColors[hash.mod(avatarColors.size)]
    }
    
    /**
     * Get iOS-style gradient avatar.
     */
    fun generateGradientAvatar(
        name: String,
        size: Int = DEFAULT_AVATAR_SIZE,
    ): ImageBitmap {
        val initials = getInitials(name)
        val colorIndex = name.hashCode().mod(avatarColors.size)
        val color1 = avatarColors[colorIndex]
        val color2 = avatarColors[(colorIndex + 1).mod(avatarColors.size)]
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw gradient background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gradient = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            color1, color2, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Draw initials
        paint.shader = null
        paint.color = Color.WHITE
        paint.textSize = (size * 0.4).toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        
        val textBounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val textY = (size / 2f) - (textHeight / 2f) - textBounds.top
        
        canvas.drawText(initials, size / 2f, textY, paint)
        
        return bitmap.asImageBitmap()
    }
    
    /**
     * Get contact photo URI for a given contact name.
     */
    fun getContactPhotoUri(contactName: String): Uri? = runCatching {
        val contactId = getContactIdFromName(contactName)
        if (contactId != null) {
            ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI,
                contactId
            )
        } else {
            null
        }
    }.getOrNull()
    
    /**
     * Clear cache for a specific contact.
     */
    fun clearCache(contactName: String, phoneNumber: String? = null) {
        val cacheKey = "${contactName}_${phoneNumber}_*"
        photoCache.keys.removeIf { it.startsWith(cacheKey) }
    }
    
    /**
     * Clear all cached photos.
     */
    fun clearAllCache() {
        photoCache.clear()
    }
    
    /**
     * Invalidate cache and reload photos.
     */
    suspend fun refreshContactPhoto(
        contactName: String,
        phoneNumber: String? = null,
        size: Int = DEFAULT_AVATAR_SIZE,
    ): ImageBitmap? {
        clearCache(contactName, phoneNumber)
        return getContactPhoto(contactName, phoneNumber, size)
    }
}
