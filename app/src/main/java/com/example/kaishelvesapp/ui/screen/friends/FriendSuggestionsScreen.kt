package com.example.kaishelvesapp.ui.screen.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.FriendSuggestion
import com.example.kaishelvesapp.data.repository.SuggestionSource
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.FriendSuggestionsViewModel

@Composable
fun FriendSuggestionsScreen(
    viewModel: FriendSuggestionsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadSuggestions()
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            KaiTopBar(
                title = stringResource(R.string.add_friend),
                subtitle = stringResource(R.string.friend_suggestions_subtitle),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = stringResource(R.string.back),
                onNavigationClick = onBack,
                centerTitle = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            SuggestionSearchField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TarnishedGold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.filteredSuggestions.isEmpty()) {
                        item {
                            EmptySuggestionsCard()
                        }
                    } else {
                        items(
                            items = uiState.filteredSuggestions,
                            key = { it.user.uid }
                        ) { suggestion ->
                            SuggestionCard(
                                suggestion = suggestion,
                                isRequestSent = suggestion.user.uid in uiState.sentRequestIds,
                                onSendRequest = viewModel::sendFriendRequest
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.search_users_or_email),
                color = DeepWalnut.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = DeepWalnut.copy(alpha = 0.7f)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = DeepWalnut.copy(alpha = 0.7f)
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OldIvory,
            unfocusedContainerColor = OldIvory,
            focusedBorderColor = TarnishedGold,
            unfocusedBorderColor = TarnishedGold.copy(alpha = 0.45f),
            focusedTextColor = DeepWalnut,
            unfocusedTextColor = DeepWalnut,
            cursorColor = BloodWine
        )
    )
}

@Composable
private fun SuggestionCard(
    suggestion: FriendSuggestion,
    isRequestSent: Boolean,
    onSendRequest: (Usuario) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = 0.985f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KaiUserAvatar(
                displayName = suggestion.user.usuario.ifBlank { suggestion.user.email },
                imageUrl = suggestion.user.photoUrl
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = suggestion.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepWalnut,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = suggestion.user.email.ifBlank { stringResource(R.string.no_email_available) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepWalnut.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (suggestion.source) {
                        SuggestionSource.FRIEND_OF_FRIEND -> stringResource(R.string.friend_of_friends)
                        SuggestionSource.RANDOM -> stringResource(R.string.suggested_user)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BloodWine
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Button(
                onClick = { onSendRequest(suggestion.user) },
                enabled = !isRequestSent,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BloodWine,
                    contentColor = OldIvory,
                    disabledContainerColor = TarnishedGold.copy(alpha = 0.24f),
                    disabledContentColor = DeepWalnut.copy(alpha = 0.66f)
                ),
                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (isRequestSent) stringResource(R.string.request_sent) else stringResource(R.string.add_friend))
            }
        }
    }
}

@Composable
private fun EmptySuggestionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.no_matching_users),
                style = MaterialTheme.typography.titleMedium,
                color = DeepWalnut
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.try_another_user_search),
                style = MaterialTheme.typography.bodyMedium,
                color = DeepWalnut.copy(alpha = 0.78f)
            )
        }
    }
}
