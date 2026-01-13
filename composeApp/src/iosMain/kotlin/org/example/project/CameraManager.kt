package org.example.project

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): CameraLauncher? {
    // TODO: Implement iOS camera with UIImagePickerController
    // For now, returning null - will be implemented when iOS support is enabled
    return null
}

actual class CameraLauncher {
    actual fun launch() {
        // TODO: Implement iOS camera
    }
}
