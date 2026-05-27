package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.data.help.HelpChatMessage
import com.example.kaishelvesapp.data.help.HelpMessageAuthor
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.HelpChatUiState

@Composable
fun HelpChatOverlay(
    state: HelpChatUiState,
    onExpand: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    if (!state.isActive) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (state.isExpanded) {
            HelpChatPanel(
                state = state,
                onMinimize = onMinimize,
                onClose = onClose,
                onInputChange = onInputChange,
                onSend = onSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .widthIn(max = 520.dp)
            )
        } else {
            ExtendedFloatingActionButton(
                onClick = onExpand,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null
                    )
                },
                text = {
                    Text("Ayuda")
                },
                containerColor = BloodWine,
                contentColor = TarnishedGold
            )
        }
    }
}

@Composable
fun HelpChatPanel(
    state: HelpChatUiState,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isLoading) {
        val lastIndex = state.messages.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BloodWine.copy(alpha = 0.22f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ayuda KaiShelves",
                        style = MaterialTheme.typography.titleMedium,
                        color = TarnishedGold
                    )
                    Text(
                        text = state.screenContext.screenName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = OldIvory.copy(alpha = 0.78f)
                    )
                }

                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimizar chat",
                        tint = TarnishedGold
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar chat",
                        tint = TarnishedGold
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    HelpChatBubble(message = message)
                }

                if (state.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TarnishedGold
                            )
                            Text(
                                text = "Preparando guía...",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = OldIvory.copy(alpha = 0.82f)
                            )
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pregunta sobre la app") },
                    singleLine = true,
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )

                IconButton(
                    onClick = onSend,
                    enabled = state.input.isNotBlank() && !state.isLoading
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar pregunta",
                        tint = if (state.input.isNotBlank() && !state.isLoading) TarnishedGold else OldIvory.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpChatBubble(
    message: HelpChatMessage
) {
    val isUser = message.author == HelpMessageAuthor.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) BloodWine.copy(alpha = 0.88f) else DeepWalnut.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = if (isUser) 0.55f else 0.32f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )

                if (!message.suggestedAction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sugerencia: ${message.suggestedAction}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TarnishedGold
                    )
                }

                if (message.confidence != null) {
                    Text(
                        text = "Confianza ${(message.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OldIvory.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
