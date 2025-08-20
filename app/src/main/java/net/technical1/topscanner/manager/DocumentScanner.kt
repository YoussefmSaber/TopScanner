package net.technical1.topscanner.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class DocumentScanner(private val context: Context) {

    private val scanner = GmsDocumentScanning.getClient(getScannerOptions())

    private fun getScannerOptions(): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()
    }

    fun startScanning(scannerLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val activity = context as? Activity ?: return
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Scanner failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun handleScanResult(
        resultIntent: Intent?,
        onImagesScanned: (List<Uri>) -> Unit,
        onPdfGenerated: (Uri) -> Unit
    ) {
        // Use provided helper to parse the result
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(resultIntent)
        val imageUris = scanResult?.pages?.mapNotNull { it.imageUri } ?: emptyList()
        onImagesScanned(imageUris)
        scanResult?.pdf?.let { pdfObj -> onPdfGenerated(pdfObj.uri) }
    }
}
