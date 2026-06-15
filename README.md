# Kai Shelves

![Version](https://img.shields.io/badge/version-3.0.0-blue?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin_2.2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Android%20Gradle%20Plugin](https://img.shields.io/badge/AGP-8.13.0-3DDC84?style=flat-square)
![Jetpack%20Compose](https://img.shields.io/badge/Compose%20BOM-2026.03.00-4285F4?style=flat-square)
![Android](https://img.shields.io/badge/minSdk%2024%20%7C%20targetSdk%2036-3DDC84?style=flat-square)
![License](https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)

<p align="center">
<img src="app\src\main\res\drawable\logo_kaishelves.png" alt="Logo de la aplicación" width="300" height="200">
</p>

**Kai Shelves** es una aplicación Android de gestión lectora desarrollada en **Kotlin** y **Jetpack Compose**. Permite descubrir libros, organizar lecturas, mantener una biblioteca local del dispositivo, leer archivos compatibles, consultar estadísticas, gestionar listas y participar en funciones sociales de lectura.

La app combina catálogo remoto, datos de usuario en Firebase, modo invitado con persistencia local, lector interno, conversor de archivos y una interfaz Compose con Material 3.

## Descripción general

Kai Shelves está orientada a centralizar la experiencia lectora en Android:

- **Gestión lectora**: libros leídos, estados de lectura, reseñas, puntuaciones, listas, etiquetas y estadísticas.
- **Descubrimiento de libros**: búsqueda por título, autor, editorial o ISBN usando Google Books, OpenLibrary e Inventaire según el caso.
- **Biblioteca local**: selección de carpetas mediante Storage Access Framework, filtros, ordenación, portadas y registro local de progreso.
- **Lector interno**: lectura de PDF, EPUB, TXT y FB2 según el formato y el contenido disponible.
- **Conversor PDF/EPUB**: conversiones locales para formatos sencillos y derivación manual a conversor externo cuando el formato no se puede transformar de forma fiable dentro de la app.
- **Social**: amistades, solicitudes, perfiles, actividad, comentarios, reacciones y privacidad.
- **Ayuda integrada**: FAQ, contexto de pantalla y chat asistido por Groq cuando se configura una clave local.

## Características principales

### Autenticación y cuentas

- Registro e inicio de sesión con Firebase Authentication mediante email y contraseña.
- Inicio de sesión con Google usando AndroidX Credentials y Google ID.
- Verificación de correo electrónico y recuperación de contraseña.
- Modo invitado con datos locales.
- Migración y fusión de datos locales de invitado al crear o usar una cuenta.
- Perfil de usuario con foto, nombre, email, proveedores de acceso y ajustes de privacidad.

### Catálogo y descubrimiento

- Consulta de libros mediante Google Books API.
- Respaldo de búsqueda con OpenLibrary.
- Consulta por ISBN con Inventaire cuando está disponible.
- Búsqueda por título, autor, editorial o ISBN.
- Búsqueda específica por ISBN desde texto introducido o escaneado.
- Escáner de códigos con CameraX y ML Kit Barcode Scanning.
- Historial local de escaneos ISBN.
- Caché local de la sección de descubrimiento.
- Localización básica de metadatos de libros para el idioma activo.

### Organización lectora

- Listas del usuario y estanterías del sistema.
- Etiquetas personalizadas para libros.
- Estados de lectura como pendiente, leyendo, leído, sin terminar, quiero leer o tengo.
- Sincronización de libros locales con la lista automática de posesión cuando procede.
- Puntuación y reseña de libros leídos.
- Importación de CSV de Goodreads mediante repositorio específico.

### Estadísticas y recomendaciones

- Estadísticas calculadas a partir de listas y lecturas.
- Caché local de estadísticas.
- Sección **Para ti** con recomendaciones basadas en libros guardados, géneros, autores y épocas detectadas.
- Opción de privacidad para activar o desactivar sugerencias personalizadas.

### Biblioteca local y lector interno

- Selección de carpeta local con Storage Access Framework.
- Exploración de archivos compatibles del dispositivo.
- Filtros por tipo, lectura y búsqueda local.
- Ordenación, dirección, diseño y preferencias persistentes.
- Portadas locales y portadas predeterminadas.
- Lector PDF con `PdfRenderer`, zoom, desplazamiento, modo vertical/horizontal, brillo, filtro de luz azul, marcadores y notas.
- Lector EPUB con WebView, recursos internos, capítulos, imágenes, portada/sinopsis cuando existen, selección de texto, subrayados, notas y marcadores.
- Lector TXT y FB2 textual en modo reflow.
- Persistencia local de progreso, página actual, ajustes de lectura y anotaciones.

### Conversor de archivos

El conversor permite crear archivos PDF o EPUB desde la app sin modificar la biblioteca local original.

Conversiones locales a **PDF**:

- TXT
- Markdown
- HTML/XHTML
- FB2
- EPUB
- PDF a PDF mediante copia
- JPG/JPEG, PNG y WEBP

Conversiones locales a **EPUB**:

- TXT
- Markdown
- HTML/XHTML
- FB2
- PDF a EPUB visual, renderizando las páginas como imágenes y añadiendo texto mínimo de compatibilidad para el lector interno
- EPUB a EPUB mediante copia

Formatos derivados a conversión online manual:

- DOC/DOCX
- ODT
- RTF
- MOBI/AZW/AZW3/PRC
- CBZ/CBR
- DJVU/DJV
- Imágenes cuando la salida elegida es EPUB
- Formatos desconocidos

La app no sube archivos automáticamente. Cuando se usa el fallback online, se abre una web externa mediante `Intent.ACTION_VIEW` y el usuario decide manualmente si sube el archivo.

### Funciones sociales y ayuda

- Lista de amigos y búsqueda de usuarios.
- Solicitudes de amistad.
- Perfiles públicos o privados según ajustes de privacidad.
- Actividad social con publicaciones derivadas de lectura y listas.
- Likes, comentarios y respuestas.
- Centro de notificaciones para solicitudes y actividad social disponible.
- Reportes y revisión administrativa desde las pantallas de administración.
- Ayuda integrada con base local de conocimiento, FAQ y chat Groq opcional.
<!-- - **Grupos** aparece como sección preparada con pantalla placeholder.
- **Desafíos de lectura** aparece como sección preparada con pantalla placeholder.
- Algunas áreas de notificaciones pueden mostrar estados vacíos cuando no hay actividad disponible. -->

## Stack tecnológico

| Área | Tecnología |
| --- | --- |
| Lenguaje | Kotlin 2.2.0 |
| UI | Jetpack Compose, Material 3 |
| Navegación | Navigation Compose |
| Arquitectura | MVVM, Repository Pattern, StateFlow |
| Asincronía | Kotlin Coroutines |
| Backend | Firebase Authentication, Cloud Firestore |
| Google Sign-In | AndroidX Credentials, Google ID |
| APIs externas | Google Books, OpenLibrary, Inventaire, Groq |
| Red | Retrofit, OkHttp, Gson Converter, Logging Interceptor |
| Imágenes | Coil 3 |
| Cámara y escaneo | CameraX, ML Kit Barcode Scanning |
| ML Kit | Language ID, Translate |
| Biblioteca local | Storage Access Framework, DocumentsContract |
| Lectura y conversión | PdfRenderer, PdfDocument, WebView, ZipInputStream, ZipOutputStream |
| Persistencia local | SharedPreferences y stores locales del proyecto |
| Testing | JUnit, AndroidX Test, Espresso, Compose UI Test |

## Arquitectura

El proyecto sigue una arquitectura basada en MVVM y separación por capas:

- **UI Compose**: pantallas y componentes reutilizables en `ui/screen` y `ui/components`.
- **ViewModels**: exponen estado reactivo, coordinan eventos de pantalla y llaman a repositorios.
- **Repositories**: encapsulan Firebase, APIs externas, almacenamiento local, biblioteca del dispositivo, estadísticas y ayuda.
- **Models, DTOs y mappers**: representan dominio local y respuestas remotas.
- **Navigation Compose**: centraliza rutas, restricciones de invitado, flujos de autenticación y navegación principal.

Flujo simplificado:

```text
Composables
  -> ViewModels
  -> Repositories
  -> Firebase / APIs externas / almacenamiento local / archivos del dispositivo
```

Repositorios relevantes:

- `AuthRepository`: autenticación, perfil, proveedores, modo invitado, migración y privacidad.
- `BookRepository`: catálogo, búsquedas, ISBN, Google Books, OpenLibrary, Inventaire, reseñas y lecturas.
- `UserListsRepository`: listas, estanterías del sistema, etiquetas y organización.
- `FriendsRepository`: amistades, actividad social, comentarios, reportes, privacidad y notificaciones.
- `ForYouRepository`: recomendaciones basadas en libros guardados.
- `DeviceFileRepository`: lectura de carpetas y archivos locales.
- `DeviceLibraryRepository`: biblioteca local, progreso, ajustes y anotaciones.
- `GoodreadsCsvImportRepository`: importación de CSV exportado desde Goodreads.
- `HelpChatRepository`: ayuda contextual con fallback local y Groq opcional.

## Estructura de carpetas

```text

|-- app/                                      
     └── src/
         |-- main/                             
         |   |-- java/com/example/kaishelvesapp/
         |   |   |-- MainActivity.kt           
         |   |   |-- data/                     # Capa de datos, modelos, repositorios y clientes externos
         |   |   |   |-- help/                 # Base local de ayuda y modelos del asistente
         |   |   |   |-- local/                # Stores locales para invitado, cachés y estadísticas
         |   |   |   |-- localization/         # Adaptación/localización de metadatos de libros
         |   |   |   |-- model/                # Modelos de dominio: usuarios, libros, listas y lector local
         |   |   |   |-- notifications/        # Gestión de notificaciones locales del dispositivo
         |   |   |   |-- remote/               # Integraciones HTTP con servicios externos
         |   |   |   |   |-- googlebooks/       # Cliente, DTOs y mappers de Google Books
         |   |   |   |   |-- groq/              # Cliente y DTOs del chat de ayuda con Groq
         |   |   |   |   |-- inventaire/        # Consulta y mapeo de datos de Inventaire
         |   |   |   |   `-- openlibrary/      # Consulta y mapeo de datos de OpenLibrary
         |   |   |   |-- repository/          # Repositorios de autenticación, catálogo, listas, social y biblioteca local
         |   |   |   |-- security/            # Utilidades de codificación y tratamiento seguro de imágenes de perfil
         |   |   |   `-- statistics/          # Cálculo de estadísticas lectoras
         |   |   `-- ui/                       # Capa de presentación basada en Jetpack Compose
         |   |       |-- components/           # Componentes reutilizables, chrome, barras, diálogos y tarjetas
         |   |       |-- language/             # Utilidades de idioma y contexto localizado
         |   |       |-- navigation/           # Rutas y grafo de navegación Compose
         |   |       |-- screen/               # Pantallas por área funcional de la app
         |   |       |   |-- catalog/            # Catálogo, resultados y escáner ISBN
         |   |       |   |-- detail/             # Detalle de libro
         |   |       |   |-- foryou/             # Recomendaciones personalizadas
         |   |       |   |-- friends/            # Amigos, perfiles sociales y notificaciones
         |   |       |   |-- help/               # Ayuda, FAQ y solicitudes de soporte
         |   |       |   |-- home/               # Inicio y actividad reciente
         |   |       |   |-- library/            # Biblioteca local, lector interno y conversor
         |   |       |   |-- lists/              # Listas, estanterías y etiquetas
         |   |       |   |-- login/              # Inicio de sesión y verificación de email
         |   |       |   |-- placeholder/        # Pantallas preparadas para secciones aún no completadas
         |   |       |   |-- profile/            # Perfil de usuario
         |   |       |   |-- readinglist/        # Lecturas registradas
         |   |       |   |-- register/           # Registro de cuenta
         |   |       |   |-- settings/           # Privacidad y pantallas administrativas
         |   |       |   `-- stats/             # Estadísticas de lectura
         |   |       |-- util/                 # Utilidades de UI y formato
         |   |       `-- viewmodel/            # ViewModels y estados de pantalla
         |   `-- res/                          # Recursos visuales y strings con idiomas
         `--  
```

## Requisitos previos

- Android Studio compatible con Android Gradle Plugin 8.13.0.
- JDK 11, de acuerdo con `sourceCompatibility`, `targetCompatibility` y `kotlinOptions.jvmTarget`.
- Android SDK con `compileSdk 36`.
- Dispositivo o emulador con Android 7.0 o superior (`minSdk 24`).
- Proyecto Firebase configurado para Authentication y Firestore.
- Archivo local `app/google-services.json`.
- Archivo local `local.properties` cuando se usen claves opcionales.

## Instalación y ejecución

1. Clona el repositorio:

   ```bash
   git clone https://github.com/AlexxKai/KaiShelvesApp.git
   cd KaiShelvesApp
   ```

2. Abre el proyecto en Android Studio.

3. Sincroniza Gradle.

4. Configura los archivos locales necesarios:

   ```text
   app/google-services.json
   local.properties
   ```

5. Ejecuta el módulo `app` en un emulador o dispositivo físico.

Para compilar desde terminal en Windows:

```powershell
.\gradlew.bat compileDebugKotlin
```

Para generar un APK debug:

```powershell
.\gradlew.bat assembleDebug
```

## Configuración local

La app lee claves opcionales desde `local.properties` y las expone mediante `BuildConfig`.

Ejemplo sin secretos reales:

```properties
googleBooksApiKey=TU_API_KEY
groqApiKey=TU_API_KEY
groqModel=llama-3.1-8b-instant
```

| Propiedad | Uso |
| --- | --- |
| `googleBooksApiKey` | Clave opcional para Google Books API. |
| `groqApiKey` | Clave opcional para activar el chat asistido por Groq. |
| `groqModel` | Modelo usado por Groq. Si no se define, el valor por defecto es `llama-3.1-8b-instant`. |

También existe configuración de Google Sign-In en recursos del proyecto. No deben versionarse claves privadas, tokens personales ni credenciales locales.

## Comandos útiles

```powershell
.\gradlew.bat compileDebugKotlin
```

```powershell
.\gradlew.bat testDebugUnitTest
```

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

```powershell
.\gradlew.bat assembleDebug
```

## Pruebas existentes

El proyecto incluye pruebas automatizadas en:

- `app/src/test/java/com/example/kaishelvesapp/data/statistics/ReadingStatsCalculatorTest.kt`
- `app/src/androidTest/java/com/example/kaishelvesapp/ui/CriticalComposeFlowsTest.kt`

`ReadingStatsCalculatorTest` cubre lógica pura de estadísticas lectoras. `CriticalComposeFlowsTest` cubre flujos críticos de interfaz Compose.

## Permisos declarados

El manifiesto declara permisos para:

- Internet.
- Estado de red.
- Cámara.
- Notificaciones en Android compatible.

La cámara está declarada como característica no obligatoria, por lo que la app puede instalarse en dispositivos sin cámara.

## Licencia

Kai Shelves se distribuye bajo **Apache License 2.0**. Consulta el archivo [LICENSE](LICENSE) para más información.

## Autor

**Alex Urueña**  
Proyecto TFG DAM  
Kai Shelves

Repositorio: [https://github.com/AlexxKai/KaiShelvesApp.git](https://github.com/AlexxKai/KaiShelvesApp.git)
