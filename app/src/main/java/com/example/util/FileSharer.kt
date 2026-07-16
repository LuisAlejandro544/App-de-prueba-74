package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class FileSharer(private val context: Context) {
    companion object {
        private const val TAG = "FileSharer"
        private const val PROVIDER_AUTHORITY = "com.aistudio.keystoregen.vuxwt.fileprovider"
    }

    fun share(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "El archivo de la llave no existe o fue eliminado.", Toast.LENGTH_SHORT).show()
            }
            Log.e(TAG, "File does not exist at $filePath")
            return
        }

        try {
            val fileUri: Uri = FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Llave de Firma: ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Aquí tienes tu Keystore de firma generado con Generador de Keystores.\n\nAlias: (revisa la app para contraseñas)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Enviar Keystore").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Handler(Looper.getMainLooper()).post {
                try {
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting share chooser activity", e)
                    Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
