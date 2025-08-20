package com.sami.topscanner.model

import android.net.Uri

data class ScannedDocument(
    val type: DocumentFormat,
    val name: String,
    val uri: Uri,
    /**
     * lastModified in milliseconds since epoch (System.currentTimeMillis() compatible)
     */
    val lastModified: Long
)
