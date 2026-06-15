package com.example.kaishelvesapp.data.repository

import android.content.Context
import com.example.kaishelvesapp.data.model.DeviceBookFormat
import com.example.kaishelvesapp.data.model.DeviceLibraryBookRecord
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotation
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotationType
import com.example.kaishelvesapp.data.model.DeviceReaderProgress
import com.example.kaishelvesapp.data.model.DeviceReaderSettings
import com.example.kaishelvesapp.data.model.DeviceReaderTheme
import java.net.URLEncoder
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class DeviceLibraryRepository(
    private val context: Context
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun registerScannedBooks(files: List<DeviceLibraryFile>): List<DeviceLibraryBookRecord> {
        val currentRecords = readBookRecords().associateBy { it.id }.toMutableMap()
        val now = System.currentTimeMillis()
        files.forEach { file ->
            val id = deviceBookId(file.uri.toString())
            val existing = currentRecords[id]
            val metadata = extractDisplayMetadata(file)
            currentRecords[id] = DeviceLibraryBookRecord(
                id = id,
                uri = file.uri.toString(),
                name = file.name,
                location = file.location,
                mimeType = file.mimeType.orEmpty(),
                format = detectFormat(file),
                title = metadata.title.ifBlank { existing?.title.orEmpty().ifBlank { file.name.substringBeforeLast('.') } },
                author = metadata.author.ifBlank { existing?.author.orEmpty() },
                description = metadata.description.ifBlank { existing?.description.orEmpty() },
                sizeBytes = file.sizeBytes,
                modifiedAtMillis = file.modifiedAtMillis,
                importedAtMillis = existing?.importedAtMillis ?: now,
                updatedAtMillis = now
            )
        }
        val updatedRecords = currentRecords.values.sortedBy { it.title.lowercase() }
        writeBookRecords(updatedRecords)
        return updatedRecords
    }

    fun getBookRecord(file: DeviceLibraryFile): DeviceLibraryBookRecord? {
        val id = deviceBookId(file.uri.toString())
        return readBookRecords().firstOrNull { it.id == id }
    }

    fun getBookRecords(): List<DeviceLibraryBookRecord> {
        return readBookRecords()
    }

    fun upsertBookRecord(file: DeviceLibraryFile) {
        val id = deviceBookId(file.uri.toString())
        val records = readBookRecords().filterNot { it.id == id }.toMutableList()
        val existing = getBookRecord(file)
        val metadata = extractDisplayMetadata(file)
        val now = System.currentTimeMillis()
        records += DeviceLibraryBookRecord(
            id = id,
            uri = file.uri.toString(),
            name = file.name,
            location = file.location,
            mimeType = file.mimeType.orEmpty(),
            format = detectFormat(file),
            title = metadata.title.ifBlank { existing?.title.orEmpty().ifBlank { file.name.substringBeforeLast('.') } },
            author = metadata.author.ifBlank { existing?.author.orEmpty() },
            description = metadata.description.ifBlank { existing?.description.orEmpty() },
            sizeBytes = file.sizeBytes,
            modifiedAtMillis = file.modifiedAtMillis,
            importedAtMillis = existing?.importedAtMillis ?: now,
            updatedAtMillis = now
        )
        writeBookRecords(records.sortedBy { it.title.lowercase() })
    }

    fun saveProgress(file: DeviceLibraryFile, currentPage: Int, pageCount: Int) {
        val safePageCount = pageCount.coerceAtLeast(1)
        val safePage = currentPage.coerceIn(0, safePageCount - 1)
        val progress = DeviceReaderProgress(
            bookId = deviceBookId(file.uri.toString()),
            currentPage = safePage,
            pageCount = safePageCount,
            percent = (((safePage + 1) * 100f) / safePageCount).toInt().coerceIn(0, 100),
            engine = detectFormat(file),
            updatedAtMillis = System.currentTimeMillis()
        )
        preferences.edit()
            .putString(progressKey(progress.bookId), progress.toJson().toString())
            .apply()
    }

    fun getProgress(file: DeviceLibraryFile): DeviceReaderProgress? {
        val id = deviceBookId(file.uri.toString())
        return preferences.getString(progressKey(id), null)?.let { raw ->
            runCatching { JSONObject(raw).toReaderProgress() }.getOrNull()
        }
    }

    fun saveSettings(settings: DeviceReaderSettings) {
        preferences.edit()
            .putString(settingsKey(settings.bookId), settings.toJson().toString())
            .apply()
    }

    fun getSettings(bookId: String): DeviceReaderSettings? {
        return preferences.getString(settingsKey(bookId), null)?.let { raw ->
            runCatching { JSONObject(raw).toReaderSettings() }.getOrNull()
        }
    }

    fun addAnnotation(file: DeviceLibraryFile, annotation: DeviceReaderAnnotation): DeviceReaderAnnotation {
        val bookId = deviceBookId(file.uri.toString())
        val now = System.currentTimeMillis()
        val storedAnnotation = annotation.copy(
            id = annotation.id.ifBlank { UUID.randomUUID().toString() },
            bookId = bookId,
            createdAtMillis = annotation.createdAtMillis.takeIf { it > 0L } ?: now,
            updatedAtMillis = now
        )
        val annotations = getAnnotations(file).filterNot { it.id == storedAnnotation.id } + storedAnnotation
        writeAnnotations(bookId, annotations.sortedWith(compareBy<DeviceReaderAnnotation> { it.page }.thenBy { it.createdAtMillis }))
        return storedAnnotation
    }

    fun updateAnnotation(file: DeviceLibraryFile, annotation: DeviceReaderAnnotation): DeviceReaderAnnotation {
        return addAnnotation(file, annotation.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    fun deleteAnnotation(file: DeviceLibraryFile, annotationId: String) {
        val bookId = deviceBookId(file.uri.toString())
        writeAnnotations(bookId, getAnnotations(file).filterNot { it.id == annotationId })
    }

    fun getAnnotations(file: DeviceLibraryFile): List<DeviceReaderAnnotation> {
        val bookId = deviceBookId(file.uri.toString())
        val raw = preferences.getString(annotationsKey(bookId), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toReaderAnnotation())
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun extractDisplayMetadata(file: DeviceLibraryFile): DeviceLibraryImportMetadata {
        return DeviceLibraryImportMetadata(title = file.name.substringBeforeLast('.'))
    }

    private fun detectFormat(file: DeviceLibraryFile): DeviceBookFormat {
        val name = file.name.lowercase()
        val mime = file.mimeType.orEmpty().lowercase()
        return when {
            name.endsWith(".pdf") || mime == "application/pdf" -> DeviceBookFormat.Pdf
            name.endsWith(".epub") || mime == "application/epub+zip" -> DeviceBookFormat.Epub
            name.endsWith(".txt") || mime.startsWith("text/") -> DeviceBookFormat.Txt
            name.endsWith(".fb2") -> DeviceBookFormat.Fb2
            name.endsWith(".mobi") -> DeviceBookFormat.Mobi
            name.endsWith(".azw") -> DeviceBookFormat.Azw
            name.endsWith(".azw3") -> DeviceBookFormat.Azw3
            name.endsWith(".cbz") -> DeviceBookFormat.Cbz
            else -> DeviceBookFormat.Unsupported
        }
    }

    private fun readBookRecords(): List<DeviceLibraryBookRecord> {
        val raw = preferences.getString(KEY_BOOK_RECORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toBookRecord())
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeBookRecords(records: List<DeviceLibraryBookRecord>) {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        preferences.edit()
            .putString(KEY_BOOK_RECORDS, array.toString())
            .apply()
    }

    private fun writeAnnotations(bookId: String, annotations: List<DeviceReaderAnnotation>) {
        val array = JSONArray()
        annotations.forEach { array.put(it.toJson()) }
        preferences.edit()
            .putString(annotationsKey(bookId), array.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "device_library_catalog"
        private const val KEY_BOOK_RECORDS = "book_records"

        fun deviceBookId(uri: String): String = URLEncoder.encode(uri, "UTF-8")

        private fun progressKey(bookId: String) = "progress_$bookId"
        private fun settingsKey(bookId: String) = "settings_$bookId"
        private fun annotationsKey(bookId: String) = "annotations_$bookId"
    }
}

private data class DeviceLibraryImportMetadata(
    val title: String = "",
    val author: String = "",
    val description: String = ""
)

private fun DeviceLibraryBookRecord.toJson() = JSONObject()
    .put("id", id)
    .put("uri", uri)
    .put("name", name)
    .put("location", location)
    .put("mimeType", mimeType)
    .put("format", format.name)
    .put("title", title)
    .put("author", author)
    .put("description", description)
    .put("sizeBytes", sizeBytes)
    .put("modifiedAtMillis", modifiedAtMillis)
    .put("importedAtMillis", importedAtMillis)
    .put("updatedAtMillis", updatedAtMillis)

private fun JSONObject.toBookRecord() = DeviceLibraryBookRecord(
    id = optString("id"),
    uri = optString("uri"),
    name = optString("name"),
    location = optString("location"),
    mimeType = optString("mimeType"),
    format = enumValueOrDefault(optString("format"), DeviceBookFormat.Unsupported),
    title = optString("title"),
    author = optString("author"),
    description = optString("description"),
    sizeBytes = nullableLong("sizeBytes"),
    modifiedAtMillis = nullableLong("modifiedAtMillis"),
    importedAtMillis = optLong("importedAtMillis"),
    updatedAtMillis = optLong("updatedAtMillis")
)

private fun DeviceReaderProgress.toJson() = JSONObject()
    .put("bookId", bookId)
    .put("currentPage", currentPage)
    .put("pageCount", pageCount)
    .put("percent", percent)
    .put("engine", engine.name)
    .put("updatedAtMillis", updatedAtMillis)

private fun JSONObject.toReaderProgress() = DeviceReaderProgress(
    bookId = optString("bookId"),
    currentPage = optInt("currentPage"),
    pageCount = optInt("pageCount", 1).coerceAtLeast(1),
    percent = optInt("percent").coerceIn(0, 100),
    engine = enumValueOrDefault(optString("engine"), DeviceBookFormat.Unsupported),
    updatedAtMillis = optLong("updatedAtMillis")
)

private fun DeviceReaderSettings.toJson() = JSONObject()
    .put("bookId", bookId)
    .put("theme", theme.name)
    .put("textSizePercent", textSizePercent)
    .put("reflowEnabled", reflowEnabled)
    .put("updatedAtMillis", updatedAtMillis)

private fun JSONObject.toReaderSettings() = DeviceReaderSettings(
    bookId = optString("bookId"),
    theme = enumValueOrDefault(optString("theme"), DeviceReaderTheme.Paper),
    textSizePercent = optInt("textSizePercent", 100),
    reflowEnabled = optBoolean("reflowEnabled", true),
    updatedAtMillis = optLong("updatedAtMillis")
)

private fun DeviceReaderAnnotation.toJson() = JSONObject()
    .put("id", id)
    .put("bookId", bookId)
    .put("type", type.name)
    .put("page", page)
    .put("pageCount", pageCount)
    .put("sourcePage", sourcePage)
    .put("selectionStart", selectionStart)
    .put("selectionEnd", selectionEnd)
    .put("selectedText", selectedText)
    .put("note", note)
    .put("color", color)
    .put("createdAtMillis", createdAtMillis)
    .put("updatedAtMillis", updatedAtMillis)

private fun JSONObject.toReaderAnnotation() = DeviceReaderAnnotation(
    id = optString("id"),
    bookId = optString("bookId"),
    type = enumValueOrDefault(optString("type"), DeviceReaderAnnotationType.Bookmark),
    page = optInt("page"),
    pageCount = optInt("pageCount", 1).coerceAtLeast(1),
    sourcePage = optInt("sourcePage", -1),
    selectionStart = optInt("selectionStart", -1),
    selectionEnd = optInt("selectionEnd", -1),
    selectedText = optString("selectedText"),
    note = optString("note"),
    color = optString("color"),
    createdAtMillis = optLong("createdAtMillis"),
    updatedAtMillis = optLong("updatedAtMillis")
)

private fun JSONObject.nullableLong(key: String): Long? {
    return if (has(key) && !isNull(key)) optLong(key) else null
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, fallback: T): T {
    return enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
