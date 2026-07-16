package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.KeyConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: KeystoreViewModel,
    onNavigateBack: () -> Unit,
    onGenerationSuccess: (Int) -> Unit
) {
    val context = LocalContext.current
    val generationState by viewModel.generationState.collectAsState()

    // Form states
    var alias by remember { mutableStateOf("") }
    var storePassword by remember { mutableStateOf("") }
    var keyPassword by remember { mutableStateOf("") }
    var samePasswordAsStore by remember { mutableStateOf(true) }
    var validityYears by remember { mutableFloatStateOf(25f) }
    var algorithm by remember { mutableStateOf("RSA") } // "RSA" or "EC"
    var keySize by remember { mutableIntStateOf(2048) } // 2048 or 4096 for RSA, 256 or 384 for EC
    
    // Certificate Distinguished Name (DN) - Optional fields
    var commonName by remember { mutableStateOf("") }
    var organizationalUnit by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    
    var extension by remember { mutableStateOf(".jks") } // ".jks" or ".keystore"
    var category by remember { mutableStateOf("PRODUCCIÓN") } // "DEBUG", "PRODUCCIÓN", "PRUEBA"

    // UI visual helpers
    var storePasswordVisible by remember { mutableStateOf(false) }
    var keyPasswordVisible by remember { mutableStateOf(false) }
    var advancedSectionExpanded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // If same password checked, mirror values
    LaunchedEffect(samePasswordAsStore, storePassword) {
        if (samePasswordAsStore) {
            keyPassword = storePassword
        }
    }

    // Automatically adjust default key sizes when algorithm changes
    LaunchedEffect(algorithm) {
        keySize = if (algorithm == "RSA") 2048 else 256
    }

    // Reset ViewModel state when screen is first presented
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Monitor generation status to navigate on success
    LaunchedEffect(generationState) {
        if (generationState is KeystoreState.Success) {
            val savedId = (generationState as KeystoreState.Success).config.id
            Toast.makeText(context, "¡Llave generada con éxito!", Toast.LENGTH_LONG).show()
            onGenerationSuccess(savedId)
        } else if (generationState is KeystoreState.Error) {
            val errMsg = (generationState as KeystoreState.Error).message
            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
        }
    }

    // Validation fields
    val isAliasValid = alias.isNotBlank()
    val isStorePasswordValid = storePassword.length >= 6
    val isKeyPasswordValid = samePasswordAsStore || keyPassword.length >= 6
    val isCountryCodeValid = countryCode.isEmpty() || countryCode.length == 2
    val isFormValid = isAliasValid && isStorePasswordValid && isKeyPasswordValid && isCountryCodeValid
    val isGenerating = generationState is KeystoreState.Generating

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generar Keystore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("btn_generator_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick presets banner for Debug template
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Atajos de Configuración",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Preconfigura instantáneamente los campos recomendados para una llave debug estándar de Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                alias = "androiddebugkey"
                                storePassword = "android"
                                keyPassword = "android"
                                samePasswordAsStore = true
                                validityYears = 30f
                                commonName = "Android Debug"
                                organization = "Android"
                                countryCode = "US"
                                category = "DEBUG"
                                extension = ".keystore"
                                Toast.makeText(context, "Plantilla Debug de Android cargada", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("btn_preset_debug")
                        ) {
                            Text("Cargar Plantilla Debug (.keystore)", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section 1: Basic Keystore credentials
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Credenciales Requeridas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Completa los parámetros básicos para configurar tu archivo de firma.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        // Alias
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { newValue ->
                                // Sanitize: replace spaces and special characters with underscore
                                alias = newValue.replace("[^a-zA-Z0-9_\\-]".toRegex(), "")
                            },
                            label = { Text("Alias de la Llave") },
                            placeholder = { Text("ej. key_alias") },
                            singleLine = true,
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_alias"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            supportingText = {
                                Text("Solo letras, números y guiones bajos (_)")
                            },
                            isError = alias.isNotEmpty() && !isAliasValid
                        )

                        // Store Password
                        OutlinedTextField(
                            value = storePassword,
                            onValueChange = { storePassword = it },
                            label = { Text("Contraseña del Keystore") },
                            placeholder = { Text("Min. 6 caracteres") },
                            singleLine = true,
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_store_password"),
                            visualTransformation = if (storePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { storePasswordVisible = !storePasswordVisible }) {
                                    Icon(
                                        imageVector = if (storePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (storePasswordVisible) "Ocultar" else "Mostrar"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            supportingText = {
                                if (storePassword.isNotEmpty() && storePassword.length < 6) {
                                    Text("La contraseña debe tener al menos 6 caracteres", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Contraseña del archivo contenedor (.jks / .keystore)")
                                }
                            },
                            isError = storePassword.isNotEmpty() && storePassword.length < 6
                        )

                        // Checkbox to reuse password
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = !isGenerating) {
                                    samePasswordAsStore = !samePasswordAsStore
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = samePasswordAsStore,
                                onCheckedChange = { samePasswordAsStore = it },
                                enabled = !isGenerating,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("checkbox_same_password")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Usar la misma contraseña para la llave (Alias)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Key Password (Visible if same password is unchecked)
                        AnimatedVisibility(
                            visible = !samePasswordAsStore,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = keyPassword,
                                onValueChange = { keyPassword = it },
                                label = { Text("Contraseña de la Llave") },
                                placeholder = { Text("Min. 6 caracteres") },
                                singleLine = true,
                                enabled = !isGenerating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_key_password"),
                                visualTransformation = if (keyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { keyPasswordVisible = !keyPasswordVisible }) {
                                        Icon(
                                            imageVector = if (keyPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (keyPasswordVisible) "Ocultar" else "Mostrar"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                supportingText = {
                                    if (keyPassword.isNotEmpty() && keyPassword.length < 6) {
                                        Text("La contraseña debe tener al menos 6 caracteres", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("Contraseña del alias de firma")
                                    }
                                },
                                isError = keyPassword.isNotEmpty() && keyPassword.length < 6
                            )
                        }

                        // Formato de Extensión (.jks or .keystore)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Formato de Extensión",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val extensions = listOf(".jks", ".keystore")
                            extensions.forEach { ext ->
                                val selected = extension == ext
                                Button(
                                    onClick = { extension = ext },
                                    enabled = !isGenerating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_extension_$ext"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(ext, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Categoría de la Llave
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Categoría de Uso",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf("DEBUG", "PRODUCCIÓN", "PRUEBA")
                            categories.forEach { cat ->
                                val selected = category == cat
                                val displayColor = when(cat) {
                                    "DEBUG" -> MaterialTheme.colorScheme.tertiary
                                    "PRODUCCIÓN" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) displayColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) displayColor else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { category = cat }
                                        .padding(vertical = 10.dp)
                                        .testTag("cat_$cat"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Cryptographic Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Algoritmo de Cifrado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Algorithm selector (RSA vs EC)
                        Text(
                            text = "Algoritmo",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val algorithms = listOf("RSA", "EC")
                            algorithms.forEach { alg ->
                                val selected = algorithm == alg
                                Button(
                                    onClick = { algorithm = alg },
                                    enabled = !isGenerating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_algo_$alg"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(alg, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Key Size Selector (Context dependent!)
                        Text(
                            text = "Tamaño de la Llave",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sizes = if (algorithm == "RSA") listOf(2048, 4096) else listOf(256, 384)
                            sizes.forEach { size ->
                                val selected = keySize == size
                                Button(
                                    onClick = { keySize = size },
                                    enabled = !isGenerating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_keysize_$size"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text("$size bits", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Validity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Años de Validez",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${validityYears.toInt()} años",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = validityYears,
                            onValueChange = { validityYears = it },
                            valueRange = 1f..100f,
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_validity")
                        )
                        Text(
                            text = "Google Play requiere al menos 25 años de validez para nuevas aplicaciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Section 3: Advanced Certificate DN & Extension Settings (Collapsible)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isGenerating) { advancedSectionExpanded = !advancedSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Información del Propietario",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Icon(
                                imageVector = if (advancedSectionExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        AnimatedVisibility(
                            visible = advancedSectionExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Campos identificadores para el certificado autofirmado (opcionales pero recomendados para publicar).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                // CN - First and Last Name
                                OutlinedTextField(
                                    value = commonName,
                                    onValueChange = { commonName = it },
                                    label = { Text("Nombre Completo (CN) - Esencial") },
                                    placeholder = { Text("ej. Pac Mesa") },
                                    singleLine = true,
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        Text("Recomendado para identificar al autor")
                                    }
                                )

                                // OU - Organizational Unit
                                OutlinedTextField(
                                    value = organizationalUnit,
                                    onValueChange = { organizationalUnit = it },
                                    label = { Text("Unidad Organizacional (OU)") },
                                    placeholder = { Text("ej. Desarrollo") },
                                    singleLine = true,
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // O - Organization
                                OutlinedTextField(
                                    value = organization,
                                    onValueChange = { organization = it },
                                    label = { Text("Organización (O) - Esencial") },
                                    placeholder = { Text("ej. Mi Compañía") },
                                    singleLine = true,
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        Text("Recomendado para identificar a la compañía")
                                    }
                                )

                                // L - Locality
                                OutlinedTextField(
                                    value = locality,
                                    onValueChange = { locality = it },
                                    label = { Text("Ciudad o Localidad (L)") },
                                    placeholder = { Text("ej. Madrid") },
                                    singleLine = true,
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // ST - State
                                OutlinedTextField(
                                    value = state,
                                    onValueChange = { state = it },
                                    label = { Text("Estado o Provincia (ST)") },
                                    placeholder = { Text("ej. Madrid") },
                                    singleLine = true,
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // C - Country Code
                                OutlinedTextField(
                                    value = countryCode,
                                    onValueChange = { newValue ->
                                        // Restrict to max 2 letters and capitalize
                                        if (newValue.length <= 2) {
                                            countryCode = newValue.uppercase()
                                        }
                                    },
                                    label = { Text("Código de País (C) - Esencial") },
                                    placeholder = { Text("ej. ES, MX, US") },
                                    singleLine = true,
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        Text("Código ISO de 2 letras de tu país")
                                    },
                                    isError = countryCode.isNotEmpty() && !isCountryCodeValid
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = {
                        val hasMissingEssential = commonName.trim().isEmpty() || organization.trim().isEmpty() || countryCode.trim().isEmpty()
                        if (hasMissingEssential) {
                            showConfirmationDialog = true
                        } else {
                            viewModel.generateKeystore(
                                context = context,
                                alias = alias,
                                storePassword = storePassword,
                                keyPassword = keyPassword,
                                validityYears = validityYears.toInt(),
                                algorithm = algorithm,
                                keySize = keySize,
                                cn = commonName,
                                ou = organizationalUnit,
                                o = organization,
                                l = locality,
                                st = state,
                                c = countryCode,
                                extension = extension,
                                category = category,
                                onSuccess = { savedConfig ->
                                    // Success flow handled by launched effect
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_submit_generation"),
                    enabled = isFormValid && !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Generando llave segura...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Generar Keystore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Warning dialog for missing essential/recommended owner information
            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¿Generar sin datos recomendados?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "No has completado algunos campos recomendados para el certificado (Nombre Completo, Organización o Código de País).\n\nAunque técnicamente son opcionales y la firma funcionará correctamente, se consideran de suma importancia para identificar de forma oficial la autoría de tu aplicación antes de subirla a Google Play.\n\n¿Quieres continuar de todas formas?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                viewModel.generateKeystore(
                                    context = context,
                                    alias = alias,
                                    storePassword = storePassword,
                                    keyPassword = keyPassword,
                                    validityYears = validityYears.toInt(),
                                    algorithm = algorithm,
                                    keySize = keySize,
                                    cn = commonName,
                                    ou = organizationalUnit,
                                    o = organization,
                                    l = locality,
                                    st = state,
                                    c = countryCode,
                                    extension = extension,
                                    category = category,
                                    onSuccess = { savedConfig ->
                                        // Success flow handled by launched effect
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("dialog_confirm_btn")
                        ) {
                            Text("Sí, generar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showConfirmationDialog = false
                                // Expand advanced section so user can fill the fields
                                advancedSectionExpanded = true
                            },
                            modifier = Modifier.testTag("dialog_dismiss_btn")
                        ) {
                            Text("Completar datos", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
