package com.example.kaishelvesapp.data.model

data class LibroLeido(
    val isbn: String = "",
    val titulo: String = "",
    val autor: String = "",
    val editorial: String = "",
    val genero: String = "",
    val fechaPublicacion: Int = 0,
    val paginas: Int = 0,
    val imagen: String = "",
    val pdf: String = "",
    val fechaLeido: String = "",
    val puntuacion: Int = 0,
    val resena: String = "",
    val contieneSpoilers: Boolean = false,
    val siNo: String = "si"
)
