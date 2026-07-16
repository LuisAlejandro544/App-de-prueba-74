# Guardián de Firmas (Keystore & JKS Generator)

Una aplicación móvil Android nativa construida con Kotlin y Jetpack Compose que permite a los desarrolladores generar, organizar, proteger y exportar llaves criptográficas de firma (`.keystore` y `.jks`).

La aplicación está diseñada pensando en la comodidad del desarrollador, ofreciendo una experiencia limpia que evita la fatiga visual al omitir estéticas cyberpunk/futuristas en favor de tonos pastel suaves y contrastes refinados basados en **Material Design 3**.

---

## 🚀 Características Clave

1. **Generación Segura Multihilo**:
   - Generación inteligente delegada a corrutinas que diferencian entre núcleos de potencia y eficiencia del procesador móvil, evitando congelamientos de la interfaz de usuario (ANRs) y cierres inesperados.
2. **Formato Flexible**:
   - Soporte para exportación en formatos estándar de Android (`.keystore` y `.jks`).
3. **Organización por Categorías**:
   - Clasificación de llaves en **DEBUG**, **PRODUCCIÓN** y **PRUEBA** con visualización diferenciada por etiquetas de color.
4. **Plantilla de Atajo Debug**:
   - Botón de carga instantánea que rellena los campos requeridos para compilar con la firma estándar de depuración de Android (`androiddebugkey`, contraseña `android`, validez de 30 años).
5. **Garantía y Seguridad mediante PIN Obligatorio**:
   - Acceso completamente bloqueado tras una pantalla de protección por PIN.
   - Las contraseñas y el PIN se encriptan utilizando algoritmos de hash unidireccional con sal dinámica (`SHA-256` y sales criptográficas locales) almacenados de forma segura en `SharedPreferences`.
6. **Descarga al Almacenamiento del Sistema**:
   - Integración nativa con `MediaStore` para guardar llaves de forma segura en la carpeta pública `/Download/Keystores/` del gestor de archivos, compatible con Android 10+ (Scoped Storage) y versiones heredadas.
7. **Suite de Herramientas de Firma & Red**:
   - **Inspector de Keystores**: Permite cargar y analizar archivos Keystore existentes para validar contraseñas, ver alias, algoritmos de firma, huellas digitales y fechas de validez.
   - **Convertidor de Formatos**: Conversión ágil e inmediata entre formatos estándar (JKS, PKCS12 y JCEKS).
   - **Verificador de Firmas de APK**: Analiza cualquier archivo APK para inspeccionar y validar su certificado de firma y huellas criptográficas.
   - **Firmador de APKs Nativo**: Permite firmar archivos APK directamente en el dispositivo utilizando los Keystores locales o externos.
   - **Prueba de Conectividad (Ping CI/CD)**: Herramienta de diagnóstico de red para verificar la disponibilidad y latencia de red (RTT) de tus servidores de integración continua, Jenkins, GitHub Actions o consolas de Google Play.

---

## 🛠️ Stack Tecnológico

- **Lenguaje**: Kotlin (100%)
- **Interfaz**: Jetpack Compose con arquitectura reactiva de componentes.
- **Base de Datos**: Room Database (SQLite local) con migraciones destructivas controladas para desarrollo ágil.
- **Concurrencia**: Kotlin Coroutines & Flow (gestión optimizada de hilos de CPU).
- **Diseño**: Material Design 3 (Soporte nativo para modo claro/oscuro dinámico y paletas suaves de alta legibilidad).
- **Persistencia de Preferencias**: `SharedPreferences` para la gestión segura del PIN.

---

## 📦 Configuración y Ejecución

### Requisitos previos
- Android Studio Ladybug o superior.
- JDK 17 o superior.
- Dispositivo físico o emulador con Android SDK 24 (Android 7.0) o superior.

### Instalación rápida
1. Clona este repositorio o abre la carpeta en Android Studio.
2. Sincroniza Gradle para resolver las dependencias configuradas en el catálogo de versiones (`gradle/libs.versions.toml`).
3. Presiona el botón **Run** (`Shift + F10`) para compilar e instalar en tu dispositivo de prueba.

---

## 🔒 Estructura de Seguridad

Para garantizar que tus credenciales de producción no se expongan en texto plano:
- El PIN se procesa usando `SHA-256` con una sal generada de forma aleatoria basada en un prefijo temporal persistido en el dispositivo.
- Los archivos generados se almacenan inicialmente en el almacenamiento interno privado de la aplicación (`context.filesDir`) y solo se exponen a la carpeta de descargas del sistema cuando el usuario presiona explícitamente el botón "Guardar en Descargas" o utiliza el menú de "Compartir nativo".
