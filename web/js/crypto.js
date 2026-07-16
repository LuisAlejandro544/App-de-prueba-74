/**
 * Keystore Companion Web - Cryptography Module (crypto.js)
 * High-performance, 100% client-side cryptographic utilities using node-forge.
 */

window.CryptoModule = (function() {
    const forge = window.forge;

    return {
        /**
         * Hashes a string (such as a PIN) with SHA-256 and an optional dynamic salt.
         */
        hashSha256: function(input, salt = '') {
            try {
                const md = forge.md.sha256.create();
                md.update(input + salt);
                return md.digest().toHex();
            } catch (e) {
                console.error("Error hashing SHA-256", e);
                return null;
            }
        },

        /**
         * Generates a random cryptographic salt of specified length.
         */
        generateSalt: function(length = 16) {
            try {
                const bytes = forge.random.getBytesSync(length);
                return forge.util.bytesToHex(bytes);
            } catch (e) {
                // Fallback if random bytes fails
                return Math.random().toString(36).substring(2, 18);
            }
        },

        /**
         * Generates a new RSA key pair and a self-signed X.509 certificate.
         */
        generateKeyPairAndCert: function(params) {
            const { cn, org, country, size, validityYears } = params;
            
            // 1. Generate RSA Key Pair
            const keys = forge.pki.rsa.generateKeyPair(size);
            
            // 2. Create self-signed X.509 Certificate
            const cert = forge.pki.createCertificate();
            cert.publicKey = keys.publicKey;
            cert.serialNumber = '01' + Math.floor(Math.random() * 1000000);
            
            cert.validity.notBefore = new Date();
            const notAfter = new Date();
            notAfter.setFullYear(notAfter.getFullYear() + validityYears);
            cert.validity.notAfter = notAfter;

            const attrs = [
                { name: 'commonName', value: cn },
                { name: 'organizationName', value: org },
                { name: 'countryName', value: country }
            ];
            cert.setSubject(attrs);
            cert.setIssuer(attrs);

            // Self-sign certificate with the private key
            cert.sign(keys.privateKey, forge.md.sha256.create());

            return {
                privateKey: keys.privateKey,
                publicKey: keys.publicKey,
                certificate: cert,
                pemPrivateKey: forge.pki.privateKeyToPem(keys.privateKey),
                pemCertificate: forge.pki.certificateToPem(cert)
            };
        },

        /**
         * Packs a Private Key and X.509 Certificate into a PKCS12 Keystore (.p12) binary.
         */
        createPkcs12Keystore: function(privateKey, cert, password, alias) {
            try {
                const p12Asn1 = forge.pkcs12.toPkcs12Asn1(
                    privateKey, 
                    [cert], 
                    password, 
                    {
                        friendlyName: alias,
                        algorithm: '3des'
                    }
                );
                const p12Der = forge.asn1.toDer(p12Asn1).getBytes();
                
                // Convert binary string to Uint8Array for file saving
                const buffer = new Uint8Array(p12Der.length);
                for (let i = 0; i < p12Der.length; i++) {
                    buffer[i] = p12Der.charCodeAt(i);
                }
                return buffer;
            } catch (e) {
                console.error("Error creating PKCS12 store", e);
                throw e;
            }
        },

        /**
         * Decodes and parses a PKCS12 Keystore (.p12 / .keystore) file directly in the browser.
         */
        parsePkcs12Keystore: function(arrayBuffer, password) {
            try {
                const binaryString = forge.util.createBuffer(new Uint8Array(arrayBuffer)).getBytes();
                const p12 = forge.pkcs12.fromPkcs12Asn1(forge.asn1.fromDer(binaryString), false, password);
                
                // Gather bags
                const bags = p12.getBags({ bagType: forge.pki.oids.certBag });
                const certBag = bags[forge.pki.oids.certBag];
                
                if (!certBag || certBag.length === 0) {
                    throw new Error("No se encontraron certificados en este almacén.");
                }

                // Extract alias and certificate from first bag
                const firstBag = certBag[0];
                const alias = firstBag.attributes.friendlyName ? firstBag.attributes.friendlyName[0] : 'Desconocido';
                const cert = firstBag.cert;
                
                const pemCert = forge.pki.certificateToPem(cert);
                const details = this.parsePemCertificate(pemCert);
                
                return {
                    success: true,
                    alias: alias,
                    certificate: cert,
                    pemCertificate: pemCert,
                    details: details
                };
            } catch (e) {
                console.error("Error parsing PKCS12", e);
                return {
                    success: false,
                    error: e.message || "Contraseña incorrecta o archivo Keystore inválido."
                };
            }
        },

        /**
         * Decodes an X.509 Certificate from PEM format and computes its properties and fingerprints.
         */
        parsePemCertificate: function(pemString) {
            try {
                const cert = forge.pki.certificateFromPem(pemString);
                
                const subjectStr = cert.subject.attributes.map(a => `${a.shortName || a.name}=${a.value}`).join(', ');
                const issuerStr = cert.issuer.attributes.map(a => `${a.shortName || a.name}=${a.value}`).join(', ');
                
                // Get DER representation for finger-printing
                const derBytes = forge.asn1.toDer(forge.pki.certificateToAsn1(cert)).getBytes();
                
                // SHA-256 fingerprint
                const sha256 = forge.md.sha256.create();
                sha256.update(derBytes);
                const fingerprint256 = sha256.digest().toHex().match(/.{1,2}/g).join(':').toUpperCase();

                // SHA-1 fingerprint
                const sha1 = forge.md.sha1.create();
                sha1.update(derBytes);
                const fingerprint1 = sha1.digest().toHex().match(/.{1,2}/g).join(':').toUpperCase();

                // MD5 fingerprint
                const md5 = forge.md.md5.create();
                md5.update(derBytes);
                const fingerprintMd5 = md5.digest().toHex().match(/.{1,2}/g).join(':').toUpperCase();

                return {
                    success: true,
                    subject: subjectStr || 'Desconocido',
                    issuer: issuerStr || 'Desconocido',
                    serialNumber: cert.serialNumber || '0',
                    validFrom: cert.validity.notBefore.toLocaleString(),
                    validTo: cert.validity.notAfter.toLocaleString(),
                    signatureAlgorithm: cert.siginfo.algorithmOid || 'sha256WithRSAEncryption',
                    sha256: fingerprint256,
                    sha1: fingerprint1,
                    md5: fingerprintMd5
                };
            } catch (e) {
                console.error("Error parsing PEM", e);
                return {
                    success: false,
                    error: "Formato PEM de certificado inválido."
                };
            }
        }
    };
})();
