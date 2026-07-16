package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.util.PinManager
import com.example.util.BiometricHelper

@Composable
fun PinScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isSetupMode by remember { mutableStateOf(!PinManager.isPinSet(context)) }
    
    // States for PIN setup
    var setupStep by remember { mutableStateOf(1) } // 1: Enter new, 2: Confirm new
    var firstEnteredPin by remember { mutableStateOf("") }
    
    // Current input
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showDigits by remember { mutableStateOf(false) }
    var showBiometricSetupDialog by remember { mutableStateOf(false) }

    val maxPinLength = 4

    // Auto trigger biometric if enabled
    LaunchedEffect(Unit) {
        if (!isSetupMode && BiometricHelper.isBiometricEnabled(context) && BiometricHelper.isBiometricAvailable(context)) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricHelper.authenticate(
                    activity = activity,
                    onSuccess = {
                        onUnlockSuccess()
                    },
                    onError = { err ->
                        errorMessage = err
                    }
                )
            }
        }
    }

    val onNumberClick: (String) -> Unit = { digit ->
        if (pinInput.length < maxPinLength) {
            pinInput += digit
            errorMessage = ""
        }
    }

    val onBackspaceClick: () -> Unit = {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
            errorMessage = ""
        }
    }

    val onConfirmClick: () -> Unit = {
        if (pinInput.length < maxPinLength) {
            errorMessage = "El PIN debe tener $maxPinLength dígitos."
        } else {
            if (isSetupMode) {
                if (setupStep == 1) {
                    firstEnteredPin = pinInput
                    pinInput = ""
                    setupStep = 2
                    errorMessage = ""
                } else {
                    if (pinInput == firstEnteredPin) {
                        PinManager.savePin(context, pinInput)
                        isSetupMode = false
                        if (BiometricHelper.isBiometricAvailable(context)) {
                            showBiometricSetupDialog = true
                        } else {
                            onUnlockSuccess()
                        }
                    } else {
                        errorMessage = "Los PINs no coinciden. Inténtalo de nuevo."
                        pinInput = ""
                        setupStep = 1
                    }
                }
            } else {
                if (PinManager.checkPin(context, pinInput)) {
                    onUnlockSuccess()
                } else {
                    errorMessage = "PIN incorrecto. Inténtalo de nuevo."
                    pinInput = ""
                }
            }
        }
    }

    if (showBiometricSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                showBiometricSetupDialog = false
                onUnlockSuccess()
            },
            title = {
                Text("¿Activar datos biométricos?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Deseas activar la autenticación biométrica (Huella digital o rostro) para acceder de forma rápida y segura?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        BiometricHelper.setBiometricEnabled(context, true)
                        showBiometricSetupDialog = false
                        onUnlockSuccess()
                    }
                ) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        BiometricHelper.setBiometricEnabled(context, false)
                        showBiometricSetupDialog = false
                        onUnlockSuccess()
                    }
                ) {
                    Text("No, gracias")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isSetupMode) {
                        if (setupStep == 1) "Configurar PIN de Seguridad" else "Confirmar PIN de Seguridad"
                    } else {
                        "Ingrese su PIN de Acceso"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isSetupMode) {
                        if (setupStep == 1) "Crea un PIN de 4 dígitos obligatorio para proteger el acceso a tus llaves." else "Confirma el PIN que acabas de ingresar."
                    } else {
                        "Protección de firma de aplicaciones activa."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // PIN Indicator Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until maxPinLength) {
                        val isFilled = i < pinInput.length
                        val digitToShow = if (isFilled) pinInput[i].toString() else ""
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    color = if (isFilled && !showDigits) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                if (showDigits) {
                                    Text(
                                        text = digitToShow,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = { showDigits = !showDigits }) {
                        Icon(
                            imageVector = if (showDigits) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar/Ocultar PIN",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedVisibility(
                    visible = errorMessage.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("pin_error_msg")
                    )
                }

                if (!isSetupMode && BiometricHelper.isBiometricAvailable(context) && BiometricHelper.isBiometricEnabled(context)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    onSuccess = { onUnlockSuccess() },
                                    onError = { err -> errorMessage = err }
                                )
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Autenticación Biométrica",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Desbloquear con Huella / Rostro",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Keyboard Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val buttonRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                
                buttonRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { num ->
                            KeyButton(
                                text = num,
                                onClick = { onNumberClick(num) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Backspace Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onBackspaceClick() }
                            .testTag("key_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Borrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 0 Button
                    KeyButton(
                        text = "0",
                        onClick = { onNumberClick("0") },
                        modifier = Modifier.weight(1f)
                    )

                    // Confirm/Check Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onConfirmClick() }
                            .testTag("key_confirm"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aceptar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .testTag("key_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
