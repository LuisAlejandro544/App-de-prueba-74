package com.example.util

import android.content.Context
import java.io.File

object FileHelper {
    
    fun shareFile(context: Context, filePath: String) {
        val fileSharer = FileSharer(context)
        fileSharer.share(filePath)
    }

    fun exportFileToDownloads(context: Context, filePath: String): String? {
        val fileExporter = FileExporter(context)
        return fileExporter.exportToDownloads(filePath)
    }

    fun convertFileToBase64(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
