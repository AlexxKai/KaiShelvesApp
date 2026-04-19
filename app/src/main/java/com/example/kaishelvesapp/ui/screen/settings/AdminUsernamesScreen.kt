package com.example.kaishelvesapp.ui.screen.settings

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.kaishelvesapp.data.repository.UsernameConflictGroup
import com.example.kaishelvesapp.data.repository.UsernameConflictUser
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AdminUsernamesViewModel

@Composable
fun AdminUsernamesScreen(
    viewModel: AdminUsernamesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadDuplicateGroups()
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
                title = stringResource(R.string.admin_usernames_title),
                subtitle = stringResource(R.string.admin_usernames_subtitle),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = stringResource(R.string.back),
                onNavigationClick = onBack,
                actionIcon = Icons.Filled.Refresh,
                actionIconContentDescription = stringResource(R.string.retry),
                onActionIconClick = viewModel::loadDuplicateGroups,
                centerTitle = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading && uiState.groups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TarnishedGold)
                    }
                }

                uiState.groups.isEmpty() -> {
                    EmptyAdminConflictsCard()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AdminSummaryCard(groupCount = uiState.groups.size)
                        }

                        items(
                            items = uiState.groups,
                            key = { it.normalizedUsername }
                        ) { group ->
                            ConflictGroupCard(
                                group = group,
                                draftUsernames = uiState.draftUsernames,
                                savingUserIds = uiState.savingUserIds,
                                onDraftUsernameChange = viewModel::onDraftUsernameChange,
                                onResolveUsername = viewModel::resolveUsername
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSummaryCard(groupCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_usernames_summary_title),
                style = MaterialTheme.typography.titleLarge,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.admin_usernames_summary_body, groupCount),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
        }
    }
}

@Composable
private fun ConflictGroupCard(
    group: UsernameConflictGroup,
    draftUsernames: Map<String, String>,
    savingUserIds: Set<String>,
    onDraftUsernameChange: (String, String) -> Unit,
    onResolveUsername: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = 0.985f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_conflict_group_title, group.users.firstOrNull()?.username.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
                color = DeepWalnut,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.admin_conflict_group_body),
                style = MaterialTheme.typography.bodySmall,
                color = DeepWalnut.copy(alpha = 0.74f)
            )

            group.users.forEach { user ->
                ConflictUserCard(
                    user = user,
                    draftUsername = draftUsernames[user.uid] ?: user.username,
                    isSaving = user.uid in savingUserIds,
                    onDraftUsernameChange = onDraftUsernameChange,
                    onResolveUsername = onResolveUsername
                )
            }
        }
    }
}

@Composable
private fun ConflictUserCard(
    user: UsernameConflictUser,
    draftUsername: String,
    isSaving: Boolean,
    onDraftUsernameChange: (String, String) -> Unit,
    onResolveUsername: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.22f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaiUserAvatar(
                    displayName = user.username.ifBlank { user.email },
                    imageUrl = user.photoUrl
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.username.ifBlank { stringResource(R.string.unknown_username) },
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepWalnut,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user.email.ifBlank { stringResource(R.string.no_email_available) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepWalnut.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "UID: ${user.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepWalnut.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedTextField(
                value = draftUsername,
                onValueChange = { onDraftUsernameChange(user.uid, it) },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
            )

            Button(
                onClick = { onResolveUsername(user.uid, user.username) },
                enabled = !isSaving && draftUsername.trim() != user.username.trim(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BloodWine,
                    contentColor = OldIvory,
                    disabledContainerColor = TarnishedGold.copy(alpha = 0.2f),
                    disabledContentColor = DeepWalnut.copy(alpha = 0.62f)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = OldIvory,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.admin_resolve_conflict))
                }
            }
        }
    }
}

@Composable
private fun EmptyAdminConflictsCard() {
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
                text = stringResource(R.string.admin_no_conflicts_title),
                style = MaterialTheme.typography.titleMedium,
                color = DeepWalnut
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.admin_no_conflicts_body),
                style = MaterialTheme.typography.bodyMedium,
                color = DeepWalnut.copy(alpha = 0.78f)
            )
        }
    }
}
