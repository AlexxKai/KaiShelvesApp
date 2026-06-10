package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.help.HelpBotStructuredAnswer
import com.example.kaishelvesapp.data.help.HelpChatMessage
import com.example.kaishelvesapp.data.help.HelpKnowledgeBase
import com.example.kaishelvesapp.data.help.HelpMessageAuthor
import com.example.kaishelvesapp.data.help.HelpScreenContext
import com.example.kaishelvesapp.data.remote.groq.GroqChatCompletionRequest
import com.example.kaishelvesapp.data.remote.groq.GroqClient
import com.example.kaishelvesapp.data.remote.groq.GroqMessage
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.google.gson.Gson

class HelpChatRepository(
    private val gson: Gson = Gson()
) {
    suspend fun ask(
        userQuestion: String,
        screenContext: HelpScreenContext,
        history: List<HelpChatMessage>
    ): Result<HelpBotStructuredAnswer> {
        if (!GroqClient.hasApiKey) {
            return Result.success(localFallback(userQuestion, screenContext))
        }

        return runCatching {
            val response = GroqClient.api.createChatCompletion(
                GroqChatCompletionRequest(
                    model = GroqClient.model,
                    messages = buildMessages(userQuestion, screenContext, history)
                )
            )
            parseStructuredAnswer(response.choices.firstOrNull()?.message?.content.orEmpty())
        }.recoverCatching {
            localFallback(userQuestion, screenContext)
        }
    }

    private fun buildMessages(
        userQuestion: String,
        screenContext: HelpScreenContext,
        history: List<HelpChatMessage>
    ): List<GroqMessage> {
        val spanish = LanguageManager.getCurrentLanguage() == "es"
        val instructions = if (spanish) {
            """
                Eres el asistente de ayuda integrado de Kai Shelves.
                Responde únicamente sobre el uso de la aplicación. Usa el contexto de pantalla y la base de conocimiento.
                No inventes funciones. Devuelve un único JSON válido, sin Markdown, con estas claves:
                {"respuesta":"texto breve y claro en español","confidence":0.0,"suggestedAction":"acción concreta o null"}
            """.trimIndent()
        } else {
            """
                You are Kai Shelves' built-in help assistant.
                Answer only about using the app. Use the screen context and knowledge base.
                Do not invent features. Return one valid JSON object without Markdown using these keys:
                {"respuesta":"brief and clear English text","confidence":0.0,"suggestedAction":"specific action or null"}
            """.trimIndent()
        }
        val recentHistory = history.takeLast(8).map { message ->
            GroqMessage(
                role = if (message.author == HelpMessageAuthor.USER) "user" else "assistant",
                content = message.text
            )
        }

        return listOf(
            GroqMessage(
                role = "system",
                content = """
                    $instructions

                    Knowledge base / Base de conocimiento:
                    ${HelpKnowledgeBase.asPromptText()}

                    Screen context / Contexto de pantalla:
                    ${screenContext.asPromptText()}
                """.trimIndent()
            )
        ) + recentHistory + GroqMessage(role = "user", content = userQuestion)
    }

    private fun parseStructuredAnswer(content: String): HelpBotStructuredAnswer {
        val parsed = gson.fromJson(content, HelpBotStructuredAnswer::class.java)
        val fallback = if (LanguageManager.getCurrentLanguage() == "es") {
            "No he podido preparar una respuesta clara. Reformula la pregunta sobre esta pantalla."
        } else {
            "I could not prepare a clear answer. Try rephrasing your question about this screen."
        }
        return parsed.copy(
            respuesta = parsed.respuesta.ifBlank { fallback },
            confidence = parsed.confidence.coerceIn(0f, 1f),
            suggestedAction = parsed.suggestedAction?.takeIf { it.isNotBlank() }
        )
    }

    private fun localFallback(
        userQuestion: String,
        screenContext: HelpScreenContext
    ): HelpBotStructuredAnswer {
        val normalized = userQuestion.lowercase()
        val spanish = LanguageManager.getCurrentLanguage() == "es"
        val action = if (spanish) {
            when {
                "isbn" in normalized || "escane" in normalized || "camara" in normalized -> "Pulsa el icono de cámara de la barra superior y concede el permiso si Android lo solicita."
                "buscar" in normalized || "catalogo" in normalized || "libro" in normalized -> "Usa la barra superior para buscar por título, autor o ISBN."
                "perfil" in normalized || "privacidad" in normalized || "ajustes" in normalized -> "Abre el menú lateral, entra en Perfil y revisa la configuración de privacidad."
                "amigo" in normalized || "solicitud" in normalized || "notificacion" in normalized -> "Pulsa la campana superior o entra en Amigos desde el menú lateral."
                else -> screenContext.availableActions.firstOrNull()
            }
        } else {
            when {
                "isbn" in normalized || "scan" in normalized || "camera" in normalized -> "Tap the camera icon in the top bar and grant permission if Android requests it."
                "search" in normalized || "catalog" in normalized || "book" in normalized -> "Use the top bar to search by title, author, or ISBN."
                "profile" in normalized || "privacy" in normalized || "settings" in normalized -> "Open the side menu, go to Profile, and review your privacy settings."
                "friend" in normalized || "request" in normalized || "notification" in normalized -> "Tap the bell in the top bar or open Friends from the side menu."
                else -> screenContext.availableActions.firstOrNull()
            }
        }

        return HelpBotStructuredAnswer(
            respuesta = if (spanish) {
                "Puedo ayudarte a usar Kai Shelves. Ahora estás en ${screenContext.screenName}: ${screenContext.description}"
            } else {
                "I can help you use Kai Shelves. You are currently on ${screenContext.screenName}: ${screenContext.description}"
            },
            confidence = 0.62f,
            suggestedAction = action
        )
    }
}
