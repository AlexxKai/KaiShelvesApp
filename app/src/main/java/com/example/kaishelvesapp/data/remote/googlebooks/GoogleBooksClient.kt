package com.example.kaishelvesapp.data.remote.googlebooks

import com.example.kaishelvesapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleBooksClient {

    private val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY.trim()

    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    private fun createQueryInterceptor(includeApiKey: Boolean) = Interceptor { chain ->
        val originalRequest = chain.request()
        val urlBuilder = originalRequest.url.newBuilder()

        if (includeApiKey && apiKey.isNotBlank()) {
            urlBuilder.addQueryParameter("key", apiKey)
        }

        val request = originalRequest.newBuilder()
            .url(urlBuilder.build())
            .header(
                "User-Agent",
                "KaiShelvesApp/1.0"
            )
            .build()

        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private fun createHttpClient(includeApiKey: Boolean): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(createQueryInterceptor(includeApiKey))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private fun createApi(includeApiKey: Boolean): GoogleBooksApi {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(createHttpClient(includeApiKey))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApi::class.java)
    }

    val api: GoogleBooksApi by lazy { createApi(includeApiKey = true) }
    val publicApi: GoogleBooksApi by lazy { createApi(includeApiKey = false) }
}
