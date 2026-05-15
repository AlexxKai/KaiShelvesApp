package com.example.kaishelvesapp.data.remote.inventaire

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface InventaireApi {

    @GET("api/entities")
    suspend fun getEntitiesByUris(
        @Query("action") action: String = "by-uris",
        @Query("uris") uris: String,
        @Query("refresh") refresh: Boolean = false
    ): JsonObject
}
