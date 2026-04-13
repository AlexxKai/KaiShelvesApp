package com.example.kaishelvesapp.data.remote.googlebooks

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(
    @SerializedName("totalItems")
    val totalItems: Int = 0,
    @SerializedName("items")
    val items: List<GoogleBookItem> = emptyList()
)

data class GoogleBookItem(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("volumeInfo")
    val volumeInfo: GoogleBookVolumeInfo? = null,
    @SerializedName("accessInfo")
    val accessInfo: GoogleBookAccessInfo? = null
)

data class GoogleBookVolumeInfo(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("authors")
    val authors: List<String>? = null,
    @SerializedName("publisher")
    val publisher: String? = null,
    @SerializedName("publishedDate")
    val publishedDate: String? = null,
    @SerializedName("industryIdentifiers")
    val industryIdentifiers: List<GoogleIndustryIdentifier>? = null,
    @SerializedName("categories")
    val categories: List<String>? = null,
    @SerializedName("pageCount")
    val pageCount: Int? = null,
    @SerializedName("imageLinks")
    val imageLinks: GoogleBookImageLinks? = null
)

data class GoogleIndustryIdentifier(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("identifier")
    val identifier: String? = null
)

data class GoogleBookImageLinks(
    @SerializedName("smallThumbnail")
    val smallThumbnail: String? = null,
    @SerializedName("thumbnail")
    val thumbnail: String? = null
)

data class GoogleBookAccessInfo(
    @SerializedName("webReaderLink")
    val webReaderLink: String? = null
)
