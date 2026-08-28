package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import com.example.model.AlbumInfo
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GalleryRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val albums: StateFlow<List<AlbumInfo>> = _albums.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun refreshPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        val photoList = mutableListOf<PhotoItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = it.getColumnIndex(MediaStore.Images.Media.DATA)
                val mimeCol = it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                val widthCol = it.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = it.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val dateAddedCol = it.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val dateTakenCol = it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val bucketCol = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "photo_$id.jpg"
                    val path = if (pathCol != -1) it.getString(pathCol) ?: "" else ""
                    val mimeType = if (mimeCol != -1) it.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val size = if (sizeCol != -1) it.getLong(sizeCol) else 0L
                    val width = if (widthCol != -1) it.getInt(widthCol) else 0
                    val height = if (heightCol != -1) it.getInt(heightCol) else 0
                    val dateAdded = if (dateAddedCol != -1) it.getLong(dateAddedCol) else 0L
                    val dateTaken = if (dateTakenCol != -1) it.getLong(dateTakenCol) else 0L
                    val bucketName = if (bucketCol != -1) it.getString(bucketCol) ?: "Camera" else "Camera"

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    photoList.add(
                        PhotoItem(
                            id = id,
                            displayName = name,
                            contentUri = contentUri,
                            path = path,
                            mimeType = mimeType,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            dateAddedSeconds = dateAdded,
                            dateTakenMs = dateTaken,
                            bucketName = bucketName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching media photos: ${e.message}")
        }

        val albumMap = mutableMapOf<String, MutableList<PhotoItem>>()
        for (photo in photoList) {
            val list = albumMap.getOrPut(photo.bucketName) { mutableListOf() }
            list.add(photo)
        }

        val albumList = albumMap.map { (name, photos) ->
            AlbumInfo(
                name = name,
                photoCount = photos.size,
                coverUri = photos.firstOrNull()?.contentUri
            )
        }.sortedByDescending { it.photoCount }

        _photos.value = photoList
        _albums.value = albumList
        _isSyncing.value = false

        photoList
    }

    fun getPhotoById(id: Long): PhotoItem? {
        return _photos.value.find { it.id == id }
    }

    fun openPhotoInputStream(photoItem: PhotoItem): InputStream? {
        return try {
            contentResolver.openInputStream(photoItem.contentUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening photo stream: ${e.message}")
            null
        }
    }

    fun getThumbnailBytes(photoItem: PhotoItem, targetWidth: Int = 320, targetHeight: Int = 320): ByteArray? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(
                    photoItem.contentUri,
                    Size(targetWidth, targetHeight),
                    null
                )
            } else {
                openPhotoInputStream(photoItem)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }

            bitmap?.let {
                val baos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                baos.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for ${photoItem.displayName}: ${e.message}")
            null
        }
    }

    suspend fun streamZipArchive(
        targetAlbum: String?,
        outputStream: OutputStream,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val targetPhotos = if (!targetAlbum.isNullOrBlank() && targetAlbum != "All") {
            _photos.value.filter { it.bucketName.equals(targetAlbum, ignoreCase = true) }
        } else {
            _photos.value
        }

        val zipOut = ZipOutputStream(outputStream)
        val buffer = ByteArray(64 * 1024)
        val total = targetPhotos.size

        for ((index, photo) in targetPhotos.withIndex()) {
            try {
                val entryName = "${photo.bucketName}/${photo.displayName}"
                val zipEntry = ZipEntry(entryName)
                zipEntry.size = photo.sizeBytes
                zipOut.putNextEntry(zipEntry)

                openPhotoInputStream(photo)?.use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        zipOut.write(buffer, 0, bytesRead)
                    }
                }
                zipOut.closeEntry()
                onProgress?.invoke(index + 1, total)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding photo to zip: ${photo.displayName}, error: ${e.message}")
            }
        }
        zipOut.finish()
        zipOut.flush()
    }

    companion object {
        private const val TAG = "GalleryRepository"
    }
}
