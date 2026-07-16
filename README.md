# Guardián de Firmas (Keystore & JKS Generator + Web Companion)

Una suite de herramientas de firma y gestión criptográfica que une una aplicación móvil Android nativa (Kotlin + Jetpack Compose) con un **Companion Web** ligero de ejecución 100% local, permitiendo a los desarrolladores generar, organizar, proteger y exportar llaves criptográficas de firma (`.keystore`, `.jks` y certificados `.pem`).

La suite está diseñada pensando en la comodidad del desarrollador, ofreciendo una experiencia limpia que evita la fatiga visual al omitir estéticas cyberpunk/futuristas en favor de tonos pastel suaves y contrastes refinados basados en **Material Design 3**.

---

## 🚀 Características Clave

1. **Expansión Web Companion**:
   - Una aplicación complementaria web localizada en `/web/index.html` que permite generar pares de llaves RSA, certificados auto-firmados X.509 PEM y analizar certificados de manera inmediata y 100% privada sin subir ningún dato a servidores externos.
2. **Generación Segura Multihilo**:
   - Generación inteligente delegada a corrutinas que diferencian entre núcleos de potencia y eficiencia del procesador móvil, evitando congelamientos de la interfaz de usuario (ANRs) y cierres inesperados.
3. **Formato Flexible**:
   - Soporte para exportación en formatos estándar de Android (`.keystore` y `.jks`).
4. **Organización por Categorías**:
   - Clasificación de llaves en **DEBUG**, **PRODUCCIÓN** y **PRUEBA** con visualización diferenciada por etiquetas de color.
5. **Plantilla de Atajo Debug**:
   - Botón de carga instantánea que rellena los campos requeridos para compilar con la firma estándar de depuración de Android (`androiddebugkey`, contraseña `android`, validez de 30 años).
6. **Garantía y Seguridad mediante PIN y Biometría**:
   - Acceso completamente bloqueado tras una pantalla de protección por PIN o autenticación biométrica (huella dactilar/rostro) opcional y de fácil configuración en los Ajustes.
   - Las contraseñas y el PIN se encriptan utilizando algoritmos de hash unidireccional con sal dinámica (`SHA-256` y sales criptográficas locales) almacenados de forma segura en `SharedPreferences`.
   - Ajustes dedicados en la pantalla de inicio para habilitar/deshabilitar la biometría o restablecer y reconfigurar el PIN de acceso de forma rápida.
7. **Descarga al Almacenamiento del Sistema**:
   - Integración nativa con `MediaStore` para guardar llaves de forma segura en la carpeta pública `/Download/Keystores/` del gestor de archivos, compatible con Android 10+ (Scoped Storage) y versiones heredadas.
8. **Suite de Herramientas de Firma & Red**:
   - **Inspector de Keystores**: Permite cargar y analizar archivos Keystore existentes para validar contraseñas, ver alias, algoritmos de firma, huellas digitales y fechas de validez.
   - **Convertidor de Formatos**: Conversión ágil e inmediata entre formatos estándar (JKS, PKCS12 y JCEKS).
   - **Verificador de Firmas de APK**: Analiza cualquier archivo APK para inspeccionar y validar su certificado de firma y huellas criptográficas.
   - **Firmador de APKs Multiesquema**: Permite firmar archivos APK eligiendo y combinando múltiples esquemas de firma de Android (**v1, v2, v3 y v4**) de manera programática, con generación automática de firmas de streaming externas (`.idsig`) usando la biblioteca oficial `apksig`.
   - **Asistente Play Store App Signing**: Guía interactiva paso a paso para el registro en Google Play, permitiendo exportar el certificado de subida en formato estándar `.pem` y generando comandos personalizados para la encriptación mediante la herramienta PEPK.
   - **Prueba de Conectividad (Ping CI/CD)**: Herramienta de diagnóstico de red para verificar la disponibilidad y latencia de red (RTT) de tus servidores de integración continua, Jenkins, GitHub Actions o consolas de Google Play.

---

## 🛠️ Stack Tecnológico

### Aplicación Android
- **Lenguaje**: Kotlin (100%)
- **Interfaz**: Jetpack Compose con arquitectura reactiva de componentes.
- **Base de Datos**: Room Database (SQLite local) con migraciones destructivas controladas para desarrollo ágil.
- **Concurrencia**: Kotlin Coroutines & Flow (gestión optimizada de hilos de CPU).
- **Diseño**: Material Design 3 (Soporte nativo para modo claro/oscuro dinámico y paletas suaves de alta legibilidad).
- **Persistencia de Preferencias**: `SharedPreferences` para la gestión segura del PIN.

### Web Companion
- **Estructura**: HTML5, Vanilla JavaScript (ES6+), y CSS3 responsivo usando Tailwind CSS.
- **Criptografía local**: Integración de la biblioteca de alto rendimiento `node-forge` (vía CDN seguro) para generación RSA/X.509 y cálculo de hash local (SHA-1, SHA-256).
- **Alojamiento**: Estático, listo para desplegar en GitHub Pages, Vercel o localmente en el navegador abriendo el archivo `index.html`.

---

## 📦 Configuración y Ejecución

### Aplicación Android
- **Requisitos previos**: Android Studio Ladybug o superior, JDK 17 o superior.
- **Instalación**: Clona el repositorio, sincroniza Gradle y presiona **Run** (`Shift + F10`).

### Web Companion
- No requiere compilación. Simplemente abre `/web/index.html` en cualquier navegador web moderno o levanta un servidor local liviano:
  ```bash
  # Python 3
  python3 -m http.server 8000
  ```
  Y accede a `http://localhost:8000/web`.

---

## 🔒 Estructura de Seguridad

Para garantizar que tus credenciales de producción no se expongan en texto plano:
- El PIN se procesa usando `SHA-256` con una sal generada de forma aleatoria basada en un prefijo temporal persistido en el dispositivo.
- Los archivos generados se almacenan inicialmente en el almacenamiento interno privado de la aplicación (`context.filesDir`) y solo se exponen a la carpeta de descargas del sistema cuando el usuario presiona explícitamente el botón "Guardar en Descargas" o utiliza el menú de "Compartir nativo".
