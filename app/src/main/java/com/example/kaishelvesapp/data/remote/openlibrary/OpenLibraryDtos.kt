package com.example.kaishelvesapp.data.remote.openlibrary

import com.google.gson.annotations.SerializedName

data class OpenLibrarySearchResponse(
    @SerializedName("numFound")
    val numFound: Int = 0,
    @SerializedName("docs")
    val docs: List<OpenLibrarySearchDoc> = emptyList()
)

data class OpenLibrarySearchDoc(
    @SerializedName("key")
    val key: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("author_name")
    val authorNames: List<String>? = null,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int? = null,
    @SerializedName("isbn")
    val isbn: List<String>? = null,
    @SerializedName("publisher")
    val publishers: List<String>? = null,
    @SerializedName("cover_i")
    val coverId: Int? = null,
    @SerializedName("subject")
    val subjects: List<String>? = null,
    @SerializedName("number_of_pages_median")
    val numberOfPagesMedian: Int? = null
)

data class OpenLibraryBookData(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("authors")
    val authors: List<OpenLibraryNamedValue>? = null,
    @SerializedName("publishers")
    val publishers: List<OpenLibraryNamedValue>? = null,
    @SerializedName("publish_date")
    val publishDate: String? = null,
    @SerializedName("number_of_pages")
    val numberOfPages: Int? = null,
    @SerializedName("cover")
    val cover: OpenLibraryCover? = null,
    @SerializedName("subjects")
    val subjects: List<OpenLibraryNamedValue>? = null,
    @SerializedName("url")
    val url: String? = null
)

data class OpenLibraryNamedValue(
    @SerializedName("name")
    val name: String? = null
)

data class OpenLibraryCover(
    @SerializedName("small")
    val small: String? = null,
    @SerializedName("medium")
    val medium: String? = null,
    @SerializedName("large")
    val large: String? = null
)
