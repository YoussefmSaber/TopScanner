package com.sami.topscanner.manager.helper

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.sami.topscanner.constant.StorageFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.forEachIndexed
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class PdfHelper(private val context: android.content.Context) {

    suspend fun renderPdfToImages(pdfUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Uri>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd == null) return@withContext result

            renderer = PdfRenderer(pfd)

            val maxDim = 2048 // cap to avoid OOM

            for (i in 0 until renderer.pageCount) {
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(i)

                    val pageWidth = page.width
                    val pageHeight = page.height
                    val scale = if (maxOf(pageWidth, pageHeight) > maxDim) {
                        maxDim.toFloat() / maxOf(pageWidth, pageHeight).toFloat()
                    } else 1f

                    val bmpWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
                    val bmpHeight = (pageHeight * scale).toInt().coerceAtLeast(1)

                    val bitmap = createBitmap(bmpWidth, bmpHeight)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.White.value.toInt())

                    if (scale == 1f) {
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    } else {
                        val temp = createBitmap(pageWidth, pageHeight)
                        val tempCanvas = Canvas(temp)
                        tempCanvas.drawColor(Color.White.value.toInt())
                        page.render(temp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val m = Matrix().apply { setScale(scale, scale) }
                        val scaled = Bitmap.createBitmap(temp, 0, 0, temp.width, temp.height, m, true)
                        canvas.drawBitmap(scaled, 0f, 0f, null)

                        scaled.recycle()
                        temp.recycle()
                    }

                    val fileName = "pdf_page_${System.currentTimeMillis()}_$i.png"
                    val outFile = File(context.cacheDir, fileName)
                    FileOutputStream(outFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                    }

                    bitmap.recycle()

                    val contentUri = try {
                        FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
                    } catch (e: IllegalArgumentException) {
                        Uri.fromFile(outFile)
                    }

                    result.add(contentUri)
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    page?.close()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
        result
    }

    suspend fun saveExistingPdfToDocuments(sourceUri: Uri, fileName: String?): File? = withContext(
        Dispatchers.IO) {
        try {
            val name = (fileName ?: "scanned_${timestamp()}") + ".pdf"
            val mime = "application/pdf"
            val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Environment.DIRECTORY_DOCUMENTS + StorageFolders.PDF_SUB
            } else null

            val values = ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime)
                if (relativePath != null) put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Files.getContentUri("external")
            }

            val targetUri = context.contentResolver.insert(collection, values) ?: return@withContext null

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    input.copyTo(out)
                }
            }

            return@withContext File(context.cacheDir, name).apply {
                context.contentResolver.openInputStream(targetUri)?.use { input ->
                    FileOutputStream(this).use { out -> input.copyTo(out) }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    suspend fun saveImagesAsPdfToDocuments(imageUris: List<Uri>, fileName: String?): File? = withContext(
        Dispatchers.IO) {
        if (imageUris.isEmpty()) return@withContext null
        try {
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            imageUris.forEachIndexed { idx, uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return@forEachIndexed
                    val scale = minOf(pageWidth.toFloat() / originalBitmap.width, pageHeight.toFloat() / originalBitmap.height)
                    val scaledWidth = (originalBitmap.width * scale).toInt()
                    val scaledHeight = (originalBitmap.height * scale).toInt()
                    val scaledBitmap = originalBitmap.scale(scaledWidth, scaledHeight)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                    val page = pdf.startPage(pageInfo)
                    val left = (pageWidth - scaledWidth) / 2
                    val top = (pageHeight - scaledHeight) / 2
                    page.canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)
                    pdf.finishPage(page)

                    originalBitmap.recycle()
                    scaledBitmap.recycle()
                }
            }

            val outStream = java.io.ByteArrayOutputStream()
            pdf.writeTo(outStream)
            pdf.close()
            val pdfBytes = outStream.toByteArray()
            outStream.close()

            val name = (fileName ?: "scanned_${timestamp()}") + ".pdf"
            val mime = "application/pdf"
            val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Environment.DIRECTORY_DOCUMENTS + StorageFolders.PDF_SUB
            } else null

            val values = ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime)
                if (relativePath != null) put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Files.getContentUri("external")
            }

            val targetUri = context.contentResolver.insert(collection, values) ?: return@withContext null
            context.contentResolver.openOutputStream(targetUri)?.use { out -> out.write(pdfBytes); out.flush() }

            val outFile = File(context.cacheDir, name)
            FileOutputStream(outFile).use { fos -> fos.write(pdfBytes) }
            outFile
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun timestamp(): String =
        java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
}