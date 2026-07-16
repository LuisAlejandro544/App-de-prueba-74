package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore

@Composable
fun ConverterTab() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var targetFormat by remember { mutableStateOf("PKCS12 (.keystore)") }

    var conversionSuccess by remember { mutableStateOf(false) }
    var conversionMsg by remember { mutableStateOf("") }
    var isConverting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileUri = uri
            fileName = getFileNameFromUri(context, uri) ?: "keystore"
            if (fileName.endsWith(".jks", ignoreCase = true)) {
                targetFormat = "PKCS12 (.keystore)"
            } else if (fileName.endsWith(".keystore", ignoreCase = true) || fileName.endsWith(".p12", ignoreCase = true)) {
                targetFormat = "JKS (.jks)"
            }
            conversionSuccess = false
            conversionMsg = ""
            
            // Proactively copy the chosen URI in the background immediately
            coroutineScope.launch {
                val tempFile = withContext(Dispatchers.IO) {
                    copyUriToTempFile(context, uri, "temp_convert_in")
                }
                if (tempFile == null) {
                    conversionMsg = "No se pudo leer el archivo de firmas seleccionado de forma inmediata."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Conversor de Formatos de Firma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Convierte fácilmente un archivo de firmas de formato JKS a PKCS12 (estándar .keystore) o viceversa para su uso en diferentes flujos de compilación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConverting
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fileName.isEmpty()) "Seleccionar archivo original" else "Archivo: $fileName")
                }

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Contraseña Actual de Keystore") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConverting
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva Contraseña de Salida") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConverting
                )

                Text(
                    text = "Formato Objetivo: $targetFormat",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        val uri = fileUri
                        if (uri == null) {
                            conversionMsg = "Seleccione un archivo Keystore primero."
                            conversionSuccess = false
                            return@Button
                        }
                        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                            conversionMsg = "Las contraseñas no pueden estar vacías."
                            conversionSuccess = false
                            return@Button
                        }

                        isConverting = true
                        conversionMsg = ""

                        coroutineScope.launch {
                            val tempIn = File(context.cacheDir, "temp_convert_in")
                            if (!tempIn.exists()) {
                                conversionMsg = "El archivo temporal no está listo. Intente seleccionarlo de nuevo."
                                conversionSuccess = false
                                isConverting = false
                                return@launch
                            }

                            val outExt = if (targetFormat.startsWith("JKS")) ".jks" else ".keystore"
                            val outName = fileName.substringBeforeLast(".") + "_convertido" + outExt
                            val tempOutFile = File(context.cacheDir, outName)

                            val success = withContext(Dispatchers.IO) {
                                convertKeystoreFormat(
                                    tempIn,
                                    currentPassword,
                                    tempOutFile,
                                    newPassword,
                                    targetFormat.startsWith("JKS")
                                )
                            }

                            isConverting = false
                            if (success) {
                                conversionSuccess = true
                                conversionMsg = "Keystore convertido con éxito."
                                FileHelper.shareFile(context, tempOutFile.absolutePath)
                            } else {
                                conversionSuccess = false
                                conversionMsg = "Error en la conversión. Verifique la contraseña y la integridad del Keystore."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = fileUri != null && !isConverting
                ) {
                    if (isConverting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Transform, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convertir y Compartir")
                    }
                }
            }
        }

        if (conversionMsg.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (conversionSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = conversionMsg,
                    color = if (conversionSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun convertKeystoreFormat(
    inFile: File,
    inPass: String,
    outFile: File,
    outPass: String,
    toJKS: Boolean
): Boolean {
    return try {
        // Ensure BouncyCastle security provider is registered
        try {
            com.example.security.BouncyCastleManager.setupBouncyCastle()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        val inType = if (toJKS) "PKCS12" else "JKS"
        val outType = if (toJKS) "JKS" else "PKCS12"

        // Try getting KeyStore instance with default provider first, fallback to BouncyCastle
        val inKs = try {
            KeyStore.getInstance(inType)
        } catch (e: Exception) {
            KeyStore.getInstance(inType, "BC")
        }

        FileInputStream(inFile).use { fis ->
            inKs.load(fis, inPass.toCharArray())
        }

        val outKs = try {
            KeyStore.getInstance(outType)
        } catch (e: Exception) {
            KeyStore.getInstance(outType, "BC")
        }
        outKs.load(null, outPass.toCharArray())

        val aliases = inKs.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            if (inKs.isKeyEntry(alias)) {
                val key = inKs.getKey(alias, inPass.toCharArray())
                val chain = inKs.getCertificateChain(alias)
                outKs.setKeyEntry(alias, key, outPass.toCharArray(), chain)
            } else if (inKs.isCertificateEntry(alias)) {
                val cert = inKs.getCertificate(alias)
                outKs.setCertificateEntry(alias, cert)
            }
        }

        FileOutputStream(outFile).use { fos ->
            outKs.store(fos, outPass.toCharArray())
        }
        true
    } catch (e: Throwable) {
        e.printStackTrace()
        false
    }
}
