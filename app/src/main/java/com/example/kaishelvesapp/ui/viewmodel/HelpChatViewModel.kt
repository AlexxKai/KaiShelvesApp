package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.help.HelpBotStructuredAnswer
import com.example.kaishelvesapp.data.help.HelpChatMessage
import com.example.kaishelvesapp.data.help.HelpMessageAuthor
import com.example.kaishelvesapp.data.help.HelpScreenContext
import com.example.kaishelvesapp.data.repository.HelpChatRepository
import com.example.kaishelvesapp.ui.language.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HelpChatUiState(
    val isActive: Boolean = false,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val input: String = "",
    val messages: List<HelpChatMessage> = emptyList(),
    val screenContext: HelpScreenContext = defaultHelpScreenContext(),
    val errorMessage: String? = null
)

class HelpChatViewModel(
    private val repository: HelpChatRepository = HelpChatRepository()
) : ViewModel() {

    private var nextMessageId = 1L

    private val _uiState = MutableStateFlow(HelpChatUiState())
    val uiState: StateFlow<HelpChatUiState> = _uiState.asStateFlow()

    fun updateScreenContext(context: HelpScreenContext) {
        _uiState.update { it.copy(screenContext = context) }
    }

    fun startChat() {
        _uiState.update { state ->
            val initialMessages = if (state.messages.isEmpty()) {
                listOf(
                    HelpChatMessage(
                        id = nextMessageId++,
                        author = HelpMessageAuthor.ASSISTANT,
                        text = localizedInitialMessage(),
                        suggestedAction = state.screenContext.availableActions.firstOrNull(),
                        confidence = 1f
                    )
                )
            } else {
                state.messages
            }

            state.copy(
                isActive = true,
                isExpanded = true,
                messages = initialMessages,
                errorMessage = null
            )
        }
    }

    fun minimizeChat() {
        _uiState.update { it.copy(isExpanded = false) }
    }

    fun expandChat() {
        _uiState.update { it.copy(isActive = true, isExpanded = true) }
    }

    fun closeChat() {
        _uiState.update {
            HelpChatUiState(
                screenContext = it.screenContext
            )
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val question = state.input.trim()
        if (question.isBlank() || state.isLoading) return

        val userMessage = HelpChatMessage(
            id = nextMessageId++,
            author = HelpMessageAuthor.USER,
            text = question
        )
        val historyForRequest = state.messages + userMessage

        _uiState.update {
            it.copy(
                input = "",
                isLoading = true,
                messages = historyForRequest,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = repository.ask(
                userQuestion = question,
                screenContext = _uiState.value.screenContext,
                history = historyForRequest
            )

            result
                .onSuccess { answer -> appendAssistantAnswer(answer) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: localizedConnectionError()
                        )
                    }
                }
        }
    }

    private fun appendAssistantAnswer(answer: HelpBotStructuredAnswer) {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                messages = state.messages + HelpChatMessage(
                    id = nextMessageId++,
                    author = HelpMessageAuthor.ASSISTANT,
                    text = answer.respuesta,
                    suggestedAction = answer.suggestedAction,
                    confidence = answer.confidence
                ),
                errorMessage = null
            )
        }
    }
}

fun defaultHelpScreenContext(): HelpScreenContext {
    val spanish = LanguageManager.getCurrentLanguage() == "es"
    return HelpScreenContext(
        route = "unknown",
        screenName = "KaiShelves",
        description = if (spanish) {
            "Pantalla general de la aplicación."
        } else {
            "General app screen."
        },
        availableActions = listOf(
            if (spanish) {
                "Abre el menú lateral para cambiar de sección."
            } else {
                "Open the side menu to change sections."
            }
        )
    )
}

private fun localizedInitialMessage(): String {
    return if (LanguageManager.getCurrentLanguage() == "es") {
        "Hola, soy la ayuda de Kai Shelves. Dime qué necesitas hacer en esta pantalla y te guío paso a paso."
    } else {
        "Hi, I'm Kai Shelves Help. Tell me what you need on this screen and I'll guide you step by step."
    }
}

private fun localizedConnectionError(): String {
    return if (LanguageManager.getCurrentLanguage() == "es") {
        "No se pudo conectar con la ayuda."
    } else {
        "Could not connect to Help."
    }
}
