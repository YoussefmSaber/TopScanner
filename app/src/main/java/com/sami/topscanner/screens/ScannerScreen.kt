package com.sami.topscanner.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sami.topscanner.R
import com.sami.topscanner.componant.DocumentItem
import com.sami.topscanner.componant.InputField
import com.sami.topscanner.manager.DocumentScanner
import com.sami.topscanner.ui.theme.Primary
import com.sami.topscanner.viewModel.ScannerViewModel

@Composable
fun ScannerScreen(
    documentScanner: DocumentScanner,
    onScanFinished: (List<Uri>, Uri?) -> Unit,
    viewModel: ScannerViewModel,
    navController: NavController
) {
    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            documentScanner.handleScanResult(
                result.data,
                onImagesScanned = { uris ->
                    viewModel.updateScannedImages(uris)
                },
                onPdfGenerated = { pdfUri ->
                    viewModel.updatePdfUri(pdfUri)
                }
            )

            onScanFinished(viewModel.scannedImages, viewModel.pdfUri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDocuments()
    }

    val document by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.search.collectAsStateWithLifecycle()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Start)
                .padding(16.dp),
            text = stringResource(R.string.app_name),
            style = TextStyle(
                color = Primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        InputField(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = searchQuery,
            onValueChange = viewModel::search,
            placeholder = stringResource(R.string.search_document)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(document) { document ->
                DocumentItem(
                    modifier = Modifier.animateItem(tween(200))
                        .clickable(onClick = {
                            viewModel.selectDocument(document)
                            viewModel.loadDocumentForPreview()
                            navController.navigate("result")
                        }),
                    document = document
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .clickable {
                    documentScanner.startScanning(activityLauncher)
                }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = ImageVector.vectorResource(R.drawable.camera),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.scan_doc),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                )
            )
        }
    }
}