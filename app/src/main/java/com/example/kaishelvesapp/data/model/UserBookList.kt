package com.example.kaishelvesapp.data.model

data class UserBookList(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val bookCount: Int = 0,
    val position: Int = 0,
    val previewImageUrls: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val systemKey: String = ""
)
