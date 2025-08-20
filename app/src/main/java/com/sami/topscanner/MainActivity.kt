package com.sami.topscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sami.topscanner.screens.ScannerScreen
import com.sami.topscanner.manager.DocumentFileManager
import com.sami.topscanner.manager.DocumentScanner
import com.sami.topscanner.screens.ResultScreen
import com.sami.topscanner.viewModel.ScannerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileManager = DocumentFileManager(applicationContext)
        val docScanner = DocumentScanner(this)
        val scannerViewModel = ScannerViewModel(fileManager)

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "scanner") {
                composable("scanner") {
                    ScannerScreen(
                        documentScanner = docScanner,
                        viewModel = scannerViewModel,
                        navController = navController,
                        onScanFinished = { images, pdf ->
                            scannerViewModel.updateScannedImages(images)
                            scannerViewModel.updatePdfUri(pdf)
                            navController.navigate("result")
                        }
                    )
                }

                composable("result") {
                    ResultScreen(
                        scannedImages = scannerViewModel.scannedImages,
                        pdfUri = scannerViewModel.pdfUri,
                        fileManager = fileManager,
                        viewModel = scannerViewModel,
                        navController = navController,
                        onBack = {
                            scannerViewModel.clearScanResults()
                            navController.popBackStack() }
                    )
                }
            }
        }
    }
}