package com.example.kaishelvesapp.data.model

enum class DeviceBookFormat {
    Pdf,
    Epub,
    Txt,
    Fb2,
    Unsupported
}

enum class DeviceReaderTheme {
    Paper,
    Dark,
    Sepia
}

enum class DeviceReaderAnnotationType {
    Bookmark,
    Highlight,
    Note
}

data class DeviceLibraryBookRecord(
    val id: String = "",
    val uri: String = "",
    val name: String = "",
    val location: String = "",
    val mimeType: String = "",
    val format: DeviceBookFormat = DeviceBookFormat.Unsupported,
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val sizeBytes: Long? = null,
    val modifiedAtMillis: Long? = null,
    val importedAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class DeviceReaderProgress(
    val bookId: String = "",
    val currentPage: Int = 0,
    val pageCount: Int = 1,
    val percent: Int = 0,
    val engine: DeviceBookFormat = DeviceBookFormat.Unsupported,
    val updatedAtMillis: Long = 0L
)

data class DeviceReaderSettings(
    val bookId: String = "",
    val theme: DeviceReaderTheme = DeviceReaderTheme.Paper,
    val textSizePercent: Int = 100,
    val reflowEnabled: Boolean = true,
    val updatedAtMillis: Long = 0L
)

data class DeviceReaderAnnotation(
    val id: String = "",
    val bookId: String = "",
    val type: DeviceReaderAnnotationType = DeviceReaderAnnotationType.Bookmark,
    val page: Int = 0,
    val pageCount: Int = 1,
    val selectedText: String = "",
    val note: String = "",
    val color: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
