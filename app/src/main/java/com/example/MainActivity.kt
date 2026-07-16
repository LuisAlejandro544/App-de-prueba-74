package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.KeyConfigRepository
import com.example.ui.KeystoreApp
import com.example.ui.KeystoreViewModel
import com.example.ui.KeystoreViewModelFactory
import com.example.ui.PinScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = KeyConfigRepository(database.keyConfigDao())
        val viewModel = ViewModelProvider(this, KeystoreViewModelFactory(repository))[KeystoreViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isUnlocked by remember { mutableStateOf(false) }
                if (!isUnlocked) {
                    PinScreen(onUnlockSuccess = { isUnlocked = true })
                } else {
                    KeystoreApp(viewModel)
                }
            }
        }
    }
}
