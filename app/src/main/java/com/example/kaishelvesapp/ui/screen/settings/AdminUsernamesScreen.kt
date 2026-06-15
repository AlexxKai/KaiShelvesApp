package com.example.kaishelvesapp.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.kaishelvesapp.data.repository.UsernameRegistryStatus
import com.example.kaishelvesapp.data.repository.UsernameRegistryUser
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AdminUsernameRegistryFilter
import com.example.kaishelvesapp.ui.viewmodel.AdminUsernamesPanelMode
import com.example.kaishelvesapp.ui.viewmodel.AdminUsernamesViewModel
import com.example.kaishelvesapp.ui.viewmodel.matchesAdminUsernameFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsernamesScreen(
    viewModel: AdminUsernamesViewModel,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadAll()
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
        PullToRefreshBox(
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::loadAll
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AdminUsernamesHeader(
                    selectedMode = uiState.selectedMode,
                    onBack = onBack,
                    onModeChange = viewModel::onModeChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (uiState.selectedMode) {
                    AdminUsernamesPanelMode.REGISTRY -> {
                        UsernameRegistryContent(
                            users = uiState.users.filter {
                                it.matchesAdminUsernameFilter(uiState.selectedFilter, uiState.showNotifiedOnly)
                            },
                            selectedFilter = uiState.selectedFilter,
                            showNotifiedOnly = uiState.showNotifiedOnly,
                            isLoading = uiState.isLoading,
                            savingUserIds = uiState.savingUserIds,
                            onFilterChange = viewModel::onFilterChange,
                            onToggleNotifiedOnly = viewModel::toggleNotifiedOnly,
                            onMarkReviewed = viewModel::markUsernameReviewed,
                            onRequestChange = viewModel::requestUsernameChange,
                            onOpenUserProfile = onOpenUserProfile
                        )
                    }

                    AdminUsernamesPanelMode.DUPLICATES -> {
                        DuplicateUsernamesContent(
                            groups = uiState.groups,
                            isLoading = uiState.isLoading,
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

@Composable
private fun AdminUsernamesHeader(
    selectedMode: AdminUsernamesPanelMode,
    onBack: () -> Unit,
    onModeChange: (AdminUsernamesPanelMode) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = TarnishedGold
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedMode) {
                        AdminUsernamesPanelMode.REGISTRY -> stringResource(R.string.admin_username_registry_title)
                        AdminUsernamesPanelMode.DUPLICATES -> stringResource(R.string.admin_username_duplicates_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = TarnishedGold,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = TarnishedGold
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = Obsidian
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.admin_username_registry_title), color = OldIvory) },
                    onClick = {
                        menuExpanded = false
                        onModeChange(AdminUsernamesPanelMode.REGISTRY)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.admin_username_duplicates_title), color = OldIvory) },
                    onClick = {
                        menuExpanded = false
                        onModeChange(AdminUsernamesPanelMode.DUPLICATES)
                    }
                )
            }
        }
    }
}

