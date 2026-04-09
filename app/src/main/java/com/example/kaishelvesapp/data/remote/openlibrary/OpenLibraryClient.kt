package com.example.kaishelvesapp.data.remote.openlibrary

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenLibraryClient {

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header(
                "User-Agent",
                "KaiShelvesApp/1.0 (alexurumart@gmail.com)"
            )
            .build()

        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: OpenLibraryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApi::class.java)
    }
}