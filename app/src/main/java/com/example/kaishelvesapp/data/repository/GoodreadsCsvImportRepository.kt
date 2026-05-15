package com.example.kaishelvesapp.data.repository

import android.net.Uri
import com.example.kaishelvesapp.data.local.AppContextProvider
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.remote.googlebooks.GoogleBooksClient
import com.example.kaishelvesapp.data.remote.googlebooks.toLibro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoodreadsCsvImportResult(
    val importedBooks: Int,
    val skippedRows: Int
)

data class GoodreadsCsvImportProgress(
    val processedRows: Int,
    val totalRows: Int,
    val importedBooks: Int,
    val skippedRows: Int
)

class GoodreadsCsvImportRepository(
    private val userListsRepository: UserListsRepository = UserListsRepository()
) {

    suspend fun importFromUri(
        uri: Uri,
        onProgress: (GoodreadsCsvImportProgress) -> Unit = {}
    ): Result<GoodreadsCsvImportResult> {
        return try {
            val csvText = withContext(Dispatchers.IO) {
                AppContextProvider.requireContext()
                    .contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IllegalArgumentException("No se pudo abrir el archivo CSV")
            }

            val rows = parseCsv(csvText)
            if (rows.size < 2) {
                return Result.failure(IllegalArgumentException("El archivo CSV no contiene libros"))
            }

            val header = rows.first().map { it.trim().removePrefix("\uFEFF") }
            val importedRows = rows
                .drop(1)
                .map { fields -> header.zip(fields + List((header.size - fields.size).coerceAtLeast(0)) { "" }).toMap() }

            var importedBooks = 0
            var skippedRows = 0
            var processedRows = 0
            val totalRows = importedRows.size
            val customListIdsByShelf = mutableMapOf<String, String>()

            onProgress(
                GoodreadsCsvImportProgress(
                    processedRows = processedRows,
                    totalRows = totalRows,
                    importedBooks = importedBooks,
                    skippedRows = skippedRows
                )
            )

            importedRows.forEach { row ->
                val book = row.toBook().withGoogleBooksCover()
                if (book.titulo.isBlank()) {
                    skippedRows += 1
                    processedRows += 1
                    onProgress(
                        GoodreadsCsvImportProgress(
                            processedRows = processedRows,
                            totalRows = totalRows,
                            importedBooks = importedBooks,
                            skippedRows = skippedRows
                        )
                    )
                    return@forEach
                }

                val listId = row.targetListId(customListIdsByShelf).getOrThrow()
                userListsRepository.updateBookAssignments(
                    libro = book,
                    selectedListIds = setOf(listId),
                    readMetadata = row.toReadMetadata().takeIf { listId == UserListsRepository.SYSTEM_LIST_READ_ID }
                ).getOrThrow()
                importedBooks += 1
                processedRows += 1
                onProgress(
                    GoodreadsCsvImportProgress(
                        processedRows = processedRows,
                        totalRows = totalRows,
                        importedBooks = importedBooks,
                        skippedRows = skippedRows
                    )
                )
            }

            Result.success(
                GoodreadsCsvImportResult(
                    importedBooks = importedBooks,
                    skippedRows = skippedRows
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun Map<String, String>.targetListId(customListIdsByShelf: MutableMap<String, String>): Result<String> {
        val rawShelf = this["Exclusive Shelf"].orEmpty()
            .ifBlank { this["Bookshelves"].orEmpty().split(',').firstOrNull().orEmpty() }
            .trim()
            .ifBlank { "to-read" }

        val systemListId = when (rawShelf.normalizeShelfKey()) {
            "read" -> UserListsRepository.SYSTEM_LIST_READ_ID
            "currently-reading", "reading" -> UserListsRepository.SYSTEM_LIST_READING_ID
            "to-read", "want-to-read", "want-to-read-books" -> UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID
            "unfinished", "dnf", "did-not-finish" -> UserListsRepository.SYSTEM_LIST_UNFINISHED_ID
            "pending" -> UserListsRepository.SYSTEM_LIST_PENDING_ID
            else -> null
        }

        if (systemListId != null) {
            return Result.success(systemListId)
        }

        val customName = rawShelf.toDisplayShelfName()
        val existingId = customListIdsByShelf[customName]
        if (existingId != null) {
            return Result.success(existingId)
        }

        return userListsRepository
            .getOrCreateCustomList(
                name = customName,
                description = "Importada desde Goodreads."
            )
            .onSuccess { listId -> customListIdsByShelf[customName] = listId }
    }

    private fun Map<String, String>.toBook(): Libro {
        val isbn13 = this["ISBN13"].orEmpty().cleanGoodreadsIsbn()
        val isbn = isbn13.ifBlank { this["ISBN"].orEmpty().cleanGoodreadsIsbn() }
        val bookId = this["Book Id"].orEmpty().trim()
        val author = buildList {
            this@toBook["Author"].orEmpty().trim().collapseSpaces().takeIf { it.isNotBlank() }?.let(::add)
            this@toBook["Additional Authors"].orEmpty().trim().collapseSpaces().takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(", ")

        return Libro(
            id = isbn.ifBlank { "goodreads_$bookId" },
            isbn = isbn,
            titulo = this["Title"].orEmpty().trim(),
            autor = author,
            editorial = this["Publisher"].orEmpty().trim(),
            fechaPublicacion = this["Original Publication Year"].orEmpty().toIntOrNull()
                ?: this["Year Published"].orEmpty().toIntOrNull()
                ?: 0,
            paginas = this["Number of Pages"].orEmpty().toIntOrNull() ?: 0
        )
    }

    private suspend fun Libro.withGoogleBooksCover(): Libro {
        val normalizedIsbn = isbn.normalizeIsbn()
        if (normalizedIsbn.isBlank()) return this

        val googleBook = runCatching {
            val api = if (GoogleBooksClient.hasApiKey) {
                GoogleBooksClient.api
            } else {
                GoogleBooksClient.publicApi
            }
            api.searchBooks(
                query = "isbn:$normalizedIsbn",
                maxResults = 1,
                startIndex = 0
            ).items.firstOrNull()?.toLibro()
        }.getOrNull() ?: return this

        return copy(
            imagen = googleBook.imagen.ifBlank { imagen },
            genero = genero.ifBlank { googleBook.genero },
            editorial = editorial.ifBlank { googleBook.editorial },
            paginas = paginas.takeIf { it > 0 } ?: googleBook.paginas,
            fechaPublicacion = fechaPublicacion.takeIf { it > 0 } ?: googleBook.fechaPublicacion
        )
    }

    private fun Map<String, String>.toReadMetadata(): ImportedReadMetadata {
        return ImportedReadMetadata(
            readDate = this["Date Read"].orEmpty().toKaiDate(),
            rating = this["My Rating"].orEmpty().toIntOrNull()?.coerceIn(0, 5) ?: 0,
            review = this["My Review"].orEmpty().trim(),
            containsSpoilers = this["Spoiler"].orEmpty().trim().equals("true", ignoreCase = true)
        )
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && insideQuotes && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"')
                    index += 1
                }
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    row.add(field.toString())
                    field.clear()
                }
                (char == '\n' || char == '\r') && !insideQuotes -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                        index += 1
                    }
                    row.add(field.toString())
                    field.clear()
                    if (row.any { it.isNotBlank() }) {
                        rows.add(row)
                    }
                    row = mutableListOf()
                }
                else -> field.append(char)
            }
            index += 1
        }

        row.add(field.toString())
        if (row.any { it.isNotBlank() }) {
            rows.add(row)
        }
        return rows
    }

    private fun String.cleanGoodreadsIsbn(): String {
        return trim()
            .removePrefix("=\"")
            .removeSuffix("\"")
            .filter { it.isDigit() || it == 'X' || it == 'x' }
    }

    private fun String.normalizeIsbn(): String {
        return trim()
            .replace("-", "")
            .replace(" ", "")
            .uppercase()
            .filter { it.isDigit() || it == 'X' }
    }

    private fun String.normalizeShelfKey(): String {
        return trim()
            .lowercase()
            .replace('_', '-')
            .replace(' ', '-')
    }

    private fun String.toDisplayShelfName(): String {
        val normalized = normalizeShelfKey()
        if (normalized == "owned") return "Tengo (Goodreads)"
        return normalized
            .split('-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Goodreads" }
    }

    private fun String.toKaiDate(): String {
        val trimmed = trim()
        if (trimmed.isBlank()) return ""
        val parts = trimmed.split('/', '-')
        if (parts.size != 3) return trimmed
        return "${parts[0].padStart(4, '0')}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
    }

    private fun String.collapseSpaces(): String = replace(Regex("\\s+"), " ")
}
