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

private fun extractBestIsbn(values: List<String>?): String {
    val normalizedValues = values
        ?.map { normalize(it).replace("-", "") }
        ?.filter { it.isNotBlank() }
        .orEmpty()

    return normalizedValues.firstOrNull { it.length == 13 }
        ?: normalizedValues.firstOrNull { it.length == 10 }
        ?: normalizedValues.firstOrNull()
        .orEmpty()
}

fun OpenLibrarySearchDoc.toLibro(): Libro {
    val workKey = normalize(key)
    val bestIsbn = extractBestIsbn(isbn)
    val fallbackId = workKey
        .removePrefix("/works/")
        .ifBlank { bestIsbn }

    return Libro(
        id = fallbackId,
        isbn = bestIsbn,
        titulo = normalize(title),
        autor = authorNames
            ?.mapNotNull { normalize(it).takeIf(String::isNotBlank) }
            ?.joinToString(", ")
            .orEmpty(),
        editorial = publishers
            ?.firstOrNull()
            ?.let(::normalize)
            .orEmpty(),
        genero = subjects
            ?.firstOrNull()
            ?.let(::normalize)
            .orEmpty(),
        fechaPublicacion = firstPublishYear ?: 0,
        paginas = numberOfPagesMedian ?: 0,
        averageRating = 0.0,
        ratingsCount = 0,
        imagen = coverId
            ?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
            .orEmpty(),
        pdf = workKey
            .takeIf { it.isNotBlank() }
            ?.let { "https://openlibrary.org$it" }
            .orEmpty()
    )
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
