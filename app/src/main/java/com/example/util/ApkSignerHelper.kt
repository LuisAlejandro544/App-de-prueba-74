package com.example.util

import android.content.Context
import android.util.Base64
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.*
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.security.MessageDigest
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
        keyPass: String
    ): SignResult {
        var zipIn: ZipInputStream? = null
        var zipOut: ZipOutputStream? = null
        try {
            // 1. Load the Keystore and private key + certificate
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
            val cert = certChain[0] as? X509Certificate
                ?: return SignResult(false, "El certificado no es del formato X509Certificate esperado.")

            // 2. Setup streams
            val sourceFile = File(inputApkPath)
            if (!sourceFile.exists()) {
                return SignResult(false, "El archivo APK de origen no existe.")
            }

            zipIn = ZipInputStream(BufferedInputStream(FileInputStream(sourceFile)))
            val destFile = File(outputApkPath)
            if (destFile.exists()) destFile.delete()
            zipOut = ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile)))

            val fileDigests = mutableMapOf<String, String>()
            val buffer = ByteArray(10240)

            // 3. Copy all normal zip entries (excluding existing signatures) and compute digests
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val name = entry.name
                
                // Exclude any existing signatures or signing files in META-INF/
                val isSignatureFile = name.startsWith("META-INF/", ignoreCase = true) && (
                    name.endsWith(".SF", ignoreCase = true) ||
                    name.endsWith(".RSA", ignoreCase = true) ||
                    name.endsWith(".DSA", ignoreCase = true) ||
                    name.endsWith(".EC", ignoreCase = true) ||
                    name.startsWith("META-INF/SIG-", ignoreCase = true)
                )

                if (!isSignatureFile && !entry.isDirectory) {
                    val outEntry = ZipEntry(name)
                    outEntry.method = entry.method
                    if (entry.method == ZipEntry.STORED) {
                        outEntry.size = entry.size
                        outEntry.compressedSize = entry.compressedSize
                        outEntry.crc = entry.crc
                    }
                    zipOut.putNextEntry(outEntry)

                    val md = MessageDigest.getInstance("SHA-256")
                    var len: Int
                    while (zipIn.read(buffer).also { len = it } > 0) {
                        zipOut.write(buffer, 0, len)
                        md.update(buffer, 0, len)
                    }
                    zipOut.closeEntry()

                    val digestBase64 = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                    fileDigests[name] = digestBase64
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            // 4. Generate MANIFEST.MF
            val manifest = Manifest()
            manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            manifest.mainAttributes[Attributes.Name("Created-By")] = "1.0 (Keystore-Manager-Android)"

            for ((filename, digest) in fileDigests) {
                val attrs = Attributes()
                attrs[Attributes.Name("SHA-256-Digest")] = digest
                manifest.entries[filename] = attrs
            }

            val manifestBytes = ByteArrayOutputStream().use { baos ->
                manifest.write(baos)
                baos.toByteArray()
            }

            // Write MANIFEST.MF
            zipOut.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zipOut.write(manifestBytes)
            zipOut.closeEntry()

            // 5. Generate Signature File (KEY.SF)
            val manifestDigest = MessageDigest.getInstance("SHA-256").digest(manifestBytes)
            val manifestDigestBase64 = Base64.encodeToString(manifestDigest, Base64.NO_WRAP)

            val sf = StringBuilder()
            sf.append("Signature-Version: 1.0\r\n")
            sf.append("Created-By: 1.0 (Keystore-Manager-Android)\r\n")
            sf.append("SHA-256-Digest-Manifest: $manifestDigestBase64\r\n\r\n")

            // Individual SF entries
            for ((filename, _) in fileDigests) {
                // Read manifest block for this file
                val blockBuilder = StringBuilder()
                blockBuilder.append("Name: $filename\r\n")
                blockBuilder.append("SHA-256-Digest: ${fileDigests[filename]}\r\n\r\n")
                
                val blockBytes = blockBuilder.toString().toByteArray(Charsets.UTF_8)
                val entryDigest = MessageDigest.getInstance("SHA-256").digest(blockBytes)
                val entryDigestBase64 = Base64.encodeToString(entryDigest, Base64.NO_WRAP)

                sf.append("Name: $filename\r\n")
                sf.append("SHA-256-Digest: $entryDigestBase64\r\n\r\n")
            }

            val sfBytes = sf.toString().toByteArray(Charsets.UTF_8)

            // Write KEY.SF
            zipOut.putNextEntry(ZipEntry("META-INF/KEY.SF"))
            zipOut.write(sfBytes)
            zipOut.closeEntry()

            // 6. Generate Signature Block (KEY.RSA) using Bouncy Castle
            val signatureAlgorithm = when (privateKey.algorithm) {
                "EC" -> "SHA256withECDSA"
                else -> "SHA256withRSA"
            }

            val generator = CMSSignedDataGenerator()
            generator.addSignerInfoGenerator(
                JcaSimpleSignerInfoGeneratorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(signatureAlgorithm, privateKey, cert)
            )

            val certList = listOf(cert)
            val certsStore = org.bouncycastle.cert.jcajce.JcaCertStore(certList)
            generator.addCertificates(certsStore)

            val processable = CMSProcessableByteArray(sfBytes)
            val signedData = generator.generate(processable, true)
            val rsaBytes = signedData.encoded

            // Write KEY.RSA
            zipOut.putNextEntry(ZipEntry("META-INF/KEY.RSA"))
            zipOut.write(rsaBytes)
            zipOut.closeEntry()

            return SignResult(true, "APK firmado con éxito utilizando el alias '$alias'.")
        } catch (e: Exception) {
            e.printStackTrace()
            return SignResult(false, "Error al firmar el APK: ${e.localizedMessage}", e)
        } finally {
            try { zipIn?.close() } catch (ignored: Exception) {}
            try { zipOut?.close() } catch (ignored: Exception) {}
        }
    }
}
