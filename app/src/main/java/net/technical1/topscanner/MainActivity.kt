package net.technical1.topscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.technical1.topscanner.manager.DocumentFileManager
import net.technical1.topscanner.manager.DocumentScanner
import net.technical1.topscanner.navigation.AppNavigation
import net.technical1.topscanner.viewModel.ScannerViewModel

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