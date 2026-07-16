package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KeyConfig
import com.example.data.KeyConfigRepository
import com.example.security.KeystoreGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class KeystoreViewModel(private val repository: KeyConfigRepository) : ViewModel() {

    val allConfigs: StateFlow<List<KeyConfig>> = repository.allConfigs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _generationState = MutableStateFlow<KeystoreState>(KeystoreState.Idle)
    val generationState: StateFlow<KeystoreState> = _generationState.asStateFlow()

    fun resetState() {
        _generationState.value = KeystoreState.Idle
    }

    fun generateKeystore(
        context: Context,
        alias: String,
        storePassword: String,
        keyPassword: String,
        validityYears: Int,
        algorithm: String,
        keySize: Int,
        cn: String,
        ou: String,
        o: String,
        l: String,
        st: String,
        c: String,
        extension: String,
        category: String,
        onSuccess: (KeyConfig) -> Unit
    ) {
        viewModelScope.launch {
            _generationState.value = KeystoreState.Generating
            try {
                // Perform heavy cryptographic generation off the main thread using Dispatchers.Default.
                // This leverages Kotlin's default CPU pool, intelligently utilizing both performance & efficiency cores.
                val savedConfig = withContext(Dispatchers.Default) {
                    // Set thread priority to background so the OS scheduler handles it optimally without affecting UI frames or causing ANRs.
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                    
                    // Generate the physical keystore file
                    val file = KeystoreGenerator.generate(
                        context = context,
                        alias = alias.trim(),
                        storePassword = storePassword,
                        keyPassword = keyPassword,
                        validityYears = validityYears,
                        algorithm = algorithm,
                        keySize = keySize,
                        cn = cn.trim(),
                        ou = ou.trim(),
                        o = o.trim(),
                        l = l.trim(),
                        st = st.trim(),
                        c = c.trim(),
                        extension = extension
                    )

                    // Create database record
                    val config = KeyConfig(
                        alias = alias.trim(),
                        storePassword = storePassword,
                        keyPassword = keyPassword,
                        validityYears = validityYears,
                        algorithm = algorithm,
                        keySize = keySize,
                        commonName = cn.trim(),
                        organizationalUnit = ou.trim(),
                        organization = o.trim(),
                        locality = l.trim(),
                        state = st.trim(),
                        countryCode = c.trim(),
                        fileName = file.name,
                        filePath = file.absolutePath,
                        category = category
                    )

                    val id = repository.insertConfig(config)
                    config.copy(id = id.toInt())
                }

                _generationState.value = KeystoreState.Success(savedConfig)
                onSuccess(savedConfig)
            } catch (e: Exception) {
                Log.e("KeystoreViewModel", "Error generating keystore", e)
                _generationState.value = KeystoreState.Error(
                    e.localizedMessage ?: "Ocurrió un error inesperado al generar la llave."
                )
            }
        }
    }

    suspend fun getConfigById(id: Int): KeyConfig? {
        return repository.getConfigById(id)
    }

    fun insertConfig(config: KeyConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertConfig(config)
        }
    }

    fun deleteConfig(config: KeyConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Delete physical file first
                val file = File(config.filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("KeystoreViewModel", "Physical file deleted: $deleted")
                }

                // 2. Delete database entry
                repository.deleteConfig(config)
            } catch (e: Exception) {
                Log.e("KeystoreViewModel", "Error deleting keystore", e)
            }
        }
    }
}
