package com.example.kaishelvesapp.data.statistics

import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.ReadingStatsSnapshot

object ReadingStatsCalculator {

    fun buildSnapshot(
        books: List<LibroLeido>,
        readListBooks: List<Libro>,
        totalUniqueBooksInLists: Int,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): ReadingStatsSnapshot {
        val ratedBooks = books.filter { it.puntuacion > 0 }
        val favoriteGenre = readListBooks
            .map { it.genero.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            .orEmpty()

        return ReadingStatsSnapshot(
            totalBooksInLists = totalUniqueBooksInLists,
            totalReadBooks = readListBooks.distinctBy(::bookIdentityKey).size,
            averageRating = ratedBooks
                .takeIf { it.isNotEmpty() }
                ?.map { it.puntuacion }
                ?.average(),
            favoriteGenre = favoriteGenre,
            totalPages = books.sumOf { it.paginas },
            updatedAtMillis = updatedAtMillis
        )
    }

    fun bookIdentityKey(book: Libro): String {
        return when {
            book.isbn.isNotBlank() -> "isbn:${book.isbn.trim().lowercase()}"
            book.id.isNotBlank() -> "id:${book.id.trim().lowercase()}"
            else -> "title:${book.titulo.trim().lowercase()}|author:${book.autor.trim().lowercase()}"
        }
    }

    fun readBookIdentityKey(book: LibroLeido): String {
        return when {
            book.isbn.isNotBlank() -> "isbn:${book.isbn.trim().lowercase()}"
            book.id.isNotBlank() -> "id:${book.id.trim().lowercase()}"
            else -> "title:${book.titulo.trim().lowercase()}|author:${book.autor.trim().lowercase()}"
        }
    }
}
