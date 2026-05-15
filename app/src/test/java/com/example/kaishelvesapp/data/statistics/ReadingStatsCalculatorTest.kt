package com.example.kaishelvesapp.data.statistics

import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingStatsCalculatorTest {

    @Test
    fun buildSnapshot_deduplicatesReadBooksAndKeepsListTotal() {
        val readBooks = listOf(
            LibroLeido(isbn = " 978-1 ", titulo = "Uno", autor = "Autora", paginas = 120, puntuacion = 4),
            LibroLeido(isbn = "978-2", titulo = "Dos", autor = "Autor", paginas = 220, puntuacion = 2),
            LibroLeido(titulo = "Sin ISBN", autor = "Autora", paginas = 80, puntuacion = 0)
        )
        val readListBooks = listOf(
            Libro(isbn = "978-1", titulo = "Uno", autor = "Autora", genero = "Fantasía"),
            Libro(isbn = "978-1", titulo = "Uno duplicado", autor = "Autora", genero = "Fantasía"),
            Libro(titulo = "Sin ISBN", autor = "Autora", genero = "Misterio")
        )

        val snapshot = ReadingStatsCalculator.buildSnapshot(
            books = readBooks,
            readListBooks = readListBooks,
            totalUniqueBooksInLists = 7,
            updatedAtMillis = 123L
        )

        assertEquals(7, snapshot.totalBooksInLists)
        assertEquals(2, snapshot.totalReadBooks)
        assertEquals(3.0, snapshot.averageRating ?: 0.0, 0.001)
        assertEquals("Fantasía", snapshot.favoriteGenre)
        assertEquals(420, snapshot.totalPages)
        assertEquals(123L, snapshot.updatedAtMillis)
    }

    @Test
    fun buildSnapshot_withoutRatingsReturnsNullAverageAndEmptyGenre() {
        val snapshot = ReadingStatsCalculator.buildSnapshot(
            books = listOf(LibroLeido(titulo = "Lectura", autor = "Autor", puntuacion = 0)),
            readListBooks = listOf(Libro(titulo = "Lectura", autor = "Autor")),
            totalUniqueBooksInLists = 1,
            updatedAtMillis = 99L
        )

        assertNull(snapshot.averageRating)
        assertEquals("", snapshot.favoriteGenre)
        assertEquals(1, snapshot.totalReadBooks)
    }

    @Test
    fun identityKey_prefersIsbnThenIdThenTitleAndAuthor() {
        assertEquals(
            "isbn:978-x",
            ReadingStatsCalculator.bookIdentityKey(Libro(isbn = " 978-X ", id = "ignored"))
        )
        assertEquals(
            "id:book-1",
            ReadingStatsCalculator.bookIdentityKey(Libro(id = " Book-1 ", titulo = "ignored"))
        )
        assertEquals(
            "title:la casa|author:ana",
            ReadingStatsCalculator.bookIdentityKey(Libro(titulo = " La Casa ", autor = " Ana "))
        )
    }
}
