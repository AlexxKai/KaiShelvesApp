package com.example.kaishelvesapp.data.remote.openlibrary

import com.google.gson.annotations.SerializedName

data class OpenLibrarySearchResponse(
    @SerializedName("numFound")
    val numFound: Int = 0,
    @SerializedName("docs")
    val docs: List<OpenLibraryBookDoc> = emptyList()
)

data class OpenLibraryBookDoc(
    @SerializedName("key")
    val key: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("author_name")
    val authorNames: List<String>? = null,
    @SerializedName("publisher")
    val publishers: List<String>? = null,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int? = null,
    @SerializedName("isbn")
    val isbns: List<String>? = null,
    @SerializedName("cover_i")
    val coverId: Int? = null,
    @SerializedName("cover_edition_key")
    val coverEditionKey: String? = null,
    @SerializedName("subject")
    val subjects: List<String>? = null,
    @SerializedName("number_of_pages_median")
    val numberOfPagesMedian: Int? = null
)