package com.example.kaishelvesapp.data.remote.googlebooks

data class LibraryGenre(
    val label: String,
    val subjectQuery: String
)

object LibraryGenres {
    val all = listOf(
        LibraryGenre("Fantasia", "fantasy"),
        LibraryGenre("Misterio", "mystery"),
        LibraryGenre("Terror", "horror"),
        LibraryGenre("Romance", "romance"),
        LibraryGenre("Ciencia ficcion", "science fiction"),
        LibraryGenre("Historia", "history"),
        LibraryGenre("Aventura", "adventure"),
        LibraryGenre("Poesia", "poetry")
    )
}
