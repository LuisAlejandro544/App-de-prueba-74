package com.example.security

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate

class KeystoreStorageService {
    companion object {
        private const val TAG = "KeystoreStorageService"
    }

    fun saveKeystore(
        context: Context,
        alias: String,
        privateKey: PrivateKey,
        certificateChain: Array<Certificate>,
        storePassword: String,
        keyPassword: String,
        extension: String
    ): File {
        // Build Keystore
        val keyStore = KeyStore.getInstance("PKCS12", "BC")
        keyStore.load(null, null)

        // Set entry
        keyStore.setKeyEntry(
            alias,
            privateKey,
            keyPassword.toCharArray(),
            certificateChain
        )

        // Write to File in context's external files directory so it can be easily shared
        val outputDir = File(context.getExternalFilesDir(null), "keystores")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val cleanedAlias = alias.replace("\\s+".toRegex(), "_").lowercase()
        val fileName = "${cleanedAlias}_$timestamp$extension"
        val file = File(outputDir, fileName)

        FileOutputStream(file).use { fos ->
            keyStore.store(fos, storePassword.toCharArray())
        }

        Log.d(TAG, "Keystore file written to: ${file.absolutePath}")
        return file
    }
}
