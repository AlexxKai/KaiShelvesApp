package com.example.kaishelvesapp.data.remote.openlibrary

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApi {

    @GET("api/books")
    suspend fun getBooksByBibkeys(
        @Query("bibkeys") bibkeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jscmd: String = "data"
    ): Map<String, OpenLibraryBookData>
}
