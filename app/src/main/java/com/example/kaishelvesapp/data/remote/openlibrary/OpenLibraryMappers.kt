package com.example.kaishelvesapp.data.remote.openlibrary

import com.example.kaishelvesapp.data.model.Libro

private fun normalize(value: String?): String {
    return value?.trim().orEmpty()
}

private fun parsePublishedYear(value: String?): Int {
    return Regex("\\d{4}")
        .find(value.orEmpty())
        ?.value
        ?.toIntOrNull()
        ?: 0
}

private fun normalizeImageUrl(url: String?): String {
    return normalize(url).replace("http://", "https://")
}

fun OpenLibraryBookData.toLibro(isbn: String): Libro {
    val normalizedIsbn = normalize(isbn)
    val title = normalize(title)

    return Libro(
        id = normalizedIsbn,
        isbn = normalizedIsbn,
        titulo = title,
        autor = authors
            ?.mapNotNull { normalize(it.name).takeIf(String::isNotBlank) }
            ?.joinToString(", ")
            .orEmpty(),
        editorial = publishers
            ?.firstOrNull()
            ?.name
            ?.let(::normalize)
            .orEmpty(),
        genero = subjects
            ?.firstOrNull()
            ?.name
            ?.let(::normalize)
            .orEmpty(),
        fechaPublicacion = parsePublishedYear(publishDate),
        paginas = numberOfPages ?: 0,
        averageRating = 0.0,
        ratingsCount = 0,
        imagen = normalizeImageUrl(cover?.large ?: cover?.medium ?: cover?.small),
        pdf = normalize(url)
    )
}
