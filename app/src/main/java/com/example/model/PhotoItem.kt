package com.example.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a photo item discovered in the device's MediaStore.
 */
data class PhotoItem(
    val id: Long,
    val displayName: String,
    val contentUri: Uri,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAddedSeconds: Long,
    val dateTakenMs: Long,
    val bucketName: String
) {
    fun getFormattedSize(): String {
        if (sizeBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = sizeBytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }

    fun getFormattedDate(): String {
        val timestamp = if (dateTakenMs > 0) dateTakenMs else dateAddedSeconds * 1000
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getResolutionString(): String {
        return if (width > 0 && height > 0) "${width}x${height}" else "Image"
    }
}

data class AlbumInfo(
    val name: String,
    val photoCount: Int,
    val coverUri: Uri?
)
