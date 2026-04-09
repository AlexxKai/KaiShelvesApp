package com.example.kaishelvesapp.data.remote.openlibrary

data class LibraryGenre(
    val label: String,
    val subjectQuery: String
)

object LibraryGenres {
    val all = listOf(
        LibraryGenre("Fantasía", "fantasy"),
        LibraryGenre("Misterio", "mystery"),
        LibraryGenre("Terror", "horror"),
        LibraryGenre("Romance", "romance"),
        LibraryGenre("Ciencia ficción", "science fiction"),
        LibraryGenre("Historia", "history"),
        LibraryGenre("Aventura", "adventure"),
        LibraryGenre("Poesía", "poetry")
    )
}