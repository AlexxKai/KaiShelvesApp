package com.example.kaishelvesapp.data.model

data class UserBookTagSummary(
    val tag: UserBookTag = UserBookTag(),
    val bookCount: Int = 0,
    val previewImageUrls: List<String> = emptyList()
)
