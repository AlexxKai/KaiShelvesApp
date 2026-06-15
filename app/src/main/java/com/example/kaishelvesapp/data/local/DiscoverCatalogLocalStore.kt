package com.example.kaishelvesapp.data.local

import android.content.Context
import com.example.kaishelvesapp.data.model.Libro
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object DiscoverCatalogLocalStore {
    private const val PREFS_NAME = "kai_discover_catalog_cache"
    private const val KEY_PREFIX = "discover_books_v2_"
    private const val REFRESH_PREFIX = "discover_refresh_v2_"

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

        val seededBooks = defaultDiscoverBooks(languageTag, modeKey)
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

    private fun defaultDiscoverBooks(languageTag: String, modeKey: String): List<Libro> {
        return when (modeKey) {
            "current" -> defaultCurrentBooks(languageTag)
            "top_rated" -> defaultTopRatedBooks(languageTag)
            "known_authors" -> defaultKnownAuthorBooks(languageTag)
            else -> defaultSpecialBooks(languageTag)
        }
    }

    private fun defaultSpecialBooks(languageTag: String): List<Libro> {
        val es = languageTag == "es"
        return listOf(
            Libro(
                id = "seed-don-quijote",
                titulo = "Don Quijote de la Mancha",
                autor = "Miguel de Cervantes",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Clasicos" else "Classics",
                fechaPublicacion = 1605,
                imagen = coverByTitle("Don Quijote de la Mancha")
            ),
            Libro(
                id = "seed-frankenstein",
                titulo = "Frankenstein",
                autor = "Mary Shelley",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Terror" else "Horror",
                fechaPublicacion = 1818,
                imagen = coverByTitle("Frankenstein")
            ),
            Libro(
                id = "seed-pride-prejudice",
                titulo = if (es) "Orgullo y prejuicio" else "Pride and Prejudice",
                autor = "Jane Austen",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Novela" else "Novel",
                fechaPublicacion = 1813,
                imagen = coverByTitle("Pride and Prejudice")
            ),
            Libro(
                id = "seed-dracula",
                titulo = "Dracula",
                autor = "Bram Stoker",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Terror" else "Horror",
                fechaPublicacion = 1897,
                imagen = coverByTitle("Dracula")
            ),
            Libro(
                id = "seed-alice",
                titulo = if (es) "Alicia en el pais de las maravillas" else "Alice's Adventures in Wonderland",
                autor = "Lewis Carroll",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Fantasia" else "Fantasy",
                fechaPublicacion = 1865,
                imagen = coverByTitle("Alice's Adventures in Wonderland")
            ),
            Libro(
                id = "seed-treasure-island",
                titulo = if (es) "La isla del tesoro" else "Treasure Island",
                autor = "Robert Louis Stevenson",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Aventura" else "Adventure",
                fechaPublicacion = 1883,
                imagen = coverByTitle("Treasure Island")
            ),
            Libro(
                id = "seed-lazarillo",
                titulo = "La vida de Lazarillo de Tormes",
                autor = if (es) "Anonimo" else "Anonymous",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Clasicos" else "Classics",
                fechaPublicacion = 1554,
                imagen = coverByTitle("Lazarillo de Tormes")
            ),
            Libro(
                id = "seed-war-worlds",
                titulo = if (es) "La guerra de los mundos" else "The War of the Worlds",
                autor = "H. G. Wells",
                editorial = if (es) "Dominio publico" else "Public domain",
                genero = if (es) "Ciencia ficcion" else "Science fiction",
                fechaPublicacion = 1898,
                imagen = coverByTitle("The War of the Worlds")
            )
        )
    }

    private fun defaultCurrentBooks(languageTag: String): List<Libro> {
        if (languageTag == "es") {
            return listOf(
                seeded("seed-current-01", "Alas de sangre", "Rebecca Yarros", "Fantasia", 2023, "Fourth Wing"),
                seeded("seed-current-02", "Amarilla", "R. F. Kuang", "Ficcion", 2023, "Yellowface"),
                seeded("seed-current-03", "Manana, y manana, y manana", "Gabrielle Zevin", "Ficcion", 2022, "Tomorrow, and Tomorrow, and Tomorrow"),
                seeded("seed-current-04", "Lecciones de quimica", "Bonnie Garmus", "Ficcion", 2022, "Lessons in Chemistry"),
                seeded("seed-current-05", "Babel", "R. F. Kuang", "Fantasia", 2022, "Babel"),
                seeded("seed-current-06", "La tienda de los suenos", "James McBride", "Ficcion", 2023, "The Heaven & Earth Grocery Store"),
                seeded("seed-current-07", "El pacto del agua", "Abraham Verghese", "Historica", 2023, "The Covenant of Water"),
                seeded("seed-current-08", "El problema final", "Arturo Perez-Reverte", "Misterio", 2023, "El problema final")
            )
        }

        return listOf(
            seeded("seed-current-01", "Fourth Wing", "Rebecca Yarros", "Fantasy", 2023, "Fourth Wing"),
            seeded("seed-current-02", "Yellowface", "R. F. Kuang", "Fiction", 2023, "Yellowface"),
            seeded("seed-current-03", "Tomorrow, and Tomorrow, and Tomorrow", "Gabrielle Zevin", "Fiction", 2022, "Tomorrow, and Tomorrow, and Tomorrow"),
            seeded("seed-current-04", "Lessons in Chemistry", "Bonnie Garmus", "Fiction", 2022, "Lessons in Chemistry"),
            seeded("seed-current-05", "Babel", "R. F. Kuang", "Fantasy", 2022, "Babel"),
            seeded("seed-current-06", "The Heaven & Earth Grocery Store", "James McBride", "Fiction", 2023, "The Heaven & Earth Grocery Store"),
            seeded("seed-current-07", "The Covenant of Water", "Abraham Verghese", "Historical fiction", 2023, "The Covenant of Water"),
            seeded("seed-current-08", "The Final Problem", "Arturo Perez-Reverte", "Mystery", 2023, "El problema final")
        )
    }

    private fun defaultTopRatedBooks(languageTag: String): List<Libro> {
        if (languageTag == "es") {
            return listOf(
                seeded("seed-rated-01", "El nombre del viento", "Patrick Rothfuss", "Fantasia", 2007, "El nombre del viento", "QeYF9kTMypgC", 4.5),
                seeded("seed-rated-02", "Proyecto Hail Mary", "Andy Weir", "Ciencia ficcion", 2021, "Proyecto Hail Mary", "t1slEAAAQBAJ", 4.5),
                seeded("seed-rated-03", "El camino de los reyes", "Brandon Sanderson", "Fantasia", 2010, "El camino de los reyes", "LcqipwAACAAJ", 4.6),
                seeded("seed-rated-04", "El hobbit", "J. R. R. Tolkien", "Fantasia", 1937, "The Hobbit", rating = 4.3),
                seeded("seed-rated-05", "Dune", "Frank Herbert", "Ciencia ficcion", 1965, "Dune", rating = 4.2),
                seeded("seed-rated-06", "Pachinko", "Min Jin Lee", "Historica", 2017, "Pachinko", rating = 4.3),
                seeded("seed-rated-07", "Circe", "Madeline Miller", "Fantasia", 2018, "Circe", rating = 4.2),
                seeded("seed-rated-08", "La ladrona de libros", "Markus Zusak", "Historica", 2005, "The Book Thief", rating = 4.4)
            )
        }

        return listOf(
            seeded("seed-rated-01", "The Name of the Wind", "Patrick Rothfuss", "Fantasy", 2007, "The Name of the Wind", "TG5DXNXv2tAC", 4.5),
            seeded("seed-rated-02", "Project Hail Mary", "Andy Weir", "Science fiction", 2021, "Project Hail Mary", "GrYsEAAAQBAJ", 4.5),
            seeded("seed-rated-03", "The Way of Kings", "Brandon Sanderson", "Fantasy", 2010, "The Way of Kings", rating = 4.6),
            seeded("seed-rated-04", "The Hobbit", "J. R. R. Tolkien", "Fantasy", 1937, "The Hobbit", rating = 4.3),
            seeded("seed-rated-05", "Dune", "Frank Herbert", "Science fiction", 1965, "Dune", rating = 4.2),
            seeded("seed-rated-06", "Pachinko", "Min Jin Lee", "Historical fiction", 2017, "Pachinko", rating = 4.3),
            seeded("seed-rated-07", "Circe", "Madeline Miller", "Fantasy", 2018, "Circe", rating = 4.2),
            seeded("seed-rated-08", "The Book Thief", "Markus Zusak", "Historical fiction", 2005, "The Book Thief", rating = 4.4)
        )
    }

    private fun defaultKnownAuthorBooks(languageTag: String): List<Libro> {
        if (languageTag == "es") {
            return listOf(
                seeded("seed-author-01", "El resplandor", "Stephen King", "Terror", 1977, "The Shining"),
                seeded("seed-author-02", "Asesinato en el Orient Express", "Agatha Christie", "Misterio", 1934, "Murder on the Orient Express"),
                seeded("seed-author-03", "Kafka en la orilla", "Haruki Murakami", "Ficcion", 2002, "Kafka on the Shore"),
                seeded("seed-author-04", "La casa de los espiritus", "Isabel Allende", "Realismo magico", 1982, "La casa de los espiritus"),
                seeded("seed-author-05", "La mano izquierda de la oscuridad", "Ursula K. Le Guin", "Ciencia ficcion", 1969, "The Left Hand of Darkness"),
                seeded("seed-author-06", "American Gods", "Neil Gaiman", "Fantasia", 2001, "American Gods"),
                seeded("seed-author-07", "Cien anos de soledad", "Gabriel Garcia Marquez", "Realismo magico", 1967, "Cien anos de soledad"),
                seeded("seed-author-08", "El juego del angel", "Carlos Ruiz Zafon", "Misterio", 2008, "El juego del angel")
            )
        }

        return listOf(
            seeded("seed-author-01", "The Shining", "Stephen King", "Horror", 1977, "The Shining"),
            seeded("seed-author-02", "Murder on the Orient Express", "Agatha Christie", "Mystery", 1934, "Murder on the Orient Express"),
            seeded("seed-author-03", "Kafka on the Shore", "Haruki Murakami", "Fiction", 2002, "Kafka on the Shore"),
            seeded("seed-author-04", "The House of the Spirits", "Isabel Allende", "Magical realism", 1982, "The House of the Spirits"),
            seeded("seed-author-05", "The Left Hand of Darkness", "Ursula K. Le Guin", "Science fiction", 1969, "The Left Hand of Darkness"),
            seeded("seed-author-06", "American Gods", "Neil Gaiman", "Fantasy", 2001, "American Gods"),
            seeded("seed-author-07", "One Hundred Years of Solitude", "Gabriel Garcia Marquez", "Magical realism", 1967, "One Hundred Years of Solitude"),
            seeded("seed-author-08", "The Angel's Game", "Carlos Ruiz Zafon", "Mystery", 2008, "The Angel's Game")
        )
    }

    private fun seeded(
        id: String,
        title: String,
        author: String,
        genre: String,
        year: Int,
        coverTitle: String,
        googleVolumeId: String = "",
        rating: Double = 0.0
    ): Libro {
        return Libro(
            id = id,
            titulo = title,
            autor = author,
            genero = genre,
            fechaPublicacion = year,
            averageRating = rating,
            ratingsCount = if (rating > 0.0) 1000 else 0,
            imagen = googleVolumeId
                .takeIf(String::isNotBlank)
                ?.let(::googleCover)
                ?: coverByTitle(coverTitle)
        )
    }

    private fun googleCover(volumeId: String): String {
        return "https://books.google.com/books/content?id=$volumeId&printsec=frontcover&img=1&zoom=1&source=gbs_api"
    }

    private fun coverByTitle(title: String): String {
        return "https://covers.openlibrary.org/b/title/${title.urlEncoded()}-L.jpg?default=false"
    }

    private fun String.urlEncoded(): String {
        return replace(" ", "%20")
            .replace(",", "%2C")
            .replace("&", "%26")
            .replace("'", "%27")
    }
}
