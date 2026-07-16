package com.example.util

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkSignerHelper {

    init {
        // Add Bouncy Castle security provider
        com.example.security.BouncyCastleManager.setupBouncyCastle()
    }

    class SignResult(val success: Boolean, val message: String, val error: Throwable? = null)

    fun signApk(
        context: Context,
        inputApkPath: String,
        outputApkPath: String,
        keystorePath: String,
        keystorePass: String,
        alias: String,
        keyPass: String,
        v1Enabled: Boolean = true,
        v2Enabled: Boolean = true,
        v3Enabled: Boolean = false,
        v4Enabled: Boolean = false
    ): SignResult {
        try {
            val keystoreFile = File(keystorePath)
            if (!keystoreFile.exists()) {
                return SignResult(false, "El archivo de Keystore especificado no existe.")
            }

            val keystoreType = if (keystorePath.endsWith(".jks", ignoreCase = true)) "JKS" else "PKCS12"
            val ks = KeyStore.getInstance(keystoreType)
            FileInputStream(keystoreFile).use { fis ->
                ks.load(fis, keystorePass.toCharArray())
            }

            if (!ks.containsAlias(alias)) {
                return SignResult(false, "El alias '$alias' no existe en el Keystore.")
            }

            val privateKey = ks.getKey(alias, keyPass.toCharArray()) as? PrivateKey
                ?: return SignResult(false, "No se pudo recuperar la llave privada. Contraseña de llave incorrecta.")
            
            val certChain = ks.getCertificateChain(alias)
            if (certChain == null || certChain.isEmpty()) {
                return SignResult(false, "El Keystore no contiene un certificado para el alias '$alias'.")
            }
            
            val certList = certChain.mapNotNull { it as? X509Certificate }
            if (certList.isEmpty()) {
                return SignResult(false, "El certificado no es del formato X509Certificate esperado.")
            }

            val sourceFile = File(inputApkPath)
            if (!sourceFile.exists()) {
                return SignResult(false, "El archivo APK de origen no existe.")
            }

            val destFile = File(outputApkPath)
            if (destFile.exists()) destFile.delete()

            // 1. Create a SignerConfig
            val signerConfig = com.android.apksig.ApkSigner.SignerConfig.Builder(alias, privateKey, certList)
                .build()

            val signers = listOf(signerConfig)

            // 2. Build the ApkSigner
            val builder = com.android.apksig.ApkSigner.Builder(signers)
                .setInputApk(sourceFile)
                .setOutputApk(destFile)
                .setV1SigningEnabled(v1Enabled)
                .setV2SigningEnabled(v2Enabled)
                .setV3SigningEnabled(v3Enabled)
                .setV4SigningEnabled(v4Enabled)

            if (v4Enabled) {
                val idsigFile = File(outputApkPath + ".idsig")
                if (idsigFile.exists()) idsigFile.delete()
                builder.setV4SignatureOutputFile(idsigFile)
            }

            val signer = builder.build()
            signer.sign()

            val successMsg = StringBuilder("APK firmado con éxito utilizando el alias '$alias'.")
            val activeSchemes = mutableListOf<String>()
            if (v1Enabled) activeSchemes.add("v1 (JAR)")
            if (v2Enabled) activeSchemes.add("v2")
            if (v3Enabled) activeSchemes.add("v3")
            if (v4Enabled) activeSchemes.add("v4")
            if (activeSchemes.isNotEmpty()) {
                successMsg.append("\nEsquemas de firma activos: ").append(activeSchemes.joinToString(", "))
            }
            if (v4Enabled) {
                successMsg.append("\nSe generó un archivo de firma incremental externa: ${destFile.name}.idsig")
            }

            return SignResult(true, successMsg.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            return SignResult(false, "Error al firmar el APK: ${e.localizedMessage}", e)
        }
    }
}
