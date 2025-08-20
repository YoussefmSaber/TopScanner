package com.sami.topscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sami.topscanner.manager.DocumentFileManager
import com.sami.topscanner.manager.DocumentScanner
import com.sami.topscanner.navigation.AppNavigation
import com.sami.topscanner.viewModel.ScannerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileManager = DocumentFileManager(applicationContext)
        val docScanner = DocumentScanner(this)
        val scannerViewModel = ScannerViewModel(fileManager)

        setContent {

            AppNavigation(docScanner, scannerViewModel, fileManager)
        }
    }
}