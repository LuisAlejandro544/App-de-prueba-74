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
- [x] **Firmado y Verificación de APKs**: Firma nativa de binarios de Android y análisis de firma criptográfica para comprobar integridad.
- [x] **Prueba de Red (Ping CI/CD)**: Medidor de latencia RTT y comprobación de puertos para servidores de despliegue y repositorios remotos.

### 🟣 Fase 4: Sincronización y Respaldo Externo (Largo Plazo)
- [ ] **Sincronización en la Nube Encriptada**: Almacenamiento en la nube (como Google Drive) encriptando las llaves del lado del cliente mediante una clave derivada del PIN del usuario antes de la subida.
- [ ] **Generación de Firmas para Google Play**: Asistente detallado con flujos recomendados para el nuevo estándar "App Signing by Google Play".
- [ ] **Cuentas de Respaldo**: Códigos de recuperación únicos para restablecer el PIN de seguridad de la aplicación si el usuario lo olvida, previniendo la pérdida de acceso al historial local.

### 🟤 Fase 5: Expansión Multiplataforma (Futuro)
- [ ] **Soporte para PC y Escritorio**: Migrar la interfaz de usuario a **Compose Multiplatform** para compilar versiones de escritorio nativas en Windows, macOS y Linux.
- [ ] **Soporte para Navegador (Web)**: Implementar una versión web autohospedada utilizando **Kotlin/Wasm** o **Compose para Web**, permitiendo a los desarrolladores generar sus llaves directamente desde el navegador de manera segura y 100% local (Client-Side) sin enviar información al servidor.

