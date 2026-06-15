package com.example.kaishelvesapp.data.model

data class ReadingStatsSnapshot(
    val totalBooksInLists: Int = 0,
    val totalReadBooks: Int = 0,
    val averageRating: Double? = null,
    val favoriteGenre: String = "",
    val totalPages: Int = 0,
    val updatedAtMillis: Long = 0L
)
