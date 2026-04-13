package com.example.kaishelvesapp.data.remote.googlebooks

import com.example.kaishelvesapp.data.model.Libro

private fun normalize(value: String?): String {
    return value?.trim().orEmpty()
}

private fun parsePublishedYear(publishedDate: String?): Int {
    val rawValue = publishedDate
        ?.trim()
        .orEmpty()

    return rawValue
        .take(4)
        .toIntOrNull()
        ?: 0
}

private fun normalizeImageUrl(url: String?): String {
    return url
        ?.trim()
        .orEmpty()
        .replace("http://", "https://")
}

private fun extractBestIsbn(identifiers: List<GoogleIndustryIdentifier>?): String {
    val isbn13 = identifiers
        ?.firstOrNull { it.type == "ISBN_13" }
        ?.identifier
        .orEmpty()

    if (isbn13.isNotBlank()) return isbn13

    return identifiers
        ?.firstOrNull { it.type == "ISBN_10" }
        ?.identifier
        .orEmpty()
}

fun GoogleBookItem.toLibro(fallbackGenero: String = ""): Libro {
    val info = volumeInfo
    val bestIsbn = extractBestIsbn(info?.industryIdentifiers)

    return Libro(
        id = normalize(id).ifBlank { bestIsbn },
        isbn = bestIsbn,
        titulo = normalize(info?.title),
        autor = info?.authors
            ?.filter { it.isNotBlank() }
            ?.joinToString(", ")
            .orEmpty(),
        editorial = normalize(info?.publisher),
        genero = when {
            fallbackGenero.isNotBlank() -> fallbackGenero
            !info?.categories.isNullOrEmpty() -> info?.categories?.firstOrNull().orEmpty()
            else -> ""
        },
        fechaPublicacion = parsePublishedYear(info?.publishedDate),
        paginas = info?.pageCount ?: 0,
        imagen = normalizeImageUrl(
            info?.imageLinks?.thumbnail
                ?: info?.imageLinks?.smallThumbnail
        ),
        pdf = normalize(accessInfo?.webReaderLink)
    )
}
