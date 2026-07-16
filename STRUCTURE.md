# Estructura del Proyecto - Guardián de Firmas (Modularizado)

Este documento detalla el árbol de directorios y la arquitectura del código fuente implementado en la aplicación para asegurar que futuros agentes y desarrolladores entiendan el flujo de datos. La aplicación ha sido completamente modularizada para adherirse a los principios de **Responsabilidad Única (SRP)** e **Inversión de Dependencias (DIP)**.

---

## 🌳 Árbol de Archivos del Código Fuente

```text
/app/src/main/
│
├── AndroidManifest.xml             # Manifiesto de Android (Permisos de almacenamiento, entrada, etc.)
│
└── java/com/example/
    │
    ├── MainActivity.kt             # Punto de entrada de la actividad (Gatillador de seguridad por PIN)
    │
    ├── data/                       # Capa de datos (Persistencia Room local)
    │   ├── AppDatabase.kt          # Declaración e inicialización de la Base de Datos Room
    │   ├── KeyConfig.kt            # Entidad que mapea las llaves criptográficas y sus propiedades
    │   └── KeyConfigDao.kt         # Queries de acceso a datos de Room (CRUD de registros de firma)
    │
    ├── security/                   # Capa de seguridad y criptografía modular
    │   ├── BouncyCastleManager.kt  # Registro y gestión del proveedor criptográfico Bouncy Castle
    │   ├── KeyPairService.kt       # Generación de pares de llaves públicas/privadas (RSA y EC)
    │   ├── CertificateService.kt   # Creación y firma de certificados autofirmados X.509 v3
    │   ├── KeystoreStorageService.kt # Guardado físico seguro en disco local en formato PKCS12 (.keystore/.jks)
    │   └── KeystoreGenerator.kt    # Fachada unificada que orquesta el flujo de generación criptográfica
    │
    ├── ui/                         # Capa de Presentación (Jetpack Compose, MVVM)
    │   ├── KeystoreApp.kt          # Grafo central de navegación (NavHost) y rutas de la app
    │   ├── HomeScreen.kt           # Historial de llaves creadas con buscador, categorías y opciones
    │   ├── GeneratorScreen.kt      # Formulario reactivo para generar llaves seguras y plantilla Debug
    │   ├── DetailScreen.kt         # Visualizador detallado de claves, contraseñas ocultables y exportación
    │   ├── tools/                  # Suite avanzada de firma de APKs, conversión de formatos y utilidades
    │   │   ├── ToolsScreen.kt      # Pantalla principal (Contenedor y Tabs) para herramientas de firma
    │   │   ├── InspectorTab.kt     # Inspector e importador de Keystores existentes
    │   │   ├── ConverterTab.kt     # Conversor de formatos de firma (JKS / PKCS12)
    │   │   ├── ApkVerifierTab.kt   # Extractor y verificador de firmas digitales de APKs
    │   │   ├── ApkSignerTab.kt     # Firmador de APKs nativo usando firmas cargadas
    │   │   ├── PingTab.kt          # Prueba de latencia RTT de red para servidores de CI/CD
    │   │   └── ToolsShared.kt      # Utilidades compartidas y componentes visuales para las herramientas
    │   ├── PinScreen.kt            # Componente de autenticación obligatoria (Creación y verificación de PIN)
    │   ├── KeystoreState.kt        # Interfaz de estados de UI reactivos (Idle, Generating, Success, Error)
    │   ├── KeystoreViewModel.kt    # ViewModel que expone flujos de datos y gestiona procesos asíncronos
    │   ├── KeystoreViewModelFactory.kt # Fábrica para inyección manual del repositorio en el ViewModel
    │   │
    │   └── theme/                  # Paleta de estilos y colores cómodos (Material 3)
    │       ├── Color.kt            # Definición de tonos suaves (pastel de bajo cansancio ocular)
    │       ├── Theme.kt            # Enlace de colores dinámicos y tipografías
    │       └── Type.kt             # Tipografía moderna corporativa
    │
    └── util/                       # Clases de apoyo técnico y utilidades del sistema
        ├── FileHelper.kt           # Fachada unificada para compartir y exportar archivos
        ├── FileSharer.kt           # Lógica especializada de compartir a través de FileProvider de Android
        ├── FileExporter.kt         # Lógica especializada para exportar a la carpeta pública de descargas
        ├── PinManager.kt           # Algoritmo de Hashing SHA-256 con sal dinámica para almacenar el PIN
```

