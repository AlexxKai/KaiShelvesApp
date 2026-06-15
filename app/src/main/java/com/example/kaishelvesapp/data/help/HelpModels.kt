package com.example.kaishelvesapp.data.help

import com.example.kaishelvesapp.ui.language.LanguageManager

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
    fun asPromptText(languageTag: String = LanguageManager.getCurrentLanguage()): String {
        val spanish = languageTag == "es"
        return buildString {
            appendLine("${if (spanish) "Pantalla actual" else "Current screen"}: $screenName")
            appendLine("${if (spanish) "Ruta" else "Route"}: $route")
            appendLine("${if (spanish) "Contexto" else "Context"}: $description")
            appendLine(if (spanish) "Acciones visibles o relevantes:" else "Visible or relevant actions:")
            availableActions.forEach { appendLine("- $it") }
        }
    }
}

data class HelpBotStructuredAnswer(
    val respuesta: String,
    val confidence: Float,
    val suggestedAction: String? = null
)
