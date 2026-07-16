package com.example.security

import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.Date

class CertificateService {
    fun generateSelfSignedCertificate(
        alias: String,
        keyPair: KeyPair,
        validityYears: Int,
        algorithm: String,
        cn: String,
        ou: String,
        o: String,
        l: String,
        st: String,
        c: String
    ): X509Certificate {
        // 1. Build Distinguished Name (DN)
        val dnBuilder = X500NameBuilder(BCStyle.INSTANCE)
        val hasAnyField = cn.isNotBlank() || ou.isNotBlank() || o.isNotBlank() || 
                          l.isNotBlank() || st.isNotBlank() || c.isNotBlank()
        
        if (hasAnyField) {
            if (cn.isNotBlank()) dnBuilder.addRDN(BCStyle.CN, cn)
            if (ou.isNotBlank()) dnBuilder.addRDN(BCStyle.OU, ou)
            if (o.isNotBlank()) dnBuilder.addRDN(BCStyle.O, o)
            if (l.isNotBlank()) dnBuilder.addRDN(BCStyle.L, l)
            if (st.isNotBlank()) dnBuilder.addRDN(BCStyle.ST, st)
            if (c.isNotBlank()) dnBuilder.addRDN(BCStyle.C, c)
        } else {
            dnBuilder.addRDN(BCStyle.CN, alias)
            dnBuilder.addRDN(BCStyle.O, "Android KeyStore Generator")
            dnBuilder.addRDN(BCStyle.C, "US")
        }
        val subject = dnBuilder.build()
        val issuer = subject // Self-signed

        // 2. Define Certificate dates
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis() + SecureRandom().nextInt(10000).toLong())
        val notBefore = Date()
        val calendar = Calendar.getInstance()
        calendar.time = notBefore
        calendar.add(Calendar.YEAR, validityYears)
        val notAfter = calendar.time

        // 3. Select signature algorithm
        val signatureAlgorithm = if (algorithm.equals("RSA", ignoreCase = true)) {
            "SHA256WithRSAEncryption"
        } else {
            "SHA256withECDSA"
        }

        // 4. Build certificate
        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder(signatureAlgorithm)
            .setProvider("BC")
            .build(keyPair.private)

        val certHolder = certBuilder.build(contentSigner)
        return JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder)
    }
}
