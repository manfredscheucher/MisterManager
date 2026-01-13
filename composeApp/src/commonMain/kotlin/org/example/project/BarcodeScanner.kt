package org.example.project

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBarcodeScannerLauncher(onResult: (String?) -> Unit): BarcodeScannerLauncher?

expect class BarcodeScannerLauncher {
    fun launch()
}
