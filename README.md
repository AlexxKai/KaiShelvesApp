# KaiShelvesApp
Aplicacion para gestion de listas de libros.

## Google Books API

### Reestructuración `DeviceLibraryScreen.kt`

Ahora queda así:
- `DeviceLibraryScreen.kt`: pantalla principal y top bar.
- `DeviceLibraryCoversAndImport.kt`: portadas, importación y metadatos locales.
- `DeviceLibrarySearchFilter.kt`: paneles de búsqueda/filtros/layout.
- `DeviceLibraryReader.kt`: lector interno.
- `DeviceLibraryContent.kt`: lista, grid, carrusel, tarjetas y previews.
- `DeviceLibraryUtils.kt`: helpers de formatos, PDF/EPUB, progreso y modelos.