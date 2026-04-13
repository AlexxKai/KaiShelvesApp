package com.example.kaishelvesapp.data.remote.googlebooks

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {

    @GET("books/v1/volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 40,
        @Query("startIndex") startIndex: Int = 0,
        @Query("printType") printType: String = "books",
        @Query("langRestrict") langRestrict: String? = null
    ): GoogleBooksResponse
}
