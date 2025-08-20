package com.sami.topscanner.manager.helper

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.sami.topscanner.constant.StorageFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.Document
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.collections.forEachIndexed

class DocxHelper(private val context: android.content.Context) {

    suspend fun renderDocxToImages(docUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Uri>()
        try {
            context.contentResolver.openInputStream(docUri)?.use { input ->
                val doc = XWPFDocument(input)
                val seen = HashSet<String>()
                val allPics: List<XWPFPictureData> = doc.allPictures

                allPics.forEachIndexed { idx, pic ->
                    try {
                        val data = pic.data ?: return@forEachIndexed
                        val checksum = sha256Hex(data)
                        if (seen.contains(checksum)) return@forEachIndexed
                        seen.add(checksum)

                        val suggested = pic.suggestFileExtension()?.takeIf { it.isNotBlank() }
                        val ext = suggested ?: when (pic.pictureType) {
                            Document.PICTURE_TYPE_PNG -> "png"
                            Document.PICTURE_TYPE_JPEG -> "jpg"
                            Document.PICTURE_TYPE_GIF -> "gif"
                            Document.PICTURE_TYPE_BMP -> "bmp"
                            else -> "bin"
                        }

                        val fileName = "docx_img_${System.currentTimeMillis()}_${idx}.$ext"
                        val outFile = File(context.cacheDir, fileName)
                        FileOutputStream(outFile).use { fos -> fos.write(data); fos.flush() }

                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(outFile.absolutePath, opts)
                        val valid = opts.outWidth > 0 && opts.outHeight > 0
                        if (!valid) {
                            outFile.delete()
                            return@forEachIndexed
                        }

                        val contentUri = try {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                outFile
                            )
                        } catch (e: IllegalArgumentException) {
                            Uri.fromFile(outFile)
                        }

                        result.add(contentUri)
                    } catch (ie: Exception) {
                        ie.printStackTrace()
                    }
                }
                doc.close()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        result
    }

    // Reuse your existing saveImagesAsDocx function if needed (kept minimal here)
    fun saveImagesAsDocx(imageUris: List<Uri>, fileName: String): File? {
        return try {
            val outputFile = File(context.cacheDir, "$fileName.docx")
            val doc = XWPFDocument()

            val pageWidthEMU = Units.toEMU(8.27 * 72)
            val pageHeightEMU = Units.toEMU(11.69 * 72)
            val marginEMU = Units.toEMU(72.0)

            val maxWidthEMU = pageWidthEMU - marginEMU * 2
            val maxHeightEMU = pageHeightEMU - marginEMU * 2

            imageUris.forEachIndexed { idx, uri ->
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    if (inputStream == null) return@forEachIndexed
                    val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@forEachIndexed

                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                    val bytes = bos.toByteArray()
                    bos.close()

                    val widthEMU = Units.toEMU(bitmap.width.toDouble())
                    val heightEMU = Units.toEMU(bitmap.height.toDouble())

                    val widthScale = maxWidthEMU.toDouble() / widthEMU
                    val heightScale = maxHeightEMU.toDouble() / heightEMU
                    val scale = minOf(widthScale, heightScale, 1.0)

                    val finalWidthEMU = (widthEMU * scale).toInt()
                    val finalHeightEMU = (heightEMU * scale).toInt()

                    val paragraph = doc.createParagraph().apply {
                        alignment = ParagraphAlignment.CENTER
                    }
                    val run = paragraph.createRun()

                    run.addPicture(
                        bytes.inputStream(),
                        Document.PICTURE_TYPE_JPEG,
                        "image_$idx.jpg",
                        finalWidthEMU,
                        finalHeightEMU
                    )

                    doc.createParagraph()
                }
            }

            FileOutputStream(outputFile).use { fos -> doc.write(fos) }
            doc.close()

            val name = outputFile.name
            val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Environment.DIRECTORY_DOCUMENTS + StorageFolders.DOCX_SUB
            } else null

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
                if (relativePath != null) put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val targetUri = context.contentResolver.insert(collection, values)
            if (targetUri != null) {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    Files.newInputStream(outputFile.toPath()).use { input ->
                        input.copyTo(out)
                    }
                }
            }

            outputFile
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
