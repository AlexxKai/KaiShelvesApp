package com.example.kaishelvesapp.data.remote.inventaire

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object InventaireClient {

    private val httpClient = OkHttpClient.Builder()
        .build()

    val api: InventaireApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://inventaire.io/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InventaireApi::class.java)
    }
}
