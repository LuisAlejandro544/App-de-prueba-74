# Manual Contextual para Inteligencias Artificiales (AI_CONTEXT.md)

Este documento sirve como un manual técnico de entrada y guía de estilo para cualquier Agente de Inteligencia Artificial o programador que venga a expandir, depurar o refactorizar este proyecto. 

---

## 🎯 Filosofía del Proyecto

1. **Intención del Usuario como Límite**: Construye estrictamente lo que se solicita. Evita la sobre-ingeniería o añadir dependencias unificadas no deseadas.
2. **Estética Limpia y Relajante**:
   - **REGLA ESTRICTA**: No utilices colores estridentes como paletas "cyberpunk", neones brillantes, ni verdes/morados futuristas. El usuario solicitó específicamente una interfaz cómoda de tonos pastel suaves que prevenga la fatiga ocular.
   - Utiliza exclusivamente las paletas configuradas en `Color.kt` y `Theme.kt` siguiendo la nomenclatura de Material Design 3.
3. **Persistencia Confiable**:
   - Cualquier adición de configuración o metadatos de llave debe registrarse en la entidad `KeyConfig` y controlarse a través del DAO de Room Database.

---

## 🔒 Patrones de Seguridad y Gobernanza

### 1. Gestión del PIN Obligatorio
El PIN de acceso nunca debe persistirse en texto plano. Se implementa en `PinManager.kt` mediante el hash `SHA-256`.
- **Flujo de verificación**:
  1. Al abrir la app, `MainActivity` comprueba si el usuario tiene un PIN establecido.
  2. Si no tiene, se le obliga a crear uno de 4 dígitos.
  3. Si tiene, se le solicita ingresarlo. Se aplica hash al PIN ingresado con la sal almacenada en `SharedPreferences` y se compara con el hash guardado.
  4. Solo si coinciden se desbloquea el estado `isUnlocked = true`, permitiendo renderizar el `NavHost`.

### 2. Exportación e Interoperabilidad de Archivos
- **Almacenamiento Privado**: Las llaves generadas residen inicialmente en `context.filesDir` (directorio privado inaccesible para otras apps).
- **Exportación con MediaStore**: Al descargar, se usa la API de Scoped Storage (`MediaStore.Downloads`) para escribir en el directorio público `/Download/Keystores/` en Android Q+ de forma segura y sin requerir permisos de escritura peligrosos. Mantén este flujo en `FileHelper.kt` para evitar que la aplicación falle en dispositivos modernos.

---

## ⚡ Concurrencia y Rendimiento

La generación de un Keystore es una tarea criptográfica intensiva (cálculo de claves públicas/privadas RSA/EC y codificación de certificados).
- **Prohibido ejecutar en el hilo principal (Main Thread)**: Cualquier proceso criptográfico debe delegarse a `Dispatchers.Default` o `Dispatchers.IO` a través del `KeystoreViewModel`.
- **Manejo de Estados**: Usa estados de interfaz de tipo `StateFlow` reactivos para inhabilitar botones de generación mientras haya un proceso activo, previniendo reinicios accidentales o bloqueos de la app.

---

## 🛠️ Convenciones de Desarrollo

- **Kotlin DSL**: Configuración de Gradle con archivos `.kts`.
- **Componentes Material 3**: Usa `Scaffold`, `Card`, `OutlinedTextField` y botones con bordes redondeados estándar de Material 3.
- **Asignación de TestTags**: Todo elemento interactivo principal (botones de generación, diálogos de confirmación, campos de texto clave) **debe** incluir un modificador `.testTag("nombre_del_tag")` utilizando nomenclatura `snake_case` para facilitar las pruebas automatizadas.
- **Idiomas y Recursos**: Mantén la consistencia lingüística de la app (español por defecto para mensajes y diálogos públicos).
- **Edge-to-Edge**: Usa `enableEdgeToEdge()` de manera nativa en el `onCreate` de `MainActivity` y respeta los `WindowInsets` de la barra de estado y navegación en tus pantallas mediante contenedores tipo `Scaffold`.

---

## 🛠️ Arquitectura de Herramientas Avanzadas (Tools Screen)

La suite de herramientas de firma y conectividad (`ToolsScreen.kt`) implementa funcionalidades criptográficas avanzadas que dependen directamente de la inicialización limpia de Bouncy Castle:
1. **Bouncy Castle como Proveedor Global**:
   - Funcionalidades como la conversión de formatos de Keystore (`JKS <-> PKCS12`), firma de APKs (`ApkSignerHelper`), y verificación de APKs requieren del proveedor de seguridad "BC".
   - **Regla**: Siempre llama a `BouncyCastleManager.setupBouncyCastle()` antes de realizar operaciones criptográficas complejas para asegurar que esté correctamente registrado a nivel de máquina virtual.
2. **Firma Segura en Hilos de Trabajo**:
   - Al igual que la generación, las tareas de firmado de APKs y análisis de Keystores consumen recursos intensivos. Debes delegar estas operaciones a un alcance de corrutinas apropiado (`viewModelScope` con `Dispatchers.IO`) y notificar progreso mediante estados reactivos.
3. **Conectividad TCP no Bloqueante**:
   - El módulo `PingTab` realiza operaciones de red nativas por sockets TCP para medir el RTT. Al tratarse de operaciones de red, estas **deben ejecutarse estrictamente** en un contexto de corrutina de E/S (`Dispatchers.IO`), manejando excepciones de red amigablemente para evitar crasheos por hilos principales saturados o excepciones de red imprevistas.
