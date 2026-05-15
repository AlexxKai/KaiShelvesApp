package com.example.kaishelvesapp.data.local

import android.content.Context
import com.example.kaishelvesapp.data.model.Libro
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object DiscoverCatalogLocalStore {
    private const val PREFS_NAME = "kai_discover_catalog_cache"
    private const val KEY_PREFIX = "discover_books_"
    private const val REFRESH_PREFIX = "discover_refresh_"

    private val gson: Gson = GsonBuilder().create()
    private val bookListType = object : TypeToken<List<Libro>>() {}.type

    private fun prefs() = AppContextProvider.requireContext().getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun read(languageTag: String, modeKey: String): List<Libro> {
        val cachedBooks = readPersisted(languageTag, modeKey)

        if (cachedBooks.isNotEmpty()) {
            return cachedBooks
        }

        // En el primer arranque se deja una base persistida por modo para que Descubre no dependa de red.
        val seededBooks = defaultDiscoverBooks(modeKey)
        write(languageTag, modeKey, seededBooks)
        return seededBooks
    }

    @Synchronized
    fun readPersisted(languageTag: String, modeKey: String): List<Libro> {
        val key = cacheKey(languageTag, modeKey)
        val rawBooks = prefs().getString(key, null)

        return rawBooks
            ?.let { raw -> runCatching { gson.fromJson<List<Libro>>(raw, bookListType) }.getOrNull() }
            .orEmpty()
            .filter { book -> book.titulo.isNotBlank() }
    }

    @Synchronized
    fun write(languageTag: String, modeKey: String, books: List<Libro>) {
        val storableBooks = books
            .filter { book -> book.titulo.isNotBlank() }
            .distinctBy(::bookKey)
            .take(40)

        if (storableBooks.isEmpty()) return

        prefs()
            .edit()
            .putString(cacheKey(languageTag, modeKey), gson.toJson(storableBooks))
            .apply()
    }

    @Synchronized
    fun nextRefreshIndex(languageTag: String, modeKey: String): Int {
        val key = refreshKey(languageTag, modeKey)
        val nextIndex = prefs().getInt(key, 0) + 1

        prefs()
            .edit()
            .putInt(key, nextIndex)
            .apply()

        return nextIndex
    }

    private fun cacheKey(languageTag: String, modeKey: String): String {
        return KEY_PREFIX + modeKey.ifBlank { "special" } + "_" + languageTag.ifBlank { "default" }
    }

    private fun refreshKey(languageTag: String, modeKey: String): String {
        return REFRESH_PREFIX + modeKey.ifBlank { "special" } + "_" + languageTag.ifBlank { "default" }
    }

    private fun bookKey(book: Libro): String {
        return book.id.ifBlank { book.isbn }
            .ifBlank { "${book.titulo.lowercase()}-${book.autor.lowercase()}" }
    }

    private fun defaultDiscoverBooks(modeKey: String): List<Libro> {
        return when (modeKey) {
            "current" -> defaultCurrentBooks()
            "top_rated" -> defaultTopRatedBooks()
            "known_authors" -> defaultKnownAuthorBooks()
            else -> defaultSpecialBooks()
        }
    }

    private fun defaultSpecialBooks(): List<Libro> {
        return listOf(
            Libro(
                id = "seed-don-quijote",
                titulo = "Don Quijote de la Mancha",
                autor = "Miguel de Cervantes",
                editorial = "Dominio público",
                genero = "Clásicos",
                fechaPublicacion = 1605,
                imagen = "https://books.google.com/books/content?id=6TseKZ9cQzYC&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-frankenstein",
                titulo = "Frankenstein",
                autor = "Mary Shelley",
                editorial = "Dominio público",
                genero = "Terror",
                fechaPublicacion = 1818,
                imagen = "https://books.google.com/books/content?id=Z5GzEAAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-pride-prejudice",
                titulo = "Orgullo y prejuicio",
                autor = "Jane Austen",
                editorial = "Dominio público",
                genero = "Novela",
                fechaPublicacion = 1813,
                imagen = "https://books.google.com/books/content?id=s1gVAAAAYAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-dracula",
                titulo = "Drácula",
                autor = "Bram Stoker",
                editorial = "Dominio público",
                genero = "Terror",
                fechaPublicacion = 1897,
                imagen = "https://books.google.com/books/content?id=QI5XDwAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-alice",
                titulo = "Alicia en el país de las maravillas",
                autor = "Lewis Carroll",
                editorial = "Dominio público",
                genero = "Fantasía",
                fechaPublicacion = 1865,
                imagen = "https://books.google.com/books/content?id=FzQ8AAAAIAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-treasure-island",
                titulo = "La isla del tesoro",
                autor = "Robert Louis Stevenson",
                editorial = "Dominio público",
                genero = "Aventura",
                fechaPublicacion = 1883,
                imagen = "https://books.google.com/books/content?id=OUEXAAAAYAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-lazarillo",
                titulo = "La vida de Lazarillo de Tormes",
                autor = "Anónimo",
                editorial = "Dominio público",
                genero = "Clásicos",
                fechaPublicacion = 1554,
                imagen = "https://books.google.com/books/content?id=fDhRAAAAcAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            ),
            Libro(
                id = "seed-war-worlds",
                titulo = "La guerra de los mundos",
                autor = "H. G. Wells",
                editorial = "Dominio público",
                genero = "Ciencia ficción",
                fechaPublicacion = 1898,
                imagen = "https://books.google.com/books/content?id=BHw_AQAAMAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
            )
        )
    }

    private fun defaultCurrentBooks(): List<Libro> {
        return listOf(
            Libro(id = "seed-current-01", titulo = "Fourth Wing", autor = "Rebecca Yarros", genero = "Fantasia", fechaPublicacion = 2023),
            Libro(id = "seed-current-02", titulo = "Yellowface", autor = "R. F. Kuang", genero = "Ficcion", fechaPublicacion = 2023),
            Libro(id = "seed-current-03", titulo = "Tomorrow, and Tomorrow, and Tomorrow", autor = "Gabrielle Zevin", genero = "Ficcion", fechaPublicacion = 2022),
            Libro(id = "seed-current-04", titulo = "Lessons in Chemistry", autor = "Bonnie Garmus", genero = "Ficcion", fechaPublicacion = 2022),
            Libro(id = "seed-current-05", titulo = "Babel", autor = "R. F. Kuang", genero = "Fantasia", fechaPublicacion = 2022),
            Libro(id = "seed-current-06", titulo = "The Heaven & Earth Grocery Store", autor = "James McBride", genero = "Ficcion", fechaPublicacion = 2023),
            Libro(id = "seed-current-07", titulo = "The Covenant of Water", autor = "Abraham Verghese", genero = "Historica", fechaPublicacion = 2023),
            Libro(id = "seed-current-08", titulo = "El problema final", autor = "Arturo Perez-Reverte", genero = "Misterio", fechaPublicacion = 2023)
        )
    }

    private fun defaultTopRatedBooks(): List<Libro> {
        return listOf(
            Libro(id = "seed-rated-01", titulo = "The Name of the Wind", autor = "Patrick Rothfuss", genero = "Fantasia", fechaPublicacion = 2007, averageRating = 4.5, ratingsCount = 1000),
            Libro(id = "seed-rated-02", titulo = "Project Hail Mary", autor = "Andy Weir", genero = "Ciencia ficcion", fechaPublicacion = 2021, averageRating = 4.5, ratingsCount = 1000),
            Libro(id = "seed-rated-03", titulo = "The Way of Kings", autor = "Brandon Sanderson", genero = "Fantasia", fechaPublicacion = 2010, averageRating = 4.6, ratingsCount = 1000),
            Libro(id = "seed-rated-04", titulo = "The Hobbit", autor = "J. R. R. Tolkien", genero = "Fantasia", fechaPublicacion = 1937, averageRating = 4.3, ratingsCount = 1000),
            Libro(id = "seed-rated-05", titulo = "Dune", autor = "Frank Herbert", genero = "Ciencia ficcion", fechaPublicacion = 1965, averageRating = 4.2, ratingsCount = 1000),
            Libro(id = "seed-rated-06", titulo = "Pachinko", autor = "Min Jin Lee", genero = "Historica", fechaPublicacion = 2017, averageRating = 4.3, ratingsCount = 1000),
            Libro(id = "seed-rated-07", titulo = "Circe", autor = "Madeline Miller", genero = "Fantasia", fechaPublicacion = 2018, averageRating = 4.2, ratingsCount = 1000),
            Libro(id = "seed-rated-08", titulo = "The Book Thief", autor = "Markus Zusak", genero = "Historica", fechaPublicacion = 2005, averageRating = 4.4, ratingsCount = 1000)
        )
    }

    private fun defaultKnownAuthorBooks(): List<Libro> {
        return listOf(
            Libro(id = "seed-author-01", titulo = "The Shining", autor = "Stephen King", genero = "Terror", fechaPublicacion = 1977),
            Libro(id = "seed-author-02", titulo = "Murder on the Orient Express", autor = "Agatha Christie", genero = "Misterio", fechaPublicacion = 1934),
            Libro(id = "seed-author-03", titulo = "Kafka on the Shore", autor = "Haruki Murakami", genero = "Ficcion", fechaPublicacion = 2002),
            Libro(id = "seed-author-04", titulo = "La casa de los espiritus", autor = "Isabel Allende", genero = "Realismo magico", fechaPublicacion = 1982),
            Libro(id = "seed-author-05", titulo = "The Left Hand of Darkness", autor = "Ursula K. Le Guin", genero = "Ciencia ficcion", fechaPublicacion = 1969),
            Libro(id = "seed-author-06", titulo = "American Gods", autor = "Neil Gaiman", genero = "Fantasia", fechaPublicacion = 2001),
            Libro(id = "seed-author-07", titulo = "Cien anos de soledad", autor = "Gabriel Garcia Marquez", genero = "Realismo magico", fechaPublicacion = 1967),
            Libro(id = "seed-author-08", titulo = "El juego del angel", autor = "Carlos Ruiz Zafon", genero = "Misterio", fechaPublicacion = 2008)
        )
    }
}
