/**
 * Keystore Companion Web - App Orchestrator (app.js)
 * Manages security states (PIN lock), file inputs, keystore exports, and tab bindings.
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialise Security System (Optional PIN protection, identical to Android app)
    initSecurityPin();

    // 2. Set up Drag-and-Drop and File Inputs for Keystore Inspector
    initInspectorUploads();

    // 3. Connect UI Listeners and Command composers
    initCommandComposers();

    // 4. Set Default Tab on Launch
    switchTab('gen');
});

// Cache for generated values
let generatedKeypair = null;
let importedKeystore = null;

/**
 * Handles the optional 4-digit PIN setup and entry, matching Android's security.
 */
function initSecurityPin() {
    const hashKey = 'companion_pin_hash';
    const saltKey = 'companion_pin_salt';
    
    const lockOverlay = document.getElementById('pin-lock-overlay');
    const pinTitle = document.getElementById('pin-title');
    const pinInput = document.getElementById('pin-input-field');
    const pinActionBtn = document.getElementById('btn-pin-action');
    const pinSkipBtn = document.getElementById('btn-pin-skip');
    
    const hasPin = localStorage.getItem(hashKey);

    if (!hasPin) {
        // Mode: Create PIN
        pinTitle.innerText = "Configurar PIN de Seguridad (Web)";
        pinInput.placeholder = "Crea tu PIN de 4 dígitos";
        pinActionBtn.innerText = "Guardar y Proteger";
        if (pinSkipBtn) pinSkipBtn.classList.remove('hidden');
    } else {
        // Mode: Enter PIN
        pinTitle.innerText = "Desbloquear Keystore Companion";
        pinInput.placeholder = "Introduce tu PIN de 4 dígitos";
        pinActionBtn.innerText = "Desbloquear";
        if (pinSkipBtn) pinSkipBtn.classList.add('hidden'); // Cannot skip once configured
    }

    // Bind action
    pinActionBtn.addEventListener('click', () => {
        const pin = pinInput.value.trim();
        if (pin.length < 4 || isNaN(pin)) {
            window.UIModule.showSnackbar("Por favor, introduce un PIN numérico de al menos 4 dígitos.", "error");
            return;
        }

        if (!hasPin) {
            // Registering PIN
            const salt = window.CryptoModule.generateSalt();
            const hash = window.CryptoModule.hashSha256(pin, salt);
            localStorage.setItem(hashKey, hash);
            localStorage.setItem(saltKey, salt);
            window.UIModule.showSnackbar("¡PIN de Seguridad guardado localmente!", "success");
            lockOverlay.remove();
        } else {
            // Verifying PIN
            const salt = localStorage.getItem(saltKey) || '';
            const correctHash = localStorage.getItem(hashKey);
            const inputHash = window.CryptoModule.hashSha256(pin, salt);

            if (inputHash === correctHash) {
                window.UIModule.showSnackbar("Acceso Autorizado", "success");
                lockOverlay.remove();
            } else {
                window.UIModule.showSnackbar("PIN de Seguridad incorrecto.", "error");
                pinInput.value = '';
                pinInput.focus();
            }
        }
    });

    if (pinSkipBtn) {
        pinSkipBtn.addEventListener('click', () => {
            window.UIModule.showSnackbar("Omitiendo protección local por PIN.", "warn");
            lockOverlay.remove();
        });
    }

    // Support Enter Key
    pinInput.addEventListener('keyup', (e) => {
        if (e.key === 'Enter') {
            pinActionBtn.click();
        }
    });
}

/**
 * Binds drag-and-drop zones for Keystores (.p12 / .keystore) and Certificates (.pem).
 */
function initInspectorUploads() {
    // Drop Zone for PKCS12 Keystores (.p12, .keystore)
    window.UIModule.initDragAndDrop('inspector-drop-zone', (file) => {
        const reader = new FileReader();
        reader.onload = function(e) {
            const arrayBuffer = e.target.result;
            promptForKeystorePassword(arrayBuffer, file.name);
        };
        reader.readAsArrayBuffer(file);
    });

    // Custom Input for raw PEM Certificates
    const parseBtn = document.getElementById('btn-parse-pem');
    if (parseBtn) {
        parseBtn.addEventListener('click', () => {
            const rawPem = document.getElementById('inp-cert').value.trim();
            if (!rawPem) {
                window.UIModule.showSnackbar("Ingresa un certificado PEM válido antes de continuar.", "warn");
                return;
            }
            const res = window.CryptoModule.parsePemCertificate(rawPem);
            if (res.success) {
                displayCertificateDetails(res);
                window.UIModule.showSnackbar("Certificado decodificado con éxito.", "success");
            } else {
                window.UIModule.showSnackbar(res.error, "error");
            }
        });
    }
}

