package com.example.kaishelvesapp.data.help

enum class HelpMessageAuthor {
    USER,
    ASSISTANT
}

data class HelpChatMessage(
    val id: Long,
    val author: HelpMessageAuthor,
    val text: String,
    val suggestedAction: String? = null,
    val confidence: Float? = null
)

data class HelpScreenContext(
    val route: String,
    val screenName: String,
    val description: String,
    val availableActions: List<String>
) {
    fun asPromptText(): String {
        return buildString {
            appendLine("Pantalla actual: $screenName")
            appendLine("Ruta: $route")
            appendLine("Contexto: $description")
            appendLine("Acciones visibles o relevantes:")
            availableActions.forEach { appendLine("- $it") }
        }
    }
}

data class HelpBotStructuredAnswer(
    val respuesta: String,
    val confidence: Float,
    val suggestedAction: String? = null
)
