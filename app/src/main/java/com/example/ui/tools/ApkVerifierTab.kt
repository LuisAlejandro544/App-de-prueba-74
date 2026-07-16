package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.JarFile

@Composable
fun ApkVerifierTab() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var apkUri by remember { mutableStateOf<Uri?>(null) }
    var apkName by remember { mutableStateOf("") }

    var isVerifying by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<ApkVerifyResult?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            apkUri = uri
            apkName = getFileNameFromUri(context, uri) ?: "apk_file"
            verifyResult = null
            errorMessage = ""
            
            // Proactively copy the file to a temp input in the background
            coroutineScope.launch {
                val tempFile = withContext(Dispatchers.IO) {
                    copyUriToTempFile(context, uri, "temp_verify_source.apk")
                }
                if (tempFile == null) {
                    errorMessage = "No se pudo leer el archivo APK seleccionado de forma inmediata."
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
                    text = "Verificador de Firmas de APK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Sube un archivo APK para extraer y verificar programáticamente su firma digital, huellas SHA-256/SHA-1 y detalles del certificado emisor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = { filePicker.launch("application/vnd.android.package-archive") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isVerifying
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (apkName.isEmpty()) "Seleccionar archivo APK" else "APK: $apkName")
                }

                Button(
                    onClick = {
                        val uri = apkUri
                        if (uri == null) {
                            errorMessage = "Seleccione un archivo APK primero."
                            return@Button
                        }
                        isVerifying = true
                        errorMessage = ""
                        verifyResult = null

                        coroutineScope.launch {
                            val tempFile = File(context.cacheDir, "temp_verify_source.apk")
                            if (!tempFile.exists()) {
                                errorMessage = "El archivo temporal no está listo. Intente seleccionarlo de nuevo."
                                isVerifying = false
                                return@launch
                            }

                            val result = withContext(Dispatchers.IO) {
                                verifyApkSignature(tempFile)
                            }
                            
                            isVerifying = false
                            if (result != null) {
                                verifyResult = result
                            } else {
                                errorMessage = "El APK seleccionado no posee firmas válidas o está dañado."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = apkUri != null && !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        Text("Verificar Firma de APK")
                    }
                }
            }
        }

        AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        AnimatedVisibility(visible = verifyResult != null) {
            verifyResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (result.isSigned) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (result.isSigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (result.isSigned) "APK Firmado Exitosamente" else "APK No Firmado / Firma Inválida",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (result.isSigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        if (result.isSigned) {
                            TextValueRow("Algoritmo de Firma", result.sigAlgName)
                            TextValueRow("Sujeto (Propietario)", result.subject)
                            TextValueRow("Emisor", result.issuer)
                            TextValueRow("Válido Desde", result.validFrom)
                            TextValueRow("Válido Hasta", result.validUntil)
                            TextValueRow("Huella SHA-256", result.sha256, isCode = true)
                            TextValueRow("Huella SHA-1", result.sha1, isCode = true)
                        }
                    }
                }
            }
        }
    }
}

private fun verifyApkSignature(apkFile: File): ApkVerifyResult? {
    var jarFile: JarFile? = null
    try {
        jarFile = JarFile(apkFile)
        val manifestEntry = jarFile.getJarEntry("AndroidManifest.xml") ?: return null

        // Consume stream to trigger certificate verification
        val buffer = ByteArray(8192)
        jarFile.getInputStream(manifestEntry).use { isStream ->
            while (isStream.read(buffer) != -1) { /* consume */ }
        }

        val certs = manifestEntry.certificates
        if (certs == null || certs.isEmpty()) {
            return ApkVerifyResult(false, "", "", "", "", "", "", "")
        }

        val cert = certs[0] as? X509Certificate ?: return null
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val mdSha256 = MessageDigest.getInstance("SHA-256")
        val sha256Bytes = mdSha256.digest(cert.encoded)
        val sha256Hex = sha256Bytes.joinToString(":") { "%02X".format(it) }

        val mdSha1 = MessageDigest.getInstance("SHA-1")
        val sha1Bytes = mdSha1.digest(cert.encoded)
        val sha1Hex = sha1Bytes.joinToString(":") { "%02X".format(it) }

        return ApkVerifyResult(
            isSigned = true,
            sigAlgName = cert.sigAlgName,
            subject = cert.subjectDN.name,
            issuer = cert.issuerDN.name,
            validFrom = sdf.format(cert.notBefore),
            validUntil = sdf.format(cert.notAfter),
            sha256 = sha256Hex,
            sha1 = sha1Hex
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        return null
    } finally {
        try { jarFile?.close() } catch (ignored: Throwable) {}
    }
}
