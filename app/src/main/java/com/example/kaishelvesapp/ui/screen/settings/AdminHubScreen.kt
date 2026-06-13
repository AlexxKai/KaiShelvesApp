package com.example.kaishelvesapp.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.launch

@Composable
fun AdminHubScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    userName: String? = null,
    profileImageUrl: String? = null,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenConflicts: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.ADMIN,
                subtitle = stringResource(R.string.admin_options_subtitle),
                userName = userName.orEmpty(),
                profileImageUrl = profileImageUrl.orEmpty(),
                expanded = drawerExpanded,
                onGoToProfile = {
                    scope.launch { drawerState.close() }
                    onGoToProfile()
                },
                onGoToSettingsPrivacy = {
                    scope.launch { drawerState.close() }
                    onGoToSettingsPrivacy()
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
                onSectionSelected = { section ->
                    scope.launch { drawerState.close() }
                    onSectionSelected(section)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                KaiPrimaryTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onScanResult = onScanResult,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    notificationCount = pendingRequestCount,
                    onOpenNotifications = onOpenNotifications,
                    showSearchBar = false
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.ADMIN,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            GothicBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
            ) {
                AdminHubContent(
                    onOpenConflicts = onOpenConflicts,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun AdminHubContent(
    onOpenConflicts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var layoutMode by remember { mutableStateOf(AdminHubLayoutMode.List) }
        var adminQuery by remember { mutableStateOf("") }

        val items = listOf(
            AdminHubItem(
                title = stringResource(R.string.admin_conflicts_panel_title),
                body = stringResource(R.string.admin_conflicts_panel_body),
                status = stringResource(R.string.admin_available_status),
                icon = Icons.Filled.Gavel,
                onClick = onOpenConflicts
            ),
            AdminHubItem(
                title = stringResource(R.string.admin_reports_panel_title),
                body = stringResource(R.string.admin_reports_panel_body),
                status = stringResource(R.string.implementation_in_progress),
                icon = Icons.Filled.Report
            ),
            AdminHubItem(
                title = stringResource(R.string.admin_contact_panel_title),
                body = stringResource(R.string.admin_contact_panel_body),
                status = stringResource(R.string.implementation_in_progress),
                icon = Icons.Filled.ContactMail
            ),
            AdminHubItem(
                title = stringResource(R.string.admin_requests_panel_title),
                body = stringResource(R.string.admin_requests_panel_body),
                status = stringResource(R.string.implementation_in_progress),
                icon = Icons.Filled.QuestionAnswer
            ),
            AdminHubItem(
                title = stringResource(R.string.admin_restrictions_panel_title),
                body = stringResource(R.string.admin_restrictions_panel_body),
                status = stringResource(R.string.implementation_in_progress),
                icon = Icons.Filled.Security
            )
        )
        val normalizedQuery = adminQuery.trim().lowercase()
        val visibleItems = if (normalizedQuery.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.title.lowercase().contains(normalizedQuery) ||
                    item.body.lowercase().contains(normalizedQuery)
            }
        }

        AdminHubControlsRow(
            query = adminQuery,
            onQueryChange = { adminQuery = it },
            selectedMode = layoutMode,
            onModeSelected = { layoutMode = it }
        )

        when (layoutMode) {
            AdminHubLayoutMode.List -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visibleItems.forEach { item ->
                        AdminHubCard(item = item)
                    }
                }
            }

            AdminHubLayoutMode.Grid -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visibleItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                AdminHubCard(
                                    item = item,
                                    compact = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminHubControlsRow(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedMode: AdminHubLayoutMode,
    onModeSelected: (AdminHubLayoutMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdminHubSearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )

        AdminHubModeSelector(
            selectedMode = selectedMode,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
private fun AdminHubSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(48.dp),
        placeholder = {
            Text(
                text = stringResource(R.string.admin_search_options),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp
            )
        },
        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = TarnishedGold.copy(alpha = 0.72f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = TarnishedGold.copy(alpha = 0.72f)
                    )
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        colors = com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults.outlinedTextFieldColors()
    )
}

@Composable
private fun AdminHubModeSelector(
    selectedMode: AdminHubLayoutMode,
    onModeSelected: (AdminHubLayoutMode) -> Unit
) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdminHubModeButton(
                icon = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(R.string.admin_view_list),
                selected = selectedMode == AdminHubLayoutMode.List,
                onClick = { onModeSelected(AdminHubLayoutMode.List) }
            )
            AdminHubModeButton(
                icon = Icons.Filled.GridView,
                contentDescription = stringResource(R.string.admin_view_grid),
                selected = selectedMode == AdminHubLayoutMode.Grid,
                onClick = { onModeSelected(AdminHubLayoutMode.Grid) }
            )
        }
}

@Composable
private fun AdminHubModeButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(42.dp)
            .height(34.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BloodWine.copy(alpha = 0.48f) else Obsidian.copy(alpha = 0.42f)
        ),
        border = BorderStroke(
            1.dp,
            if (selected) TarnishedGold.copy(alpha = 0.52f) else TarnishedGold.copy(alpha = 0.16f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            tint = if (selected) TarnishedGold else OldIvory.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun AdminHubCard(
    item: AdminHubItem,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = item.onClick ?: {},
        enabled = item.onClick != null,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Obsidian.copy(alpha = if (item.onClick != null) 0.94f else 0.72f),
            disabledContainerColor = Obsidian.copy(alpha = 0.62f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.onClick != null) TarnishedGold.copy(alpha = 0.38f) else TarnishedGold.copy(alpha = 0.18f)
        )
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = TarnishedGold,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OldIvory,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            return@Card
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = TarnishedGold
                )
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = OldIvory,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.82f)
            )

            Text(
                text = item.status,
                style = MaterialTheme.typography.labelMedium,
                color = TarnishedGold
            )
        }
    }
}

private enum class AdminHubLayoutMode {
    List,
    Grid
}

private data class AdminHubItem(
    val title: String,
    val body: String,
    val status: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null
)
