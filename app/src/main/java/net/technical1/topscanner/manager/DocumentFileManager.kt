package net.technical1.topscanner.manager

import android.content.Context
import android.net.Uri
import net.technical1.topscanner.model.DocumentFormat
import net.technical1.topscanner.model.ScannedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import net.technical1.topscanner.manager.helper.DocxHelper
import net.technical1.topscanner.manager.helper.ImageHelper
import net.technical1.topscanner.manager.helper.MediaStoreHelper
import net.technical1.topscanner.manager.helper.PdfHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DocumentFileManager(private val context: Context) {

    // helpers
    private val pdfHelper = PdfHelper(context)
    private val docxHelper = DocxHelper(context)
    private val imageHelper = ImageHelper(context)
    private val mediaStoreHelper = MediaStoreHelper(context)

    // Expose the same public API you had; delegate to helpers.

    suspend fun saveExistingPdfToDocuments(sourceUri: Uri, fileName: String?): File? =
        pdfHelper.saveExistingPdfToDocuments(sourceUri, fileName)

    suspend fun saveImagesAsPdfToDocuments(imageUris: List<Uri>, fileName: String?): File? =
        pdfHelper.saveImagesAsPdfToDocuments(imageUris, fileName)

    suspend fun saveImageToDocuments(imageUri: Uri, extension: String, fileName: String?): File? =
        imageHelper.saveImageToDocuments(imageUri, extension, fileName)

    suspend fun saveMultipleImages(imageUris: List<Uri>, format: DocumentFormat, fileName: String?): List<File> =
        withContext(Dispatchers.IO) {
            when (format) {
                DocumentFormat.PDF -> {
                    pdfHelper.saveImagesAsPdfToDocuments(imageUris, fileName)?.let { listOf(it) } ?: emptyList()
                }
                DocumentFormat.JPEG -> {
                    imageUris.mapIndexedNotNull { idx, uri -> imageHelper.saveImageToDocuments(uri, "jpg", (fileName ?: "scanned") + "_${idx + 1}") }
                }
                DocumentFormat.PNG -> {
                    imageUris.mapIndexedNotNull { idx, uri -> imageHelper.saveImageToDocuments(uri, "png", (fileName ?: "scanned") + "_${idx + 1}") }
                }
                DocumentFormat.DOCX -> {
                    // keep your old behavior: create docx using POI (original code can be reused here if desired)
                    saveImagesAsDocx(imageUris, fileName ?: "scanned_${timestamp()}")
                        ?.let { listOf(it) } ?: emptyList()
                }
            }
        }

    // If you want to keep original saveImagesAsDocx code inline, paste it here (using docxHelper or implement directly).
    fun saveImagesAsDocx(imageUris: List<Uri>, fileName: String): File? {
        // You can delegate to your previous implementation or move the implementation here.
        // For now, call docxHelper.saveImagesAsDocx if implemented there.
        return docxHelper.saveImagesAsDocx(imageUris, fileName)
    }

    suspend fun getAllScannedDocuments(): List<ScannedDocument> =
        mediaStoreHelper.getAllScannedDocuments()

    suspend fun renderDocxToImages(docUri: Uri): List<Uri> =
        docxHelper.renderDocxToImages(docUri)

    suspend fun renderPdfToImages(pdfUri: Uri): List<Uri> =
        pdfHelper.renderPdfToImages(pdfUri)

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}