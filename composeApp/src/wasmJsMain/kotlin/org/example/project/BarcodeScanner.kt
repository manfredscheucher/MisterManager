package org.example.project

import androidx.compose.runtime.Composable

@Composable
actual fun rememberBarcodeScannerLauncher(onResult: (String?) -> Unit): BarcodeScannerLauncher? {
    // Barcode scanning not implemented for WASM
    return null
}

actual class BarcodeScannerLauncher {
    actual fun launch() {
        // Not implemented
    }
}
