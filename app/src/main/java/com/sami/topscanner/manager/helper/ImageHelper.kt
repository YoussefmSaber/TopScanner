package com.sami.topscanner.manager.helper

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.sami.topscanner.constant.StorageFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageHelper(private val context: android.content.Context) {

    suspend fun saveImageToDocuments(imageUri: Uri, extension: String, fileName: String?): File? =
        withContext(Dispatchers.IO) {
            try {
                val name = (fileName ?: "scanned_${timestamp()}") + "." + extension
                val mime = if (extension.equals("png", true)) "image/png" else "image/jpeg"
                val relativePath = if (extension.equals("png", true)) {
                    android.os.Build.VERSION.SDK_INT.takeIf { it >= android.os.Build.VERSION_CODES.Q }?.let {
                        android.os.Environment.DIRECTORY_PICTURES + StorageFolders.PNG_SUB
                    }
                } else {
                    android.os.Build.VERSION.SDK_INT.takeIf { it >= android.os.Build.VERSION_CODES.Q }?.let {
                        android.os.Environment.DIRECTORY_PICTURES + StorageFolders.JPEG_SUB
                    }
                }

                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (relativePath != null) put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }

                val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val targetUri = context.contentResolver.insert(collection, values) ?: return@withContext null
                context.contentResolver.openInputStream(imageUri).use { input ->
                    context.contentResolver.openOutputStream(targetUri).use { out ->
                        if (input == null || out == null) return@withContext null
                        val bmp = BitmapFactory.decodeStream(input) ?: return@withContext null
                        val format = if (extension.equals("png", true)) android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG
                        bmp.compress(format, 95, out)
                        out.flush()
                    }
                }

                val outFile = File(context.cacheDir, name)
                context.contentResolver.openInputStream(targetUri).use { input ->
                    FileOutputStream(outFile).use { fos -> input?.copyTo(fos) }
                }
                return@withContext outFile
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }
        }

    fun saveBytesAsTempImage(bytes: ByteArray, ext: String = "jpg"): Uri? {
        return try {
            val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.$ext")
            FileOutputStream(file).use { fos -> fos.write(bytes); fos.flush() }
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (e: IllegalArgumentException) {
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun timestamp(): String =
        java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
}