@Composable
private fun UsernameRegistryContent(
    users: List<UsernameRegistryUser>,
    selectedFilter: AdminUsernameRegistryFilter,
    showNotifiedOnly: Boolean,
    isLoading: Boolean,
    savingUserIds: Set<String>,
    onFilterChange: (AdminUsernameRegistryFilter) -> Unit,
    onToggleNotifiedOnly: () -> Unit,
    onMarkReviewed: (String) -> Unit,
    onRequestChange: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit
) {
    UsernameRegistryFilters(
        selectedFilter = selectedFilter,
        showNotifiedOnly = showNotifiedOnly,
        onFilterChange = onFilterChange,
        onToggleNotifiedOnly = onToggleNotifiedOnly
    )

    Spacer(modifier = Modifier.height(12.dp))

    when {
        isLoading && users.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TarnishedGold)
            }
        }

        users.isEmpty() -> {
            EmptyUsernameRegistryCard()
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = users,
                    key = { it.uid }
                ) { user ->
                    UsernameRegistryRow(
                        user = user,
                        isSaving = user.uid in savingUserIds,
                        onMarkReviewed = onMarkReviewed,
                        onRequestChange = onRequestChange,
                        onOpenUserProfile = onOpenUserProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun UsernameRegistryFilters(
    selectedFilter: AdminUsernameRegistryFilter,
    showNotifiedOnly: Boolean,
    onFilterChange: (AdminUsernameRegistryFilter) -> Unit,
    onToggleNotifiedOnly: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdminUsernameRegistryFilter.entries.forEach { filter ->
            TextButton(
                onClick = { onFilterChange(filter) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selectedFilter == filter) BloodWine.copy(alpha = 0.5f) else Obsidian.copy(alpha = 0.72f),
                    contentColor = if (selectedFilter == filter) TarnishedGold else OldIvory.copy(alpha = 0.82f)
                )
            ) {
                Text(
                    text = when (filter) {
                        AdminUsernameRegistryFilter.ALL -> stringResource(R.string.admin_username_filter_all)
                        AdminUsernameRegistryFilter.NEW -> stringResource(R.string.admin_username_filter_new)
                        AdminUsernameRegistryFilter.MODIFIED -> stringResource(R.string.admin_username_filter_modified)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onToggleNotifiedOnly,
            modifier = Modifier
                .background(
                    color = if (showNotifiedOnly) BloodWine.copy(alpha = 0.52f) else Obsidian.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(999.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = stringResource(R.string.admin_username_notified_users),
                tint = if (showNotifiedOnly) TarnishedGold else OldIvory.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun UsernameRegistryRow(
    user: UsernameRegistryUser,
    isSaving: Boolean,
    onMarkReviewed: (String) -> Unit,
    onRequestChange: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KaiUserAvatar(
                displayName = user.username.ifBlank { user.email },
                imageUrl = user.photoUrl,
                size = 34.dp,
                showGlow = false
            )

            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    modifier = Modifier.clickable { onOpenUserProfile(user.uid) },
                    style = MaterialTheme.typography.titleMedium,
                    color = OldIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = usernameRegistryStatusLabel(user.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = usernameRegistryStatusColor(user.status),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TarnishedGold,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = { onMarkReviewed(user.uid) },
                    enabled = !user.isReviewed
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.admin_username_mark_reviewed),
                        tint = if (user.isReviewed) OldIvory.copy(alpha = 0.28f) else TarnishedGold
                    )
                }
                IconButton(
                    onClick = { onRequestChange(user.uid) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailUnread,
                        contentDescription = stringResource(R.string.admin_username_request_change),
                        tint = if (user.isChangeNotified) BloodWine.copy(alpha = 0.9f) else TarnishedGold
                    )
                }
            }
        }
    }
}

@Composable
private fun usernameRegistryStatusLabel(status: UsernameRegistryStatus): String {
    return when (status) {
        UsernameRegistryStatus.NEW -> stringResource(R.string.admin_username_filter_new)
        UsernameRegistryStatus.REVIEWED -> stringResource(R.string.admin_username_reviewed)
        UsernameRegistryStatus.MODIFIED -> stringResource(R.string.admin_username_filter_modified)
        UsernameRegistryStatus.NOTIFIED -> stringResource(R.string.admin_username_notified)
    }
}

private fun usernameRegistryStatusColor(status: UsernameRegistryStatus): Color {
    return when (status) {
        UsernameRegistryStatus.NEW -> TarnishedGold
        UsernameRegistryStatus.REVIEWED -> OldIvory.copy(alpha = 0.7f)
        UsernameRegistryStatus.MODIFIED -> TarnishedGold.copy(alpha = 0.86f)
        UsernameRegistryStatus.NOTIFIED -> BloodWine.copy(alpha = 0.95f)
    }
}

@Composable
private fun EmptyUsernameRegistryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f))
    ) {
        Text(
            text = stringResource(R.string.admin_username_registry_empty),
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = DeepWalnut
        )
    }
}

@Composable
private fun DuplicateUsernamesContent(
    groups: List<UsernameConflictGroup>,
    isLoading: Boolean,
    draftUsernames: Map<String, String>,
    savingUserIds: Set<String>,
    onDraftUsernameChange: (String, String) -> Unit,
    onResolveUsername: (String, String) -> Unit
) {
    when {
        isLoading && groups.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TarnishedGold)
            }
        }

        groups.isEmpty() -> {
            EmptyAdminConflictsCard()
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AdminSummaryCard(groupCount = groups.size)
                }

                items(
                    items = groups,
                    key = { it.normalizedUsername }
                ) { group ->
                    ConflictGroupCard(
                        group = group,
                        draftUsernames = draftUsernames,
                        savingUserIds = savingUserIds,
                        onDraftUsernameChange = onDraftUsernameChange,
                        onResolveUsername = onResolveUsername
                    )
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
