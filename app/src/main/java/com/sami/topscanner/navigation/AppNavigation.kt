package com.sami.topscanner.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sami.topscanner.manager.DocumentFileManager
import com.sami.topscanner.manager.DocumentScanner
import com.sami.topscanner.screens.DetailsScreen
import com.sami.topscanner.screens.ResultScreen
import com.sami.topscanner.screens.ScannerScreen
import com.sami.topscanner.viewModel.ScannerViewModel

@Composable
fun AppNavigation(
    docScanner: DocumentScanner,
    scannerViewModel: ScannerViewModel,
    fileManager: DocumentFileManager
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ScannerRoute) {
        composable<ScannerRoute> {
            ScannerScreen(
                documentScanner = docScanner,
                viewModel = scannerViewModel,
                navController = navController,
                onScanFinished = { images, pdf ->
                    scannerViewModel.updateScannedImages(images)
                    scannerViewModel.updatePdfUri(pdf)
                    navController.navigate(ResultRoute)
                }
            )
        }

        composable<ResultRoute> {
            ResultScreen(
                scannedImages = scannerViewModel.scannedImages,
                pdfUri = scannerViewModel.pdfUri,
                fileManager = fileManager,
                viewModel = scannerViewModel,
                navController = navController,
                onBack = {
                    scannerViewModel.clearScanResults()
                    navController.popBackStack()
                }
            )
        }

        composable<DetailsRoute> {
            DetailsScreen(
                scannedImages = scannerViewModel.scannedImages,
                pdfUri = scannerViewModel.pdfUri,
                viewModel = scannerViewModel,
                onBack = {
                    scannerViewModel.clearScanResults()
                    navController.popBackStack()
                }
            )
        }
    }
}