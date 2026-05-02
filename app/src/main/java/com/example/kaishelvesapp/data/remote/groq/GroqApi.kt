package com.example.kaishelvesapp.data.remote.groq

import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: GroqChatCompletionRequest
    ): GroqChatCompletionResponse
}