/**
 * Prompts user with overlay for Keystore password to parse it locally.
 */
function promptForKeystorePassword(arrayBuffer, fileName) {
    const overlay = document.getElementById('password-prompt-overlay');
    const pwdInput = document.getElementById('keystore-password-input');
    const unlockBtn = document.getElementById('btn-unlock-keystore');
    const cancelBtn = document.getElementById('btn-cancel-unlock');

    overlay.classList.remove('hidden');
    pwdInput.value = '';
    pwdInput.focus();

    // Handle Unlocking
    const handleUnlock = () => {
        const password = pwdInput.value;
        overlay.classList.add('hidden');
        
        // Show scanning state
        window.UIModule.showSnackbar("Analizando Keystore...", "warn");
        
        setTimeout(() => {
            const result = window.CryptoModule.parsePkcs12Keystore(arrayBuffer, password);
            if (result.success) {
                window.UIModule.showSnackbar(`¡Almacén desbloqueado! Alias: ${result.alias}`, "success");
                importedKeystore = {
                    buffer: arrayBuffer,
                    password: password,
                    alias: result.alias,
                    pem: result.pemCertificate
                };
                displayCertificateDetails(result.details, result.alias);
                
                // Prefill the Play Store PEPK tool with this keystore info!
                document.getElementById('pepk-alias').value = result.alias;
                document.getElementById('pepk-jks').value = fileName;
                window.UIModule.updatePepkCommand();
            } else {
                window.UIModule.showSnackbar(result.error, "error");
            }
        }, 100);
    };

    unlockBtn.onclick = handleUnlock;
    cancelBtn.onclick = () => overlay.classList.add('hidden');
    pwdInput.onkeyup = (e) => {
        if (e.key === 'Enter') handleUnlock();
    };
}

/**
 * Renders decoded certificate metrics to the Inspector UI.
 */
function displayCertificateDetails(details, alias = null) {
    document.getElementById('inspect-subject').innerText = details.subject;
    document.getElementById('inspect-issuer').innerText = details.issuer;
    document.getElementById('inspect-serial').innerText = details.serialNumber;
    document.getElementById('inspect-notbefore').innerText = details.validFrom;
    document.getElementById('inspect-notafter').innerText = details.validTo;
    document.getElementById('inspect-sigalgo').innerText = details.signatureAlgorithm;
    document.getElementById('inspect-sha256').innerText = details.sha256;
    document.getElementById('inspect-sha1').innerText = details.sha1;
    document.getElementById('inspect-md5').innerText = details.md5;

    // Show export buttons if loaded from actual keystore
    const exportDiv = document.getElementById('inspector-export-actions');
    if (alias && exportDiv) {
        exportDiv.classList.remove('hidden');
    }
}

/**
 * Hooks up actions for generating, exporting, copying, and filling PEPK.
 */
