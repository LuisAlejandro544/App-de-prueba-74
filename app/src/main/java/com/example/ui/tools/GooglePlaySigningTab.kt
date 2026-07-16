package com.example.ui.tools

import android.content.Context
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.KeyConfig
import com.example.ui.KeystoreViewModel
import com.example.util.FileHelper
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.X509Certificate

@Composable
fun GooglePlaySigningTab(viewModel: KeystoreViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val savedKeys by viewModel.allConfigs.collectAsState()

    var selectedKey by remember { mutableStateOf<KeyConfig?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var keystorePassword by remember { mutableStateOf("") }
    var pemExportMessage by remember { mutableStateOf("") }
    var pemExportSuccess by remember { mutableStateOf(false) }

    // FAQs states
    var faq1Expanded by remember { mutableStateOf(false) }
    var faq2Expanded by remember { mutableStateOf(false) }
    var faq3Expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero banner card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Asistente: App Signing de Google Play",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "Google Play App Signing protege la llave con la que se firman tus APKs finales. Este asistente interactivo te guiará para preparar tus llaves de subida y exportar los certificados necesarios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Selection of Key
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Selecciona tu Llave de Subida (Upload Key)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedKey?.let { "${it.alias} (${it.fileName})" } ?: "Seleccionar Keystore para Google Play",
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
                                        isDropdownExpanded = false
                                        pemExportMessage = ""
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedKey != null) {
                    OutlinedTextField(
                        value = keystorePassword,
                        onValueChange = { keystorePassword = it },
                        label = { Text("Contraseña de Keystore") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Interactive Checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "2. Pasos Interactivos para el Registro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Step A: Generate Upload Key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    val stepDone = selectedKey != null
                    Icon(
                        imageVector = if (stepDone) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (stepDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paso A: Crear o seleccionar Llave de Subida",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (stepDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (stepDone) "Llave seleccionada: ${selectedKey?.alias} (${selectedKey?.category})" else "Necesitas una llave dedicada para firmar lo que subes a Play Store.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider()

                // Step B: Export PEM Certificate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    val stepDone = pemExportSuccess
                    Icon(
                        imageVector = if (stepDone) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (stepDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paso B: Exportar Certificado de Subida (.pem)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (stepDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Google Play Console requiere que registres tu certificado de llave de subida pública (.pem) si restableces o configuras una llave de subida de forma manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val key = selectedKey
                                if (key == null) {
                                    pemExportMessage = "Por favor selecciona un Keystore primero."
                                    pemExportSuccess = false
                                    return@Button
                                }
                                if (keystorePassword.isEmpty()) {
                                    pemExportMessage = "Debe proporcionar la contraseña del Keystore."
                                    pemExportSuccess = false
                                    return@Button
                                }

                                val pemContent = exportCertificateAsPem(key.filePath, keystorePassword, key.alias)
                                if (pemContent != null) {
                                    val pemFile = File(context.cacheDir, "${key.alias}_upload_cert.pem")
                                    pemFile.writeText(pemContent)
                                    FileHelper.shareFile(context, pemFile.absolutePath)
                                    pemExportMessage = "Certificado PEM generado y exportado con éxito en: ${pemFile.name}"
                                    pemExportSuccess = true
                                } else {
                                    pemExportMessage = "Error al extraer el certificado. Verifique la contraseña."
                                    pemExportSuccess = false
                                }
                            },
                            enabled = selectedKey != null,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar y Compartir Certificado (.pem)", style = MaterialTheme.typography.labelMedium)
                        }

                        if (pemExportMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pemExportMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pemExportSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Step C: PEPK Command Helper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paso C: Encriptar Llave Privada con PEPK",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Si decides transferir tu propia llave de firma existente (App Signing Key) en lugar de dejar que Google genere una, debes usar la herramienta oficial PEPK de Google para encriptarla de forma segura.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val aliasName = selectedKey?.alias ?: "[ALIAS]"
                        val fileName = selectedKey?.fileName ?: "[ARCHIVO_KEYSTORE]"
                        val storePass = if (keystorePassword.isNotEmpty()) keystorePassword else "[PASSWORD_STORE]"

                        Text(
                            text = "Comando dinámico para tu terminal:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "java -jar pepk.jar --keystore=$fileName --alias=$aliasName --output=encrypted_private_key.pepk --encryptionkey=eb10c44155190981222bde1fbb3e3170560b11c08bc0d302a759041b714d42875 --algorithm=RSA",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Nota: Descarga pepk.jar de tu consola de Google Play, copia el comando de arriba y ejecútalo en la carpeta donde tienes tu archivo Keystore físico.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // FAQs collapsible sections
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "3. Preguntas Frecuentes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // FAQ 1
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { faq1Expanded = !faq1Expanded }
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Qué es una Llave de Subida (Upload Key)?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (faq1Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(visible = faq1Expanded) {
                        Text(
                            text = "Es la llave que utilizas para firmar tus APKs antes de subirlos a Google Play Console. Google Play recibe tu APK firmado con esta llave, verifica que coincide con el certificado registrado, remueve esta firma de subida y vuelve a firmar tu app con la Llave de Firma de la App real para distribuirla a los usuarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                HorizontalDivider()

                // FAQ 2
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { faq2Expanded = !faq2Expanded }
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Qué pasa si pierdo mi Llave de Subida?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (faq2Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(visible = faq2Expanded) {
                        Text(
                            text = "¡No te preocupes! Si pierdes tu Llave de Subida, puedes ir a Google Play Console (sección Firma de Apps), solicitar el restablecimiento de tu llave de subida, y registrar el nuevo archivo .pem que generamos en el Paso B de este asistente interactivo. Google Play actualizará tu registro en un par de horas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                HorizontalDivider()

                // FAQ 3
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { faq3Expanded = !faq3Expanded }
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Por qué es más seguro usar Google Play App Signing?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (faq3Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(visible = faq3Expanded) {
                        Text(
                            text = "Porque si pierdes tu llave original de firma de app, nunca más podrías actualizar tu aplicación. Al delegar la llave de firma real a Google (quien la resguarda con la misma infraestructura ultra-segura de sus servicios), puedes cambiar tu llave de subida local en cualquier momento si la pierdes o es comprometida, sin afectar tus actualizaciones ni a tus usuarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extracts and formats the X.509 certificate in PEM format.
 */
private fun exportCertificateAsPem(keystorePath: String, keystorePass: String, alias: String): String? {
    return try {
        val file = File(keystorePath)
        if (!file.exists()) return null
        val keystoreType = if (keystorePath.endsWith(".jks", ignoreCase = true)) "JKS" else "PKCS12"
        val ks = KeyStore.getInstance(keystoreType)
        FileInputStream(file).use { fis ->
            ks.load(fis, keystorePass.toCharArray())
        }
        val cert = ks.getCertificate(alias) ?: return null
        val encoded = cert.encoded
        val base64 = Base64.encodeToString(encoded, Base64.DEFAULT)
        "-----BEGIN CERTIFICATE-----\n$base64-----END CERTIFICATE-----"
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
