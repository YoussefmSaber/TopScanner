package net.technical1.topscanner.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import net.technical1.topscanner.componant.InputField
import net.technical1.topscanner.model.DocumentFormat
import net.technical1.topscanner.manager.DocumentFileManager
import net.technical1.topscanner.navigation.ResultRoute
import net.technical1.topscanner.navigation.ScannerRoute
import net.technical1.topscanner.ui.theme.Primary
import net.technical1.topscanner.viewModel.ScannerViewModel
import kotlinx.coroutines.launch
import net.technical1.topscanner.R
import java.io.File

@Composable
fun ResultScreen(
    scannedImages: List<Uri>,
    pdfUri: Uri?,
    fileManager: DocumentFileManager,
    viewModel: ScannerViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var docName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var chosenFormat by remember { mutableStateOf(DocumentFormat.PDF) }
    val coroutineScope = rememberCoroutineScope()
    var pageIndex by remember { mutableIntStateOf(0) }

    val effectiveScannedImages = scannedImages.ifEmpty { viewModel.scannedImages }
    val effectivePdfUri = pdfUri ?: viewModel.pdfUri

    // unified items list used for counts / pagination logic:
    val items = remember(effectiveScannedImages, effectivePdfUri) {
        effectiveScannedImages.ifEmpty { listOfNotNull(effectivePdfUri) }
    }
    val totalItems = items.size

    // reset page index when the shown items change
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

    val density = LocalDensity.current
    var rowWidth by remember { mutableStateOf(0.dp) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row
        HeaderRow(
            onBack = onBack,
            onSaveClick = { showSaveDialog = true }
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

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_document)) },
            text = {
                Column {
                    InputField(
                        value = docName,
                        onValueChange = { docName = it },
                        placeholder = stringResource(R.string.file_name_without_extension),
                        leadingIcon = null
                    )
                    Spacer(Modifier.height(8.dp))

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = { expanded = true })
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .onGloballyPositioned {
                                    rowWidth = with(density) { it.size.width.toDp() }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = chosenFormat.name,
                                color = Color.Black
                            )
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Primary
                            )
                        }
                        DropdownMenu(
                            modifier = Modifier.width(rowWidth),
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DocumentFormat.entries.forEach { fmt ->
                                DropdownMenuItem(
                                    onClick = {
                                        chosenFormat = fmt
                                        expanded = false
                                    },
                                    text = {
                                        Text(fmt.name)
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    coroutineScope.launch {
                        viewModel.setLoading(true)
                        val saved: List<File> = try {
                            // use the effective sources for saving as well
                            val sources =
                                effectiveScannedImages.ifEmpty { listOfNotNull(effectivePdfUri) }
                            when (chosenFormat) {
                                DocumentFormat.PDF -> {
                                    if (sources.isNotEmpty() && effectivePdfUri == null) {
                                        // images -> single PDF
                                        fileManager.saveImagesAsPdfToDocuments(sources, docName)
                                            ?.let { listOf(it) }
                                            ?: emptyList()
                                    } else if (effectivePdfUri != null && sources.isNotEmpty()) {
                                        // if sources contains the pdf as uri, copy existing pdf
                                        fileManager.saveExistingPdfToDocuments(
                                            effectivePdfUri,
                                            docName
                                        )
                                            ?.let { listOf(it) } ?: emptyList()
                                    } else emptyList()
                                }

                                DocumentFormat.DOCX -> {
                                    fileManager.saveMultipleImages(
                                        sources,
                                        DocumentFormat.DOCX,
                                        docName
                                    )
                                }

                                DocumentFormat.JPEG -> {
                                    fileManager.saveMultipleImages(
                                        sources,
                                        DocumentFormat.JPEG,
                                        docName
                                    )
                                }

                                DocumentFormat.PNG -> {
                                    fileManager.saveMultipleImages(
                                        sources,
                                        DocumentFormat.PNG,
                                        docName
                                    )
                                }
                            }
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            emptyList()
                        }

                        viewModel.updateSavedFiles(saved)
                        viewModel.setLoading(false)

                        if (saved.isNotEmpty()) {
                            Toast.makeText(
                                context,
                                "Saved ${saved.size} file(s): ${saved.joinToString { it.name }}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                        }
                        navController.navigate(ScannerRoute) {
                            popUpTo(ResultRoute) { inclusive = true } // Adjust this
                        }
                    }
                }) {
                    Text(
                        stringResource(R.string.save),
                        color = Primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                }) { Text(stringResource(R.string.cancel), color = Color.Red) }
            }
        )
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
    onSaveClick: () -> Unit
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
            text = stringResource(R.string.edit),
            style = TextStyle(
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onSaveClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.save),
                style = TextStyle(
                    color = Primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
