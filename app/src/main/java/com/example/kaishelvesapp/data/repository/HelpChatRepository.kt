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
        val responseLanguage = detectResponseLanguage(userQuestion)
        if (!GroqClient.hasApiKey) {
            return Result.success(localFallback(userQuestion, screenContext, responseLanguage))
        }

        return runCatching {
            val response = GroqClient.api.createChatCompletion(
                GroqChatCompletionRequest(
                    model = GroqClient.model,
                    messages = buildMessages(userQuestion, screenContext, history, responseLanguage)
                )
            )
            parseStructuredAnswer(
                content = response.choices.firstOrNull()?.message?.content.orEmpty(),
                responseLanguage = responseLanguage
            )
        }.recoverCatching {
            localFallback(userQuestion, screenContext, responseLanguage)
        }
    }

    private fun buildMessages(
        userQuestion: String,
        screenContext: HelpScreenContext,
        history: List<HelpChatMessage>,
        responseLanguage: HelpResponseLanguage
    ): List<GroqMessage> {
        val instructions = when (responseLanguage) {
            HelpResponseLanguage.SPANISH -> """
                Eres el asistente de ayuda integrado de Kai Shelves.
                Responde únicamente sobre el uso de la aplicación. Usa el contexto de pantalla y la base de conocimiento.
                Responde en español porque la pregunta del usuario está en español.
                No inventes funciones. Devuelve un único JSON válido, sin Markdown, con estas claves:
                {"respuesta":"texto breve y claro en español","confidence":0.0,"suggestedAction":"acción concreta o null"}
            """.trimIndent()
            HelpResponseLanguage.ENGLISH -> """
                You are Kai Shelves' built-in help assistant.
                Answer only about using the app. Use the screen context and knowledge base.
                Answer in English because the user's question is in English.
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
        val languageTag = responseLanguage.languageTag

        return listOf(
            GroqMessage(
                role = "system",
                content = """
                    $instructions

                    Knowledge base / Base de conocimiento:
                    ${HelpKnowledgeBase.asPromptText(languageTag)}

                    Screen context / Contexto de pantalla:
                    ${screenContext.asPromptText(languageTag)}
                """.trimIndent()
            )
        ) + recentHistory + GroqMessage(role = "user", content = userQuestion)
    }

    private fun parseStructuredAnswer(
        content: String,
        responseLanguage: HelpResponseLanguage
    ): HelpBotStructuredAnswer {
        val parsed = gson.fromJson(content, HelpBotStructuredAnswer::class.java)
        val fallback = when (responseLanguage) {
            HelpResponseLanguage.SPANISH -> "No he podido preparar una respuesta clara. Reformula la pregunta sobre esta pantalla."
            HelpResponseLanguage.ENGLISH -> "I could not prepare a clear answer. Try rephrasing your question about this screen."
        }
        return parsed.copy(
            respuesta = parsed.respuesta.ifBlank { fallback },
            confidence = parsed.confidence.coerceIn(0f, 1f),
            suggestedAction = parsed.suggestedAction?.takeIf { it.isNotBlank() }
        )
    }

    private fun localFallback(
        userQuestion: String,
        screenContext: HelpScreenContext,
        responseLanguage: HelpResponseLanguage
    ): HelpBotStructuredAnswer {
        val normalized = userQuestion.lowercase()
        val action = when (responseLanguage) {
            HelpResponseLanguage.SPANISH -> when {
                "isbn" in normalized || "escane" in normalized || "cámara" in normalized || "camara" in normalized -> {
                    "Pulsa el icono de cámara de la barra superior y concede el permiso si Android lo solicita."
                }
                "buscar" in normalized || "catálogo" in normalized || "catalogo" in normalized || "libro" in normalized -> {
                    "Usa la barra superior para buscar por título, autor o ISBN."
                }
                "perfil" in normalized || "privacidad" in normalized || "ajustes" in normalized || "idioma" in normalized -> {
                    "Ve a Perfil, entra en Ajustes y selecciona Idioma."
                }
                "amigo" in normalized || "solicitud" in normalized || "notificación" in normalized || "notificacion" in normalized -> {
                    "Pulsa la campana superior o entra en Amigos desde el menú lateral."
                }
                else -> localizedFirstAction(screenContext, responseLanguage)
            }
            HelpResponseLanguage.ENGLISH -> when {
                "isbn" in normalized || "scan" in normalized || "camera" in normalized -> {
                    "Tap the camera icon in the top bar and grant permission if Android requests it."
                }
                "search" in normalized || "catalog" in normalized || "book" in normalized -> {
                    "Use the top bar to search by title, author, or ISBN."
                }
                "profile" in normalized || "privacy" in normalized || "settings" in normalized || "language" in normalized -> {
                    "Go to Profile, open Settings, and select Language."
                }
                "friend" in normalized || "request" in normalized || "notification" in normalized -> {
                    "Tap the bell in the top bar or open Friends from the side menu."
                }
                else -> localizedFirstAction(screenContext, responseLanguage)
            }
        }

        return HelpBotStructuredAnswer(
            respuesta = when (responseLanguage) {
                HelpResponseLanguage.SPANISH -> "Puedo ayudarte a usar Kai Shelves. Estás en ${screenContext.screenName}."
                HelpResponseLanguage.ENGLISH -> "I can help you use Kai Shelves. You are currently on ${screenContext.screenName}."
            },
            confidence = 0.62f,
            suggestedAction = action
        )
    }

    private fun localizedFirstAction(
        screenContext: HelpScreenContext,
        responseLanguage: HelpResponseLanguage
    ): String? {
        return screenContext.availableActions.firstOrNull()?.takeIf {
            LanguageManager.getCurrentLanguage() == responseLanguage.languageTag
        } ?: when (responseLanguage) {
            HelpResponseLanguage.SPANISH -> "Describe qué quieres hacer en esta pantalla."
            HelpResponseLanguage.ENGLISH -> "Describe what you want to do on this screen."
        }
    }

    private fun detectResponseLanguage(userQuestion: String): HelpResponseLanguage {
        val normalized = userQuestion.lowercase()
        val spanishScore = spanishMarkers.count { it in normalized } +
            if (normalized.any { it in "áéíóúñ¿¡" }) 2 else 0
        val englishScore = englishMarkers.count { it in normalized }

        return when {
            englishScore > spanishScore -> HelpResponseLanguage.ENGLISH
            spanishScore > englishScore -> HelpResponseLanguage.SPANISH
            LanguageManager.getCurrentLanguage() == "es" -> HelpResponseLanguage.SPANISH
            else -> HelpResponseLanguage.ENGLISH
        }
    }

    private enum class HelpResponseLanguage(val languageTag: String) {
        SPANISH("es"),
        ENGLISH("en")
    }

    private companion object {
        val spanishMarkers = listOf(
            "como", "cómo", "donde", "dónde", "puedo", "quiero", "poner", "cambiar", "configurar",
            "español", "idioma", "perfil", "ajustes", "ayuda", "libro", "buscar", "leer", "leído",
            "leido", "lista", "biblioteca", "notificación", "notificacion", "solicitud", "amigo"
        )
        val englishMarkers = listOf(
            "how", "where", "what", "can", "set", "change", "configure", "english", "language", "app",
            "profile", "settings", "help", "book", "search", "read", "list", "library", "notification",
            "request", "friend", "understand"
        )
    }
}
