package com.example.security

import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

object BouncyCastleManager {
    private const val TAG = "BouncyCastleManager"

    fun setupBouncyCastle() {
        try {
            val provider = Security.getProvider("BC")
            if (provider == null || provider.javaClass.name != BouncyCastleProvider::class.java.name) {
                Log.d(TAG, "Registering full BouncyCastle Provider")
                Security.removeProvider("BC")
                Security.addProvider(BouncyCastleProvider())
            } else {
                Log.d(TAG, "BouncyCastle Provider already registered correctly")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register BouncyCastle", e)
        }
    }
}
