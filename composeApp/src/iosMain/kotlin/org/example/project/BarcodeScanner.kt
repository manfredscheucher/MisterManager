package org.example.project

import androidx.compose.runtime.Composable

@Composable
actual fun rememberBarcodeScannerLauncher(onResult: (String?) -> Unit): BarcodeScannerLauncher? {
    // TODO: Implement iOS barcode scanning with AVFoundation or VisionKit
    // For now, returning null - will be implemented when iOS support is enabled
    return null
}

actual class BarcodeScannerLauncher {
    actual fun launch() {
        // TODO: Implement iOS barcode scanning
    }
}
