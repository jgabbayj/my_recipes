package com.morsel.recipes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ImageCache {
    private const val TAG = "ImageCache"
    private const val DIRECTORY_NAME = "recipe_images"

    private fun getImagesDir(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    internal fun getFileName(url: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(url.toByteArray())
            bytes.joinToString("") { "%02x".format(it) } + ".jpg"
        } catch (e: Exception) {
            // Fallback to sanitizing URL
            url.replace(Regex("[^a-zA-Z0-9]"), "_") + ".jpg"
        }
    }

    /**
     * Tries to decode a cached local image if it exists.
     * Returns null if not cached or decoding fails.
     */
    fun getCached(context: Context, url: String): Bitmap? {
        if (!url.startsWith("http")) return null
        val localFile = File(getImagesDir(context), getFileName(url))
        if (localFile.exists() && localFile.length() > 0) {
            return try {
                BitmapFactory.decodeFile(localFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding cached file: ${localFile.absolutePath}", e)
                null
            }
        }
        return null
    }

    /**
     * Retrieves the image from cache or downloads it from the network if not cached.
     * Saves the downloaded image to disk for future fast access.
     */
    suspend fun getOrDownload(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null
        
        // If it's a local file, decode directly
        if (!url.startsWith("http")) {
            return@withContext try {
                val file = File(url)
                if (file.exists() && file.isFile) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding local file path: $url", e)
                null
            }
        }

        val localFile = File(getImagesDir(context), getFileName(url))
        
        // Check cache first
        if (localFile.exists() && localFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                if (bitmap != null) {
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed decoding cached file, will redownload", e)
            }
        }

        // Download and cache it
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.connect()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                // Download to a temporary file first to prevent half-downloaded file corruption
                val tempFile = File.createTempFile("img_dl", null, context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                
                // Verify the downloaded file is a valid image
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                if (bitmap != null) {
                    if (tempFile.renameTo(localFile)) {
                        return@withContext bitmap
                    } else {
                        // Fallback copy if rename fails
                        tempFile.copyTo(localFile, overwrite = true)
                        tempFile.delete()
                        return@withContext bitmap
                    }
                } else {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image from: $url", e)
        }
        
        return@withContext null
    }

    /**
     * Download and cache multiple images asynchronously.
     */
    suspend fun preCacheImages(context: Context, urls: List<String>) = withContext(Dispatchers.IO) {
        val uniqueUrls = urls.filter { it.isNotEmpty() && it.startsWith("http") }.distinct()
        for (url in uniqueUrls) {
            try {
                getOrDownload(context, url)
            } catch (e: Exception) {
                Log.e(TAG, "Error pre-caching image: $url", e)
            }
        }
    }

    /**
     * Cleans up local image files that are no longer referenced by any recipe.
     */
    fun deleteUnusedImages(context: Context, activeUrls: List<String>) {
        try {
            val dir = File(context.filesDir, DIRECTORY_NAME)
            if (!dir.exists()) return
            val activeFiles = activeUrls.filter { it.isNotEmpty() }.map { getFileName(it) }.toSet()
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isFile && !activeFiles.contains(file.name)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting unused images", e)
        }
    }
}
