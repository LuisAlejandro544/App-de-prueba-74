package com.example.security

import android.content.Context
import java.io.File

object KeystoreGenerator {

    private val keyPairService = KeyPairService()
    private val certificateService = CertificateService()
    private val keystoreStorageService = KeystoreStorageService()

    init {
        BouncyCastleManager.setupBouncyCastle()
    }

    /**
     * Generates a PKCS12 compliant Keystore file.
     *
     * @return The absolute path of the generated file
     */
    fun generate(
        context: Context,
        alias: String,
        storePassword: String,
        keyPassword: String,
        validityYears: Int,
        algorithm: String, // "RSA" or "EC"
        keySize: Int, // 2048, 4096 or 256, 384
        cn: String, // Common Name (e.g., Pac Mesa)
        ou: String, // Org Unit (e.g., Development)
        o: String,  // Org (e.g., Android Apps)
        l: String,  // Locality/City
        st: String, // State
        c: String,  // Country Code (e.g., ES)
        extension: String // ".jks" or ".keystore"
    ): File {
        // Ensure provider is initialized
        BouncyCastleManager.setupBouncyCastle()

        // 1. Generate KeyPair
        val keyPair = keyPairService.generateKeyPair(algorithm, keySize)

        // 2. Generate Certificate
        val certificate = certificateService.generateSelfSignedCertificate(
            alias = alias,
            keyPair = keyPair,
            validityYears = validityYears,
            algorithm = algorithm,
            cn = cn,
            ou = ou,
            o = o,
            l = l,
            st = st,
            c = c
        )

        // 3. Save Keystore File
        val certChain = arrayOf<java.security.cert.Certificate>(certificate)
        return keystoreStorageService.saveKeystore(
            context = context,
            alias = alias,
            privateKey = keyPair.private,
            certificateChain = certChain,
            storePassword = storePassword,
            keyPassword = keyPassword,
            extension = extension
        )
    }
}
