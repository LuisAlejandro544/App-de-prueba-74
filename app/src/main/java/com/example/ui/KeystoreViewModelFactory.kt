package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.KeyConfigRepository

class KeystoreViewModelFactory(private val repository: KeyConfigRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KeystoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KeystoreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
