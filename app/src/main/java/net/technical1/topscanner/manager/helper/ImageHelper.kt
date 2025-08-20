package net.technical1.topscanner.manager.helper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import net.technical1.topscanner.constant.StorageFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageHelper(private val context: Context) {

    suspend fun saveImageToDocuments(imageUri: Uri, extension: String, fileName: String?): File? =
        withContext(Dispatchers.IO) {
            try {
                val name = (fileName ?: "scanned_${timestamp()}") + "." + extension
                val mime = if (extension.equals("png", true)) "image/png" else "image/jpeg"
                val relativePath = if (extension.equals("png", true)) {
                    Build.VERSION.SDK_INT.takeIf { it >= Build.VERSION_CODES.Q }?.let {
                        Environment.DIRECTORY_PICTURES + StorageFolders.PNG_SUB
                    }
                } else {
                    Build.VERSION.SDK_INT.takeIf { it >= Build.VERSION_CODES.Q }?.let {
                        Environment.DIRECTORY_PICTURES + StorageFolders.JPEG_SUB
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (relativePath != null) put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val targetUri = context.contentResolver.insert(collection, values) ?: return@withContext null
                context.contentResolver.openInputStream(imageUri).use { input ->
                    context.contentResolver.openOutputStream(targetUri).use { out ->
                        if (input == null || out == null) return@withContext null
                        val bmp = BitmapFactory.decodeStream(input) ?: return@withContext null
                        val format = if (extension.equals("png", true)) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
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
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
