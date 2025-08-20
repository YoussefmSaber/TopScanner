package com.sami.topscanner.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sami.topscanner.R
import com.sami.topscanner.model.DocumentFormat
import com.sami.topscanner.ui.theme.Primary
import com.sami.topscanner.viewModel.ScannerViewModel

@Composable
fun DetailsScreen(
    scannedImages: List<Uri>,
    pdfUri: Uri?,
    viewModel: ScannerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pageIndex by remember { mutableIntStateOf(0) }

    val effectiveScannedImages = scannedImages.ifEmpty { viewModel.scannedImages }
    val effectivePdfUri = pdfUri ?: viewModel.pdfUri

    val items = remember(effectiveScannedImages, effectivePdfUri) {
        effectiveScannedImages.ifEmpty { listOfNotNull(effectivePdfUri) }
    }
    val totalItems = items.size

    LaunchedEffect(items) {
        pageIndex = 0
    }

    val animateNextColor by animateColorAsState(
        targetValue = if (pageIndex < totalItems - 1) Color(0xFF01678f) else Color(0xFF7d8091),
        animationSpec = tween(300)
    )

    val animatePrevColor by animateColorAsState(
        targetValue = if (pageIndex > 0) Color(0xFF9c3e52) else Color(0xFF7d8091),
        animationSpec = tween(300)
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row
        HeaderRow(
            onBack = onBack,
            onShareClick = {
                val selectedDoc = viewModel._selectedDocument.value
                val uri = selectedDoc.uri

                // pick MIME type based on format
                val mimeType = when (selectedDoc.type) {
                    DocumentFormat.PDF -> "application/pdf"
                    DocumentFormat.JPEG -> "image/jpeg"
                    DocumentFormat.PNG -> "image/png"
                    DocumentFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                }

                viewModel.shareFile(context, uri, mimeType)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(text = "${pageIndex + 1}/${totalItems.coerceAtLeast(1)}")

        Box(
            Modifier
                .fillMaxWidth(0.8f)
                .height(375.dp)
                .background(Color.Black.copy(alpha = 0.05f))
                .padding(24.dp)
        ) {
            if (viewModel.isDocumentLoaded.value) {
                if (effectiveScannedImages.isNotEmpty()) {
                    AsyncImage(
                        model = effectiveScannedImages.getOrNull(pageIndex),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (effectivePdfUri != null) {
                    Text(stringResource(R.string.pdf_generated_open_it_with_a_pdf_viewer_from_files_app))
                } else {
                    Text(stringResource(R.string.nothing_to_preview))
                }

            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        onClick = { if (pageIndex > 0) pageIndex-- },
                        enabled = pageIndex > 0
                    )
                    .background(animatePrevColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.prev),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    )
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        onClick = { if (pageIndex < totalItems - 1) pageIndex++ },
                        enabled = pageIndex < totalItems - 1
                    )
                    .background(animateNextColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.next),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(2f))
    }

    if (viewModel.isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun HeaderRow(
    onBack: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                spotColor = Color.Black.copy(0.15f)
            )
            .background(Color.White)
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = null,
                tint = Primary
            )
        }

        Text(
            text = stringResource(R.string.details),
            style = TextStyle(
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onShareClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.share),
                style = TextStyle(
                    color = Primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}