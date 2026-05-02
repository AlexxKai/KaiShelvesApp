package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.help.HelpBotStructuredAnswer
import com.example.kaishelvesapp.data.help.HelpChatMessage
import com.example.kaishelvesapp.data.help.HelpKnowledgeBase
import com.example.kaishelvesapp.data.help.HelpMessageAuthor
import com.example.kaishelvesapp.data.help.HelpScreenContext
import com.example.kaishelvesapp.data.remote.groq.GroqChatCompletionRequest
import com.example.kaishelvesapp.data.remote.groq.GroqClient
import com.example.kaishelvesapp.data.remote.groq.GroqMessage
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
            val content = response.choices.firstOrNull()?.message?.content.orEmpty()
            parseStructuredAnswer(content)
        }.recoverCatching {
            localFallback(userQuestion, screenContext)
        }
    }

    private fun buildMessages(
        userQuestion: String,
        screenContext: HelpScreenContext,
        history: List<HelpChatMessage>
    ): List<GroqMessage> {
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
                    Eres el asistente de ayuda integrado de KaiShelves, una app Kotlin/Jetpack Compose para gestionar libros, seguir lecturas, descubrir libros y leer desde el entorno de la app.
                    Responde solo sobre el uso de KaiShelves. Si el usuario pregunta algo fuera de la aplicacion, redirige amablemente a una duda sobre la app.
                    Usa siempre el contexto de pantalla actual y la base de conocimiento. No inventes funciones que no esten descritas.
                    Devuelve siempre un unico JSON valido, sin markdown, con estas claves:
                    {"respuesta":"texto breve y claro en espanol","confidence":0.0,"suggestedAction":"accion concreta o null"}

                    Base de conocimiento:
                    ${HelpKnowledgeBase.asPromptText()}

                    Contexto de pantalla:
                    ${screenContext.asPromptText()}
                """.trimIndent()
            )
        ) + recentHistory + GroqMessage(
            role = "user",
            content = userQuestion
        )
    }

    private fun parseStructuredAnswer(content: String): HelpBotStructuredAnswer {
        val parsed = gson.fromJson(content, HelpBotStructuredAnswer::class.java)
        return parsed.copy(
            respuesta = parsed.respuesta.ifBlank { "No he podido preparar una respuesta clara. Prueba a reformular la duda sobre esta pantalla." },
            confidence = parsed.confidence.coerceIn(0f, 1f),
            suggestedAction = parsed.suggestedAction?.takeIf { it.isNotBlank() }
        )
    }

    private fun localFallback(
        userQuestion: String,
        screenContext: HelpScreenContext
    ): HelpBotStructuredAnswer {
        val normalized = userQuestion.lowercase()
        val action = when {
            "isbn" in normalized || "escane" in normalized || "camara" in normalized -> "Pulsa el icono de camara de la barra superior y concede permiso si Android lo pide."
            "buscar" in normalized || "catalogo" in normalized || "libro" in normalized -> "Usa la barra superior para buscar por titulo, autor o ISBN."
            "perfil" in normalized || "privacidad" in normalized || "ajustes" in normalized -> "Abre el menu lateral, entra en Perfil y revisa la configuracion de privacidad."
            "amigo" in normalized || "solicitud" in normalized || "notificacion" in normalized -> "Pulsa la campana superior o entra en Amigos desde el menu lateral."
            else -> screenContext.availableActions.firstOrNull()
        }

        return HelpBotStructuredAnswer(
            respuesta = "Puedo ayudarte con el uso de KaiShelves. Ahora estas en ${screenContext.screenName}: ${screenContext.description}",
            confidence = 0.62f,
            suggestedAction = action
        )
    }
}
