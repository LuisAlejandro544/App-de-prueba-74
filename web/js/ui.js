/**
 * Keystore Companion Web - UI & Presentation Module (ui.js)
 * Coordinates feedback notifications, dynamic forms, file uploading, and downloads.
 */

window.UIModule = (function() {
    return {
        /**
         * Triggers a temporary animated snackbar notification.
         */
        showSnackbar: function(message, type = 'success') {
            const container = document.getElementById('snackbar-container');
            if (!container) return;

            const snackbar = document.createElement('div');
            let bgClass = 'bg-slateDark-900 border-brand-500/30 text-brand-400';
            let icon = `<svg class="w-4 h-4 text-brand-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>`;

            if (type === 'success') {
                bgClass = 'bg-slateDark-900 border-emerald-500/30 text-emerald-400';
                icon = `<svg class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>`;
            } else if (type === 'error') {
                bgClass = 'bg-slateDark-900 border-rose-500/30 text-rose-400';
                icon = `<svg class="w-4 h-4 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>`;
            } else if (type === 'warn') {
                bgClass = 'bg-slateDark-900 border-amber-500/30 text-amber-400';
                icon = `<svg class="w-4 h-4 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>`;
            }

            snackbar.className = `flex items-center space-x-3 px-4 py-3 rounded-xl border ${bgClass} shadow-xl transform translate-y-2 opacity-0 transition-all duration-300 max-w-sm`;
            snackbar.innerHTML = `
                <div class="flex-shrink-0">${icon}</div>
                <div class="text-xs font-semibold flex-grow leading-snug">${message}</div>
                <button class="text-slateDark-400 hover:text-white transition" onclick="this.parentElement.remove()">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
                </button>
            `;

            container.appendChild(snackbar);

            // Animate in
            setTimeout(() => {
                snackbar.classList.remove('translate-y-2', 'opacity-0');
            }, 10);

            // Auto-dismiss
            setTimeout(() => {
                snackbar.classList.add('translate-y-2', 'opacity-0');
                setTimeout(() => {
                    snackbar.remove();
                }, 300);
            }, 4000);
        },

        /**
         * Automatically triggers a file download for binary buffers or strings.
         */
        downloadFile: function(content, fileName, isBinary = false) {
            try {
                let blob;
                if (isBinary) {
                    blob = new Blob([content], { type: 'application/octet-stream' });
                } else {
                    blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
                }

                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                
                setTimeout(() => {
                    document.body.removeChild(a);
                    URL.revokeObjectURL(url);
                }, 0);
                
                this.showSnackbar(`Descargado con éxito: ${fileName}`, 'success');
            } catch (e) {
                this.showSnackbar('Error al iniciar descarga', 'error');
                console.error(e);
            }
        },

        /**
         * Copies text to clipboard and flashes visually.
         */
        copyTextToClipboard: function(text, successMessage = 'Copiado al portapapeles') {
            if (!text) {
                this.showSnackbar('No hay nada para copiar.', 'error');
                return;
            }

            navigator.clipboard.writeText(text).then(() => {
                this.showSnackbar(successMessage, 'success');
            }).catch(err => {
                console.error('Error al copiar', err);
                // Fallback using textarea
                const t = document.createElement('textarea');
                t.value = text;
                document.body.appendChild(t);
                t.select();
                try {
                    document.execCommand('copy');
                    this.showSnackbar(successMessage, 'success');
                } catch (e) {
                    this.showSnackbar('Fallo al copiar', 'error');
                }
                document.body.removeChild(t);
            });
        },

        /**
         * Sets up drag and drop listeners for a zone.
         */
        initDragAndDrop: function(zoneId, onFileLoaded) {
            const zone = document.getElementById(zoneId);
            if (!zone) return;

            ['dragenter', 'dragover'].forEach(eventName => {
                zone.addEventListener(eventName, (e) => {
                    e.preventDefault();
                    zone.classList.add('border-brand-500', 'bg-slateDark-900/60');
                }, false);
            });

            ['dragleave', 'drop'].forEach(eventName => {
                zone.addEventListener(eventName, (e) => {
                    e.preventDefault();
                    zone.classList.remove('border-brand-500', 'bg-slateDark-900/60');
                }, false);
            });

            zone.addEventListener('drop', (e) => {
                const dt = e.dataTransfer;
                const files = dt.files;
                if (files.length > 0) {
                    onFileLoaded(files[0]);
                }
            }, false);

            // Click support as well
            const input = zone.querySelector('input[type="file"]');
            if (input) {
                zone.addEventListener('click', () => input.click());
                input.addEventListener('change', () => {
                    if (input.files.length > 0) {
                        onFileLoaded(input.files[0]);
                    }
                });
            }
        },

        /**
         * Configures helper pre-fills for fast debugging signatures.
         */
        applyDebugTemplate: function() {
            document.getElementById('gen-cn').value = 'Android Debug';
            document.getElementById('gen-org').value = 'Android';
            document.getElementById('gen-country').value = 'US';
            document.getElementById('gen-validity').value = '30';
            document.getElementById('gen-alias').value = 'androiddebugkey';
            document.getElementById('gen-password').value = 'android';
            
            this.showSnackbar('Plantilla Debug aplicada con éxito (androiddebugkey).', 'success');
        },

        /**
         * Computes dynamic PEPK encryption command string.
         */
        updatePepkCommand: function() {
            const alias = document.getElementById('pepk-alias').value.trim() || 'upload';
            const jks = document.getElementById('pepk-jks').value.trim() || 'my-keystore.jks';
            const output = document.getElementById('pepk-output').value.trim() || 'encrypted_key.pepk';
            
            const command = `java -jar pepk.jar --keystore=${jks} --alias=${alias} --output=${output} --encryptionkey=eb10c44155190981222bde1fbb3e3170560b11c08bc0d302 --algorithm=RSA`;
            const display = document.getElementById('pepk-cmd-output');
            if (display) {
                display.innerText = command;
            }
        }
    };
})();
