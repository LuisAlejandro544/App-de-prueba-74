package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.KeyConfig
import com.example.util.ApkSignerHelper
import com.example.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ApkSignerTab(viewModel: KeystoreViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val savedKeys by viewModel.allConfigs.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var unsignedApkUri by remember { mutableStateOf<Uri?>(null) }
    var unsignedApkName by remember { mutableStateOf("") }

    var selectedKey by remember { mutableStateOf<KeyConfig?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var keystorePassword by remember { mutableStateOf("") }
    var keyPassword by remember { mutableStateOf("") }

    var isSigning by remember { mutableStateOf(false) }
    var signingResultMsg by remember { mutableStateOf("") }
    var signingSuccess by remember { mutableStateOf(false) }

    var v1Enabled by remember { mutableStateOf(true) }
    var v2Enabled by remember { mutableStateOf(true) }
    var v3Enabled by remember { mutableStateOf(false) }
    var v4Enabled by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            unsignedApkUri = uri
            unsignedApkName = getFileNameFromUri(context, uri) ?: "app.apk"
            signingResultMsg = ""
            
            // Proactively copy the file to a temp input in the background
            coroutineScope.launch {
                val tempFile = withContext(Dispatchers.IO) {
                    copyUriToTempFile(context, uri, "temp_sign_in.apk")
                }
                if (tempFile == null) {
                    signingResultMsg = "No se pudo leer el archivo APK de origen de forma inmediata."
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
                    text = "Firmar / Re-firmar archivo APK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Selecciona un APK original o sin firmar y utiliza cualquiera de tus llaves almacenadas para firmarlo programáticamente con el estándar de firma JAR v1 de Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = { filePicker.launch("application/vnd.android.package-archive") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSigning
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (unsignedApkName.isEmpty()) "Seleccionar APK Origen" else "APK: $unsignedApkName")
                }

                Text(
                    text = "Llave de Firma",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSigning
                    ) {
                        Text(
                            text = selectedKey?.let { "${it.alias} (${it.fileName})" } ?: "Seleccionar de mis Keystores guardados",
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (savedKeys.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No tienes Keystores guardados") },
                                onClick = { isDropdownExpanded = false }
                            )
                        } else {
                            savedKeys.forEach { key ->
                                DropdownMenuItem(
                                    text = { Text("${key.alias} (${key.fileName})") },
                                    onClick = {
                                        selectedKey = key
                                        keystorePassword = key.storePassword
                                        keyPassword = key.keyPassword
                                        isDropdownExpanded = false
                                        signingResultMsg = ""
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = keystorePassword,
                    onValueChange = { keystorePassword = it },
                    label = { Text("Contraseña de Keystore") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSigning
                )

                OutlinedTextField(
                    value = keyPassword,
                    onValueChange = { keyPassword = it },
                    label = { Text("Contraseña de Llave") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSigning
                )

                Text(
                    text = "Esquemas de Firma a aplicar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = v1Enabled,
                                    onCheckedChange = { v1Enabled = it },
                                    enabled = !isSigning
                                )
                                Column {
                                    Text("v1 (JAR)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Compatibilidad", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = v2Enabled,
                                    onCheckedChange = { v2Enabled = it },
                                    enabled = !isSigning
                                )
                                Column {
                                    Text("v2 (Completo)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Rápido y Seguro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = v3Enabled,
                                    onCheckedChange = { v3Enabled = it },
                                    enabled = !isSigning
                                )
                                Column {
                                    Text("v3 (Rotación)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Llaves múltiples", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = v4Enabled,
                                    onCheckedChange = { v4Enabled = it },
                                    enabled = !isSigning
                                )
                                Column {
                                    Text("v4 (Incremental)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Android 11+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val uri = unsignedApkUri
                        val key = selectedKey
                        if (uri == null || key == null) {
                            signingResultMsg = "Seleccione un APK y una llave de firma primero."
                            signingSuccess = false
                            return@Button
                        }
                        if (keystorePassword.isEmpty() || keyPassword.isEmpty()) {
                            signingResultMsg = "Las contraseñas de keystore y llave son obligatorias."
                            signingSuccess = false
                            return@Button
                        }
                        if (!v1Enabled && !v2Enabled && !v3Enabled && !v4Enabled) {
                            signingResultMsg = "Debe seleccionar al menos un esquema de firma para aplicar."
                            signingSuccess = false
                            return@Button
                        }

                        isSigning = true
                        signingResultMsg = ""

                        coroutineScope.launch {
                            val tempIn = File(context.cacheDir, "temp_sign_in.apk")
                            if (!tempIn.exists()) {
                                signingResultMsg = "El archivo temporal no está listo. Intente seleccionarlo de nuevo."
                                signingSuccess = false
                                isSigning = false
                                return@launch
                            }

                            val signedName = unsignedApkName.substringBeforeLast(".") + "_signed.apk"
                            val tempOutFile = File(context.cacheDir, signedName)

                            // Run signing in background threadpool (Dispatchers.IO)
                            val res = withContext(Dispatchers.IO) {
                                ApkSignerHelper.signApk(
                                    context = context,
                                    inputApkPath = tempIn.absolutePath,
                                    outputApkPath = tempOutFile.absolutePath,
                                    keystorePath = key.filePath,
                                    keystorePass = keystorePassword,
                                    alias = key.alias,
                                    keyPass = keyPassword,
                                    v1Enabled = v1Enabled,
                                    v2Enabled = v2Enabled,
                                    v3Enabled = v3Enabled,
                                    v4Enabled = v4Enabled
                                )
                            }

                            isSigning = false
                            signingSuccess = res.success
                            signingResultMsg = res.message

                            if (res.success) {
                                FileHelper.shareFile(context, tempOutFile.absolutePath)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = unsignedApkUri != null && selectedKey != null && !isSigning
                ) {
                    if (isSigning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.BorderColor, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Firmar APK y Compartir")
                    }
                }
            }
        }

        if (signingResultMsg.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (signingSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = signingResultMsg,
                    color = if (signingSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
