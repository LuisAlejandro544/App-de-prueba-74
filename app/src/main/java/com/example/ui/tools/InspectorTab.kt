package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.KeyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InspectorTab(viewModel: KeystoreViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var keyAlias by remember { mutableStateOf("") }
    var keyPassword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PRODUCCIÓN") }

    var inspectResult by remember { mutableStateOf<InspectDetails?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isInspecting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileUri = uri
            fileName = getFileNameFromUri(context, uri) ?: "keystore_file"
            inspectResult = null
            errorMessage = ""
            
            // Proactively copy the file to a temp input in the background
            coroutineScope.launch {
                val tempFile = withContext(Dispatchers.IO) {
                    copyUriToTempFile(context, uri, "temp_inspect_source")
                }
                if (tempFile == null) {
                    errorMessage = "No se pudo leer el archivo Keystore seleccionado de forma inmediata."
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
                    text = "Importar Keystore Existente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInspecting
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fileName.isEmpty()) "Seleccionar archivo .jks o .keystore" else "Archivo: $fileName")
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña de Keystore") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInspecting
                )

                OutlinedTextField(
                    value = keyAlias,
                    onValueChange = { keyAlias = it },
                    label = { Text("Alias de Llave") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInspecting
                )

                OutlinedTextField(
                    value = keyPassword,
                    onValueChange = { keyPassword = it },
                    label = { Text("Contraseña de Llave") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInspecting
                )

                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf("PRODUCCIÓN", "DEBUG").forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = category == cat,
                                onClick = { category = cat },
                                enabled = !isInspecting
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(cat)
                        }
                    }
                }

                Button(
                    onClick = {
                        val uri = fileUri
                        if (uri == null) {
                            errorMessage = "Seleccione un archivo Keystore primero."
                            return@Button
                        }
                        if (password.isEmpty() || keyAlias.isEmpty() || keyPassword.isEmpty()) {
                            errorMessage = "Todos los campos de contraseña y alias son obligatorios."
                            return@Button
                        }

                        isInspecting = true
                        errorMessage = ""
                        inspectResult = null

                        coroutineScope.launch {
                            val tempFile = File(context.cacheDir, "temp_inspect_source")
                            if (!tempFile.exists()) {
                                errorMessage = "El archivo temporal no está listo. Intente seleccionarlo de nuevo."
                                isInspecting = false
                                return@launch
                            }

                            val details = withContext(Dispatchers.IO) {
                                inspectKeystore(tempFile, password, keyAlias, keyPassword)
                            }

                            isInspecting = false
                            if (details != null) {
                                inspectResult = details
                                errorMessage = ""
                            } else {
                                errorMessage = "Error al abrir el Keystore. Verifique las contraseñas, el alias y la integridad del archivo."
                                inspectResult = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = fileUri != null && !isInspecting
                ) {
                    if (isInspecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Text("Inspeccionar Llave")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessage.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AnimatedVisibility(
            visible = inspectResult != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            inspectResult?.let { details ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Detalles del Certificado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextValueRow("Algoritmo", details.algorithm)
                        TextValueRow("Tamaño", "${details.keySize} bits")
                        TextValueRow("Propietario (Subject)", details.subject)
                        TextValueRow("Emisor (Issuer)", details.issuer)
                        TextValueRow("Válido Desde", details.validFrom)
                        TextValueRow("Válido Hasta", details.validUntil)
                        TextValueRow("Firma SHA-256", details.sha256, isCode = true)
                        TextValueRow("Firma SHA-1", details.sha1, isCode = true)

                        var isSaving by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                val uri = fileUri ?: return@Button
                                isSaving = true
                                errorMessage = ""
                                
                                coroutineScope.launch {
                                    val destFolder = File(context.filesDir, "keystores")
                                    if (!destFolder.exists()) destFolder.mkdirs()
                                    val destFile = File(destFolder, fileName)
                                    
                                    val copied = withContext(Dispatchers.IO) {
                                        copyUriToFile(context, uri, destFile)
                                    }
                                    
                                    isSaving = false
                                    if (copied) {
                                        val newConfig = KeyConfig(
                                            alias = keyAlias,
                                            storePassword = password,
                                            keyPassword = keyPassword,
                                            category = category,
                                            validityYears = details.validityYears,
                                            filePath = destFile.absolutePath,
                                            fileName = fileName,
                                            algorithm = details.algorithm,
                                            keySize = details.keySize,
                                            commonName = parseDnField(details.subject, "CN"),
                                            organizationalUnit = parseDnField(details.subject, "OU"),
                                            organization = parseDnField(details.subject, "O"),
                                            locality = parseDnField(details.subject, "L"),
                                            state = parseDnField(details.subject, "ST"),
                                            countryCode = parseDnField(details.subject, "C"),
                                            timestamp = System.currentTimeMillis()
                                        )
                                        viewModel.insertConfig(newConfig)
                                        Toast.makeText(context, "Keystore importada con éxito.", Toast.LENGTH_SHORT).show()
                                        inspectResult = null
                                        fileUri = null
                                        fileName = ""
                                        password = ""
                                        keyAlias = ""
                                        keyPassword = ""
                                    } else {
                                        errorMessage = "No se pudo copiar el archivo Keystore al almacenamiento interno."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Save, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar e Importar a la App")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseDnField(dn: String, field: String): String {
    val regex = "(?i)$field=([^,]+)".toRegex()
    val match = regex.find(dn)
    return match?.groupValues?.get(1)?.trim() ?: ""
}

private fun inspectKeystore(keystoreFile: File, storePass: String, alias: String, keyPass: String): InspectDetails? {
    return try {
        // Ensure BouncyCastle is ready
        try {
            com.example.security.BouncyCastleManager.setupBouncyCastle()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        val keystoreType = if (keystoreFile.name.endsWith(".jks", ignoreCase = true)) "JKS" else "PKCS12"
        
        // Try getting KeyStore instance with default provider first, fallback to BouncyCastle
        val ks = try {
            KeyStore.getInstance(keystoreType)
        } catch (e: Exception) {
            KeyStore.getInstance(keystoreType, "BC")
        }

        FileInputStream(keystoreFile).use { fis ->
            ks.load(fis, storePass.toCharArray())
        }

        if (!ks.containsAlias(alias)) return null

        val cert = ks.getCertificate(alias) as? X509Certificate ?: return null
        val pubKey = cert.publicKey
        val algorithm = pubKey.algorithm

        // Key size estimation
        val size = when (val key = pubKey) {
            is java.security.interfaces.RSAPublicKey -> key.modulus.bitLength()
            is java.security.interfaces.ECPublicKey -> key.params.curve.field.fieldSize
            else -> 2048
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val fromStr = sdf.format(cert.notBefore)
        val untilStr = sdf.format(cert.notAfter)

        // Calculate validity years approximately
        val diffMs = cert.notAfter.time - cert.notBefore.time
        val validityYears = (diffMs / (365L * 24 * 60 * 60 * 1000)).toInt()

        // Compute fingerprints
        val mdSha256 = MessageDigest.getInstance("SHA-256")
        val sha256Bytes = mdSha256.digest(cert.encoded)
        val sha256Hex = sha256Bytes.joinToString(":") { "%02X".format(it) }

        val mdSha1 = MessageDigest.getInstance("SHA-1")
        val sha1Bytes = mdSha1.digest(cert.encoded)
        val sha1Hex = sha1Bytes.joinToString(":") { "%02X".format(it) }

        InspectDetails(
            algorithm = algorithm,
            keySize = size,
            subject = cert.subjectDN.name,
            issuer = cert.issuerDN.name,
            validFrom = fromStr,
            validUntil = untilStr,
            sha256 = sha256Hex,
            sha1 = sha1Hex,
            validityYears = validityYears
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}
