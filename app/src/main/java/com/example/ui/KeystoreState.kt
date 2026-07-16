package com.example.ui

import com.example.data.KeyConfig

sealed interface KeystoreState {
    object Idle : KeystoreState
    object Generating : KeystoreState
    data class Success(val config: KeyConfig) : KeystoreState
    data class Error(val message: String) : KeystoreState
}
