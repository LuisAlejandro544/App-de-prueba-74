# Hoja de Ruta (Roadmap) - Guardián de Firmas

Este documento resume los hitos de desarrollo completados, el estado actual de la aplicación y las futuras mejoras recomendadas para expandir la utilidad de la plataforma.

---

## 🗺️ Fases de Desarrollo

### 🟢 Fase 1: Cimientos y Generación Básica (Completado)
- [x] Configuración inicial de arquitectura limpia con Kotlin y Jetpack Compose.
- [x] Interfaz de diseño fluida Material Design 3 con colores amigables de baja fatiga visual.
- [x] Creación de archivos en almacenamiento local privado de la app.
- [x] Integración de Room para registrar los metadatos de las firmas generadas.

### 🟡 Fase 2: Robustez y Seguridad Avanzada (Completado)
- [x] **Clasificación por Categorías**: Separación formal en llaves de **DEBUG**, **PRODUCCIÓN** y **PRUEBA**.
- [x] **Atajo de Firma Debug**: Plantilla de carga instantánea con contraseñas y alias estándar de Android.
- [x] **Gobernanza con PIN**: Pantalla de bloqueo con hash SHA-256 + sal dinámica en `SharedPreferences`.
- [x] **Exportación Directa**: Lógica nativa de guardado seguro compatible con Scoped Storage en la carpeta `/Download/Keystores/` del sistema.
- [x] **Multihilo Inteligente**: Delegación de la lógica de cifrado criptográfico a hilos secundarios no bloqueantes de la CPU (`Dispatchers.Default`).

### 🔵 Fase 3: Interoperabilidad y Diagnóstico Avanzado (Completado)
- [x] **Inspector de Keystores**: Visualización de huellas dactilares (SHA-1, SHA-256, MD5) y extracción de metadatos de llaves externas.
- [x] **Conversor de Formatos**: Transformación bidireccional entre formatos JKS, PKCS12 y JCEKS.
- [x] **Firmado Multiesquema (v1, v2, v3, v4)**: Firma nativa de binarios de Android seleccionando y combinando múltiples esquemas de firma e integrando la biblioteca oficial `apksig` de Google.
- [x] **Prueba de Red (Ping CI/CD)**: Medidor de latencia RTT y comprobación de puertos para servidores de despliegue y repositorios remotos.
- [x] **Asistente Google Play App Signing**: Flujo interactivo paso a paso para configurar llaves de subida, exportar certificados `.pem` y generar comandos PEPK dinámicos.

### 🟣 Fase 4: Seguridad Biométrica y Respaldo (Completado y Largo Plazo)
- [x] **Autenticación Biométrica**: Acceso instantáneo configurable en ajustes mediante reconocimiento de huella o rostro integrado con `BiometricPrompt`.
- [x] **Reconfiguración de PIN Segura**: Borrado instantáneo y re-bloqueo para actualizar la contraseña desde el menú de Ajustes de la pantalla de inicio.
- [ ] **Sincronización en la Nube Encriptada**: Almacenamiento en la nube (como Google Drive) encriptando las llaves del lado del cliente mediante una clave derivada del PIN del usuario antes de la subida.
- [ ] **Cuentas de Respaldo**: Códigos de recuperación únicos para restablecer el PIN de seguridad de la aplicación si el usuario lo olvida, previniendo la pérdida de acceso al historial local.

### 🟤 Fase 5: Expansión Multiplataforma y Web Companion (Completado)
- [x] **Web Companion Estático**: Creación de una versión web complementaria en `/web/index.html` construida sobre Tailwind CSS y `node-forge`.
- [x] **Criptografía 100% Local**: Implementación de algoritmos locales de generación RSA, firmas X.509 PEM y análisis de huellas dactilares criptográficas en el navegador sin almacenamiento en servidor.
- [ ] **Soporte para PC y Escritorio**: Migrar la interfaz de usuario de Android a **Compose Multiplatform** para compilar versiones de escritorio nativas en Windows, macOS y Linux.
- [ ] **Migración a Kotlin/Wasm**: Evolucionar la lógica actual de JS a un binario compilado de WebAssembly en Kotlin Multiplatform para un rendimiento nativo.

