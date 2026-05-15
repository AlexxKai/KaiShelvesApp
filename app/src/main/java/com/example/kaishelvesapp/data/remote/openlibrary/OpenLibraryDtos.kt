package com.example.kaishelvesapp.data.remote.openlibrary

import com.google.gson.annotations.SerializedName

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