function initCommandComposers() {
    // 1. Generate Keypair + Certificate
    const btnGenerate = document.getElementById('btn-generate');
    if (btnGenerate) {
        btnGenerate.addEventListener('click', runKeyGenerationFlow);
    }

    // 2. Clear Security PIN
    const btnResetPin = document.getElementById('btn-reset-pin');
    if (btnResetPin) {
        btnResetPin.addEventListener('click', () => {
            if (confirm("¿Estás seguro de que deseas restablecer tu PIN de acceso local? Deberás configurar uno nuevo.")) {
                localStorage.removeItem('companion_pin_hash');
                localStorage.removeItem('companion_pin_salt');
                window.UIModule.showSnackbar("PIN de seguridad eliminado. Recarga la página para reconfigurarlo.", "warn");
                setTimeout(() => location.reload(), 1500);
            }
        });
    }

    // 3. PEPK Interactive Inputs
    const pepkInputs = ['pepk-alias', 'pepk-jks', 'pepk-output'];
    pepkInputs.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('input', window.UIModule.updatePepkCommand);
        }
    });

    // 4. Download Export Handlers
    document.getElementById('btn-dl-cert')?.addEventListener('click', () => {
        if (!generatedKeypair) return;
        window.UIModule.downloadFile(generatedKeypair.pemCertificate, 'certificate.pem');
    });

    document.getElementById('btn-dl-key')?.addEventListener('click', () => {
        if (!generatedKeypair) return;
        window.UIModule.downloadFile(generatedKeypair.pemPrivateKey, 'private_key.pem');
    });

    document.getElementById('btn-dl-store')?.addEventListener('click', () => {
        if (!generatedKeypair) return;
        const password = document.getElementById('gen-password').value || 'android';
        const alias = document.getElementById('gen-alias').value || 'mykey';
        
        try {
            const buffer = window.CryptoModule.createPkcs12Keystore(
                generatedKeypair.privateKey,
                generatedKeypair.certificate,
                password,
                alias
            );
            window.UIModule.downloadFile(buffer, `${alias}.keystore`, true);
        } catch (e) {
            window.UIModule.showSnackbar("Error al generar contenedor Keystore (.keystore): " + e.message, "error");
        }
    });

    // From Inspector Export Certificate PEM
    document.getElementById('btn-inspect-dl-cert')?.addEventListener('click', () => {
        if (!importedKeystore) return;
        window.UIModule.downloadFile(importedKeystore.pem, `${importedKeystore.alias}_cert.pem`);
    });
}

/**
 * Runs keypair creation on secondary thread timeout to prevent blocking browser rendering.
 */
function runKeyGenerationFlow() {
    const cn = document.getElementById('gen-cn').value.trim() || 'Android Developer';
    const org = document.getElementById('gen-org').value.trim() || 'My App Development';
    const country = document.getElementById('gen-country').value.trim() || 'ES';
    const size = parseInt(document.getElementById('gen-size').value) || 2048;
    const validityYears = parseInt(document.getElementById('gen-validity').value) || 25;
    const alias = document.getElementById('gen-alias').value.trim() || 'mykey';

    const btnText = document.getElementById('gen-text');
    const spinner = document.getElementById('gen-spinner');
    const btn = document.getElementById('btn-generate');

    // Show loading
    btnText.innerText = "Calculando Números Primos...";
    spinner.classList.remove('hidden');
    btn.disabled = true;

    setTimeout(() => {
        try {
            const keypair = window.CryptoModule.generateKeyPairAndCert({
                cn, org, country, size, validityYears
            });

            generatedKeypair = keypair;

            // Output text areas
            document.getElementById('out-cert').value = keypair.pemCertificate;
            document.getElementById('out-key').value = keypair.pemPrivateKey;

            // Show export block
            document.getElementById('export-actions-card').classList.remove('hidden');

            window.UIModule.showSnackbar("¡Par de llaves y certificado auto-firmado creados con éxito!", "success");
        } catch (err) {
            window.UIModule.showSnackbar("Error en generación: " + err.message, "error");
        } finally {
            btnText.innerText = "Generar Par de Llaves Criptográficas";
            spinner.classList.add('hidden');
            btn.disabled = false;
        }
    }, 120);
}

/**
 * Switches between workspace tabs seamlessly.
 */
function switchTab(tabId) {
    const tabs = ['gen', 'inspect', 'play', 'settings'];
    tabs.forEach(t => {
        const sec = document.getElementById('tab-' + t);
        const btn = document.getElementById('btn-tab-' + t);
        if (!sec) return;

        if (t === tabId) {
            sec.classList.remove('hidden');
            if (btn) {
                btn.className = "px-4 py-2.5 text-xs font-bold rounded-xl transition-all duration-200 bg-brand-600 text-white shadow-md shadow-brand-950/20 flex items-center space-x-1.5 border border-brand-500/10";
            }
        } else {
            sec.classList.add('hidden');
            if (btn) {
                btn.className = "px-4 py-2.5 text-xs font-semibold rounded-xl transition-all duration-200 text-slateDark-400 hover:text-slateDark-100 flex items-center space-x-1.5";
            }
        }
    });
}
window.switchTab = switchTab; // Expose to global scope for HTML clicks
