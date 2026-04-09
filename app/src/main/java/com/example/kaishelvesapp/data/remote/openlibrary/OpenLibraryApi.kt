package com.example.kaishelvesapp.data.remote.openlibrary

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApi {

    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 40,
        @Query("page") page: Int = 1
    ): OpenLibrarySearchResponse
}