# Kai Shelves

![Version](https://img.shields.io/badge/version-3.0.0-blue?style=flat-square)
![License](https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=flat-square)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.03.00-4285F4?style=flat-square)
![Material 3](https://img.shields.io/badge/Material%203-Compose-6750A4?style=flat-square)
![Android](https://img.shields.io/badge/Android-minSdk%2024%20%7C%20targetSdk%2036-3DDC84?style=flat-square)

<p align="center">
  <img src="app/src/main/res/drawable/logo_kaishelves.png" alt="Logo de Kai Shelves" width="180" />
</p>

**Kai Shelves** es una aplicación Android de gestión lectora desarrollada en **Kotlin** con **Jetpack Compose**. Permite descubrir libros, organizar lecturas, gestionar listas personales, consultar estadísticas, usar biblioteca local del dispositivo y conectar con otros lectores mediante funcionalidades sociales.

El proyecto está orientado a un **Trabajo de Fin de Grado de Desarrollo de Aplicaciones Multiplataforma (DAM)** y combina una arquitectura MVVM con Firebase, Google Books API, almacenamiento local para usuarios invitados y una interfaz Compose con identidad visual propia.

## Tabla de contenidos

- [Capturas](#capturas)
- [Características principales](#características-principales)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura del proyecto](#arquitectura-del-proyecto)
- [Estructura de carpetas](#estructura-de-carpetas)
- [Requisitos previos](#requisitos-previos)
- [Instalación](#instalación)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
- [Cómo generar APK / build](#cómo-generar-apk--build)
- [Testing](#testing)
- [Variables de entorno / configuración local](#variables-de-entorno--configuración-local)
- [Roadmap](#roadmap)
- [Contribución](#contribución)
- [Licencia](#licencia)
- [Autor](#autor)

## Capturas

Las capturas todavía no están versionadas en el repositorio. Se recomienda añadir imágenes reales de la app en las siguientes rutas:

| Pantalla | Placeholder |
| --- | --- |
| Inicio / actividad | `docs/images/home.png` |
| Catálogo / descubrir | `docs/images/catalog.png` |
| Detalle de libro | `docs/images/detail.png` |
| Mis libros / listas | `docs/images/lists.png` |
| Biblioteca local | `docs/images/device-library.png` |
| Perfil y privacidad | `docs/images/profile.png` |

## Características principales

- Autenticación con Firebase mediante email y contraseña.
- Inicio de sesión con Google usando Credential Manager y Google ID.
- Verificación de correo electrónico.
- Recuperación de contraseña.
- Modo invitado con datos locales en el dispositivo.
- Migración y fusión de biblioteca local de invitado con cuenta registrada.
- Catálogo de libros con Google Books API.
- Búsqueda por título, autor, editorial o ISBN.
- Escaneo de ISBN/códigos mediante ZXing.
- Filtros por género y ordenación de resultados.
- Ficha de libro con portada, metadatos, estado de lectura, valoración y reseña.
- Marcado de libros como leídos.
- Listas personales y estanterías predefinidas.
- Etiquetas personalizadas para organizar libros.
- Estadísticas de lectura.
- Recomendaciones personalizadas en la sección “Para ti”.
- Biblioteca local del dispositivo mediante Storage Access Framework, con disposición y filtros de tipo de archivo persistentes.
- Lector interno con motores para PDF, EPUB, TXT y FB2 textual; PDF mantiene renderizado nativo y los formatos de texto usan paginación reflow.
- Registro local persistente de libros importados, progreso de lectura y modelo común para marcadores, subrayados y notas.
- Lector reflow para EPUB, TXT y FB2 con desplazamiento continuo, selección táctil por pulsación prolongada, rangos reales persistentes, marcadores, subrayados y notas editables.
- Vista PDF visual con zoom, desplazamiento manual y lectura continua en modo vertical, manteniendo marcadores y notas sin remaquetado del contenido.
- Conversor básico a PDF para TXT, EPUB y FB2.
- Perfil de usuario con edición de datos, foto y métodos de inicio de sesión.
- Ajustes de privacidad y visibilidad social.
- Amigos, sugerencias, solicitudes, perfiles públicos/privados y listas compartidas.
- Feed social con actividad, likes y comentarios.
- Centro de notificaciones y notificaciones locales.
- Ayuda integrada con FAQ, contexto de pantalla y chat asistido por Groq si hay API key configurada.
- Internacionalización preparada con recursos en `values` y `values-es`.

## Stack tecnológico

| Área | Tecnología |
| --- | --- |
| Lenguaje | Kotlin 2.2.0 |
| UI | Jetpack Compose, Material 3 |
| Navegación | Navigation Compose |
| Arquitectura | MVVM, Repository Pattern, StateFlow |
| Asincronía | Kotlin Coroutines |
| Backend / datos remotos | Firebase Authentication, Firebase Firestore |
| APIs externas | Google Books API, Groq API |
| Red | Retrofit 3, Gson Converter, OkHttp, Logging Interceptor |
| Imágenes | Coil 3 |
| Login Google | AndroidX Credentials, Google ID |
| Biblioteca local | Storage Access Framework, `DocumentsContract` |
| Lectura local | `PdfDocument`, `PdfRenderer`, EPUB/TXT nativos y adaptador textual FB2 |
| ML / idioma | ML Kit Language ID, ML Kit Translate |
| Escaneo | ZXing Android Embedded |
| Testing declarado | JUnit, AndroidX Test, Espresso, Compose UI Test |

## Arquitectura del proyecto

La app sigue una organización basada en **MVVM**:

- **UI Compose**: pantallas y componentes reutilizables bajo `ui/screen` y `ui/components`.
- **ViewModels**: gestionan estado de pantalla, eventos de usuario y llamadas a repositorios.
- **Repositories**: encapsulan Firebase, APIs externas, almacenamiento local, biblioteca del dispositivo y lógica de transformación.
- **Models / DTOs / Mappers**: definen los modelos de dominio y las estructuras remotas de Google Books y Groq.
- **Navigation Compose**: centraliza rutas, flujo de autenticación, restricciones de usuario invitado y navegación principal en `AppNavigation.kt`.

No se detecta un framework de inyección de dependencias como Hilt. Los ViewModels se obtienen con `viewModel()` y los repositorios se instancian de forma directa o con dependencias por defecto.

### Flujo general

```text
Composables
   ↓ eventos / estado
ViewModels
   ↓ operaciones de dominio
Repositories
   ↓
Firebase / Google Books / Groq / almacenamiento local / archivos del dispositivo
```

### Capas relevantes

- `AuthRepository`: autenticación, perfiles, proveedores de login, usuarios invitados, migración de datos y privacidad.
- `BookRepository`: catálogo, búsquedas, ISBN, Google Books, lecturas, reseñas y valoración.
- `UserListsRepository`: listas, estanterías, etiquetas y organización de libros.
- `FriendsRepository`: amigos, solicitudes, privacidad social, actividad, likes, comentarios, bloqueos y reportes.
- `ForYouRepository`: recomendaciones basadas en libros guardados.
- `HelpChatRepository`: ayuda contextual con fallback local y soporte opcional de Groq.
- `DeviceFileRepository`: lectura de carpetas del dispositivo y detección de archivos compatibles.
- `DeviceLibraryRepository`: registro local de libros del dispositivo, progreso, ajustes de lectura y anotaciones.

## Estructura de carpetas

```text
KaiShelvesApp/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/example/kaishelvesapp/
│           │   ├── MainActivity.kt
│           │   ├── data/
│           │   │   ├── help/
│           │   │   ├── local/
│           │   │   ├── localization/
│           │   │   ├── model/
│           │   │   ├── notifications/
│           │   │   ├── remote/
│           │   │   │   ├── googlebooks/
│           │   │   │   └── groq/
│           │   │   ├── repository/
│           │   │   └── security/
│           │   └── ui/
│           │       ├── components/
│           │       ├── language/
│           │       ├── navigation/
│           │       ├── screen/
│           │       ├── theme/
│           │       ├── util/
│           │       └── viewmodel/
│           └── res/
│               ├── drawable/
│               ├── drawable-nodpi/
│               ├── values/
│               ├── values-es/
│               └── xml/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
└── README.md
```

## Requisitos previos

- Android Studio compatible con Android Gradle Plugin `8.13.0`.
- JDK 11.
- Android SDK con:
  - `compileSdk 36`
  - `minSdk 24`
  - `targetSdk 36`
- Cuenta/proyecto de Firebase configurado para Authentication y Firestore.
- Archivo `app/google-services.json` correspondiente al proyecto Firebase.
- Conexión a internet para sincronizar Gradle y consumir APIs remotas.

## Instalación

1. Clona el repositorio:

```bash
git clone <URL_DEL_REPOSITORIO>
cd KaiShelvesApp
```

2. Abre el proyecto en Android Studio.

3. Sincroniza Gradle desde Android Studio.

4. Comprueba que existe el archivo de configuración de Firebase:

```text
app/google-services.json
```

5. Configura las claves locales en `local.properties` si vas a usar Google Books con API key o el chat con Groq.

## Cómo ejecutar el proyecto

Desde Android Studio:

1. Selecciona el módulo `app`.
2. Elige un emulador o dispositivo físico con Android 7.0 o superior.
3. Ejecuta la configuración de lanzamiento de la app.

Desde terminal en Windows:

```powershell
.\gradlew.bat installDebug
```

Para compilar sin instalar:

```powershell
.\gradlew.bat compileDebugKotlin
```

## Cómo generar APK / build

Generar APK debug:

```powershell
.\gradlew.bat assembleDebug
```

El APK se genera en:

```text
app/build/outputs/apk/debug/
```

Generar build release:

```powershell
.\gradlew.bat assembleRelease
```

El proyecto no versiona archivos de firma (`*.jks`, `*.keystore`) y `.gitignore` excluye artefactos APK/AAB. Para publicar una build release firmada es necesario configurar la firma localmente.

## Testing

El proyecto declara dependencias de testing para JUnit, AndroidX Test, Espresso y Compose UI Test. No se han detectado archivos de test versionados en `app/src/test` o `app/src/androidTest`.

Comandos útiles:

```powershell
.\gradlew.bat testDebugUnitTest
```

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Verificación mínima recomendada antes de entregar cambios:

```powershell
.\gradlew.bat compileDebugKotlin
```

## Variables de entorno / configuración local

La app lee valores desde `local.properties` en tiempo de build y los expone mediante `BuildConfig`.

Ejemplo seguro:

```properties
googleBooksApiKey=TU_API_KEY
groqApiKey=TU_API_KEY
groqModel=llama-3.1-8b-instant
```

| Propiedad | Uso |
| --- | --- |
| `googleBooksApiKey` | Clave opcional para Google Books API. Si falta o falla por cuota/permisos, el cliente puede usar la API pública sin clave en determinados casos. |
| `groqApiKey` | Clave opcional para activar el chat asistido por Groq. Si no existe, la ayuda usa un fallback local. |
| `groqModel` | Modelo usado por Groq. Valor por defecto: `llama-3.1-8b-instant`. |

También existe configuración para Google Sign-In en:

```text
app/src/main/res/values/google_auth.xml
```

No incluyas claves privadas, tokens ni credenciales personales en el repositorio.

## Roadmap

Mejoras futuras inferidas por pantallas, textos y rutas existentes:

- Completar la sección de grupos de lectura.
- Completar los desafíos de lectura.
- Ampliar notificaciones y mensajes dentro del centro de notificaciones.
- Añadir capturas reales en `docs/images`.
- Incorporar pruebas unitarias y pruebas de UI para flujos críticos.
- Revisar textos hardcodeados restantes y moverlos a recursos localizados.
- Preparar firma release y documentación de publicación si se decide distribuir la app.

## Contribución

1. Crea una rama descriptiva:

```bash
git checkout -b feature/nombre-del-cambio
```

2. Mantén los cambios acotados y coherentes con la arquitectura existente.

3. Evita subir secretos, APKs, AABs o archivos de firma.

4. Ejecuta al menos:

```powershell
.\gradlew.bat compileDebugKotlin
```

5. Abre una pull request con:
   - resumen del cambio,
   - pantallas o flujos afectados,
   - pruebas realizadas,
   - riesgos conocidos si existen.

## Licencia

Este proyecto está distribuido bajo licencia **Apache License 2.0**. Consulta el archivo [LICENSE](LICENSE) para más información.

## Autor

Proyecto académico desarrollado como **Trabajo de Fin de Grado de Desarrollo de Aplicaciones Multiplataforma (DAM)**.

Autor: **AlexxKai**.

