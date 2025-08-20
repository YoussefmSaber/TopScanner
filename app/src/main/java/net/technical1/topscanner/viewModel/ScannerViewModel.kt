package net.technical1.topscanner.viewModel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.technical1.topscanner.manager.DocumentFileManager
import net.technical1.topscanner.model.DocumentFormat
import net.technical1.topscanner.model.ScannedDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.technical1.topscanner.R
import java.io.File

class ScannerViewModel(
    private val fileManager: DocumentFileManager
) : ViewModel() {

    var scannedImages by mutableStateOf<List<Uri>>(emptyList())
        private set

    var pdfUri by mutableStateOf<Uri?>(null)
        private set

    var savedFiles by mutableStateOf<List<File>>(emptyList())
    var isLoading = mutableStateOf(false)
    var isDocumentLoaded = mutableStateOf(false)

    val _selectedDocument = MutableStateFlow<ScannedDocument>(
        ScannedDocument(
            type = DocumentFormat.PDF,
            "",
            Uri.EMPTY,
            0
        )
    )

    private val _allDocuments = MutableStateFlow<List<ScannedDocument>>(emptyList())

    private val _filteredDocuments = MutableStateFlow<List<ScannedDocument>>(emptyList())
    val filteredDocuments: StateFlow<List<ScannedDocument>> = _filteredDocuments
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    fun loadDocuments() {
        isDocumentLoaded.value = false
        viewModelScope.launch {
            val docs = fileManager.getAllScannedDocuments()
            _allDocuments.value = docs
            _filteredDocuments.value = docs
            isDocumentLoaded.value = true
        }
    }

    fun loadDocumentForPreview() {
        isDocumentLoaded.value = false

        scannedImages = emptyList()
        viewModelScope.launch {
            when (_selectedDocument.value.type) {
                DocumentFormat.JPEG, DocumentFormat.PNG -> {
                    scannedImages = listOf(_selectedDocument.value.uri)
                    isDocumentLoaded.value = true

                }
                DocumentFormat.PDF -> {
                    scannedImages = fileManager.renderPdfToImages(_selectedDocument.value.uri)
                    isDocumentLoaded.value = true

                }
                DocumentFormat.DOCX -> {
                    scannedImages = fileManager.renderDocxToImages(_selectedDocument.value.uri)
                    isDocumentLoaded.value = true
                }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _search.value = ""
            _filteredDocuments.value = _allDocuments.value
        } else {
            _search.value = query
            _filteredDocuments.value = _allDocuments.value.filter { doc ->
                doc.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun selectDocument(document: ScannedDocument) {
        _selectedDocument.value = document
    }

    fun updateScannedImages(images: List<Uri>) {
        scannedImages = images
    }

    fun updatePdfUri(uri: Uri?) {
        pdfUri = uri
    }

    fun updateSavedFiles(files: List<File>) {
        savedFiles = files
    }

    fun setLoading(loading: Boolean) {
        isLoading.value = loading
    }

    fun clearScanResults() {
        scannedImages = emptyList()
        pdfUri = null
        savedFiles = emptyList()
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_via))
        context.startActivity(chooser)
    }
}