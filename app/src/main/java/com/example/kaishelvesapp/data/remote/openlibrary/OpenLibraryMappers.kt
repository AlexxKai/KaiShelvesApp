package com.example.kaishelvesapp.data.remote.openlibrary

import com.example.kaishelvesapp.data.model.Libro

private fun sanitizeOpenLibraryKey(key: String?): String {
    return key
        ?.trim()
        .orEmpty()
        .replace("/", "_")
}

private fun coverUrl(
    isbn: String?,
    olid: String?,
    coverId: Int?
): String {
    return when {
        !isbn.isNullOrBlank() ->
            "https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"
        !olid.isNullOrBlank() ->
            "https://covers.openlibrary.org/b/olid/$olid-L.jpg?default=false"
        coverId != null ->
            "https://covers.openlibrary.org/b/id/$coverId-L.jpg?default=false"
        else -> ""
    }
}

private fun normalize(value: String?): String {
    return value?.trim().orEmpty()
}

fun OpenLibraryBookDoc.toLibro(fallbackGenero: String = ""): Libro {
    val bestIsbn = isbns
        ?.firstOrNull { !it.isNullOrBlank() }
        .orEmpty()

    val internalId = when {
        bestIsbn.isNotBlank() -> bestIsbn
        !coverEditionKey.isNullOrBlank() -> coverEditionKey
        !key.isNullOrBlank() -> sanitizeOpenLibraryKey(key)
        else -> ""
    }

    val autor = authorNames
        ?.filter { it.isNotBlank() }
        ?.joinToString(", ")
        .orEmpty()

    val editorial = publishers
        ?.firstOrNull()
        ?.trim()
        .orEmpty()

    val genero = when {
        fallbackGenero.isNotBlank() -> fallbackGenero
        !subjects.isNullOrEmpty() -> subjects.firstOrNull().orEmpty()
        else -> ""
    }

    return Libro(
        id = internalId,
        isbn = bestIsbn,
        titulo = normalize(title),
        autor = autor,
        editorial = editorial,
        genero = genero,
        fechaPublicacion = firstPublishYear ?: 0,
        paginas = numberOfPagesMedian ?: 0,
        imagen = coverUrl(
            isbn = bestIsbn.ifBlank { null },
            olid = coverEditionKey,
            coverId = coverId
        ),
        pdf = ""
    )
}