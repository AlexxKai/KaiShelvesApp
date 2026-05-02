package com.example.kaishelvesapp.data.remote.groq

import com.google.gson.annotations.SerializedName

data class GroqChatCompletionRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<GroqMessage>,
    @SerializedName("temperature")
    val temperature: Double = 0.2,
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int = 500,
    @SerializedName("response_format")
    val responseFormat: GroqResponseFormat = GroqResponseFormat()
)

data class GroqMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class GroqResponseFormat(
    @SerializedName("type")
    val type: String = "json_object"
)

data class GroqChatCompletionResponse(
    @SerializedName("choices")
    val choices: List<GroqChoice> = emptyList()
)

data class GroqChoice(
    @SerializedName("message")
    val message: GroqMessage? = null
)
