package org.example.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
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
                    if (bytes != null) {
                        val rotatedBytes = rotateImageIfRequired(context, bytes, uri)
                        onResult(rotatedBytes)
                    } else {
                        onResult(null)
                    }
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

private fun rotateImageIfRequired(context: Context, bytes: ByteArray, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return bytes
    val exif = ExifInterface(inputStream)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
    
    if (rotation == 0) return bytes
    
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    
    val outputStream = ByteArrayOutputStream()
    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    return outputStream.toByteArray()
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
