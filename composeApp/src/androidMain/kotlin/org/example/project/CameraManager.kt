package org.example.project

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Composable
actual fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): CameraLauncher? {
    val context = getContext() as Context
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempUri?.let { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    onResult(bytes)
                } catch (e: Exception) {
                    GlobalScope.launch {
                        Logger.log(LogLevel.ERROR, "Failed to read camera image: ${e.message}")
                    }
                    onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }

    return remember {
        CameraLauncher(launcher, context) { tempUri = it }
    }
}

actual class CameraLauncher(
    private val launcher: androidx.activity.result.ActivityResultLauncher<Uri>,
    private val context: Context,
    private val onUriCreated: (Uri) -> Unit
) {
    actual fun launch() {
        try {
            val directory = File(context.cacheDir, "images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "temp_camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            onUriCreated(uri)
            launcher.launch(uri)
        } catch (e: Exception) {
            GlobalScope.launch {
                Logger.log(LogLevel.ERROR, "Failed to launch camera: ${e.message}")
            }
        }
    }
}
