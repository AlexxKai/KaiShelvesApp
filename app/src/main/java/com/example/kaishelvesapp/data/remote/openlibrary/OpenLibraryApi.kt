package com.example.kaishelvesapp.data.remote.openlibrary

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApi {

    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 40,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,isbn,publisher,cover_i,subject,number_of_pages_median"
    ): OpenLibrarySearchResponse

    @GET("api/books")
    suspend fun getBooksByBibkeys(
        @Query("bibkeys") bibkeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jscmd: String = "data"
    ): Map<String, OpenLibraryBookData>
}