---

## 🏗️ Descripción de los Módulos Principales

### 1. Gobernanza de Inicio (PIN) — `MainActivity.kt`
Al iniciar la aplicación, `MainActivity` intercepta el flujo cargando primero el estado desbloqueado en `false`. Presenta inmediatamente el `PinScreen` como un guardián visual. Si la contraseña (PIN) ingresada coincide con el hash del almacenamiento seguro, el estado cambia a `true` y se renderiza el `KeystoreApp` (el cual contiene el `NavHost` con las pantallas de la aplicación).

### 2. Capa de Seguridad Modular — `security/`
Se ha separado la generación del Keystore en servicios con responsabilidades aisladas:
- **`BouncyCastleManager`**: Se encarga del registro del proveedor de seguridad "BC" de forma segura.
- **`KeyPairService`**: Gestiona de forma aislada la generación matemática del par de claves.
- **`CertificateService`**: Construye el nombre distinguido (Distinguished Name) y genera el certificado X.509 autofirmado.
- **`KeystoreStorageService`**: Persiste físicamente la llave en formato de almacenamiento seguro moderno PKCS12 compatible con los requisitos de Google Play Store.
- **`KeystoreGenerator`**: Actúa como un punto de entrada simplificado (Fachada) que encapsula esta complejidad.

### 3. Presentación e Interfaz Desacoplada — `ui/`
La navegación ha sido extraída de la actividad principal hacia `KeystoreApp.kt` para simplificar la vista de entrada. Asimismo, los estados de renderizado se declaran modularmente en `KeystoreState.kt` y la fábrica del ViewModel se aloja en `KeystoreViewModelFactory.kt` para una arquitectura más limpia y escalable.
- **`DetailScreen.kt`**: Además de mostrar detalladamente todas las contraseñas, alias y especificaciones criptográficas generadas, incorpora la **Guía Interactiva de Integración & Base64** que permite convertir la llave binaria directamente a una cadena Base64 útil para automatizaciones CI/CD, y provee plantillas de código listas para copiar (para flujos de GitHub Actions y configuraciones locales de Gradle mediante `keystore.properties`).
- **`tools/`**: Suite avanzada de utilidades criptográficas y de firma completamente modularizada:
  - **`ToolsScreen.kt`**: Orquestador principal que renderiza el `ScrollableTabRow` y alterna entre las distintas herramientas.
  - **`InspectorTab.kt`**: Permite abrir Keystores existentes (.jks o .keystore) para extraer sus certificados, algoritmos, huellas SHA-256/SHA-1 y guardarlos en la base de datos de la app.
  - **`ConverterTab.kt`**: Conversor bidireccional entre formatos JKS y PKCS12, permitiendo compartir el archivo resultante.
  - **`ApkVerifierTab.kt`**: Extrae, procesa y valida las firmas digitales de cualquier APK de Android.
  - **`ApkSignerTab.kt`**: Firmador nativo de APKs que utiliza las llaves y credenciales almacenadas para firmar APKs mediante el formato JAR v1.
  - **`PingTab.kt`**: Herramienta de diagnóstico de red para comprobar la latencia RTT y disponibilidad de servidores de CI/CD (ej. GitHub).
  - **`ToolsShared.kt`**: Componentes visuales y utilidades de archivos comunes del módulo.

### 4. Exportación e Interoperabilidad de Archivos — `util/`
Las utilidades de archivos se dividen para evitar el acoplamiento de responsabilidades de sistema operativo:
- **`FileSharer`**: Orquesta el flujo de compartir mediante el sistema de Intents nativo de Android y FileProvider para conceder de forma segura permisos de lectura temporales.
- **`FileExporter`**: Realiza el copiado físico de ficheros al almacenamiento público `/Downloads/Keystores/` adaptándose de forma transparente al sistema de almacenamiento Scoped Storage (API 29+) o métodos legacy (API < 29).
- **`FileHelper.kt`**: Centraliza las operaciones de exportación, compartición y ahora la **conversión a Base64 sin saltos de línea (NO_WRAP)** de la llave de firma.
