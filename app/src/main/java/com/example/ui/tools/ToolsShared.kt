package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import java.io.FileOutputStream

@Composable
fun TextValueRow(label: String, value: String, isCode: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = if (isCode) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

data class InspectDetails(
    val algorithm: String,
    val keySize: Int,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validUntil: String,
    val sha256: String,
    val sha1: String,
    val validityYears: Int
)

data class ApkVerifyResult(
    val isSigned: Boolean,
    val sigAlgName: String,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validUntil: String,
    val sha256: String,
    val sha1: String
)

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

fun copyUriToTempFile(context: Context, uri: Uri, tempName: String): File? {
    return try {
        val tempFile = File(context.cacheDir, tempName)
        if (tempFile.exists()) tempFile.delete()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
    return try {
        if (destFile.exists()) destFile.delete()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
