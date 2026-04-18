package com.example.kaishelvesapp.ui.screen.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.FriendListItem
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.FriendsViewModel
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    title: String = "",
    subtitle: String = "",
    currentSection: KaiSection = KaiSection.FRIENDS,
    userName: String? = null,
    profileImageUrl: String? = null,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenFriendProfile: (String) -> Unit,
    onSectionSelected: (KaiSection) -> Unit,
    viewModel: FriendsViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var friendFilter by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadFriends()
    }

    val filteredFriends = uiState.friends.filter { friend ->
        val query = friendFilter.trim()
        query.isBlank() ||
            friend.user.usuario.contains(query, ignoreCase = true) ||
            friend.user.email.contains(query, ignoreCase = true)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = currentSection,
                subtitle = if (subtitle.isBlank()) {
                    stringResource(R.string.friends_screen_subtitle)
                } else {
                    subtitle
                },
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
                    onOpenNotifications = onOpenNotifications
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = currentSection,
                    onSelect = onSectionSelected
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onOpenSuggestions,
                    containerColor = Color(0xFF0D7C79),
                    contentColor = Color.White
                ) {
                    AddFriendFabIcon()
                }
            }
        ) { innerPadding ->
            LazyColumn(
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
                    .padding(paddingValues)
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    FriendsSummarySearchCard(
                        title = if (title.isBlank()) stringResource(R.string.friends) else title,
                        totalFriends = uiState.friends.size,
                        value = friendFilter,
                        onValueChange = { friendFilter = it }
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }
                } else if (filteredFriends.isEmpty()) {
                    item {
                        EmptyFriendsState(
                            hasSearch = friendFilter.isNotBlank()
                        )
                    }
                } else {
                    items(
                        items = filteredFriends,
                        key = { it.user.uid }
                    ) { friend ->
                        FriendRow(
                            friend = friend,
                            onClick = { onOpenFriendProfile(friend.user.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsSummarySearchCard(
    title: String,
    totalFriends: Int,
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepWalnut,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = TarnishedGold.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (totalFriends == 1) {
                            stringResource(R.string.one_friend)
                        } else {
                            stringResource(R.string.friends_count, totalFriends)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepWalnut,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FriendSearchField(
                value = value,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
private fun AddFriendFabIcon() {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.add_friend),
            modifier = Modifier
                .size(21.dp)
                .offset(x = (-4).dp),
            tint = Color.White
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun FriendSearchField(
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
                text = stringResource(R.string.search_friends),
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
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.86f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.86f),
            focusedBorderColor = TarnishedGold,
            unfocusedBorderColor = TarnishedGold.copy(alpha = 0.45f),
            focusedTextColor = DeepWalnut,
            unfocusedTextColor = DeepWalnut,
            cursorColor = BloodWine
        )
    )
}

@Composable
private fun FriendRow(
    friend: FriendListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                displayName = friend.user.usuario.ifBlank { friend.user.email },
                imageUrl = friend.user.photoUrl
            )

            Spacer(modifier = Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepWalnut,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = friendMetadata(friend),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepWalnut.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyFriendsState(
    hasSearch: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasSearch) {
                    stringResource(R.string.no_friends_found)
                } else {
                    stringResource(R.string.no_added_friends)
                },
                style = MaterialTheme.typography.titleMedium,
                color = DeepWalnut
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasSearch) {
                    stringResource(R.string.try_another_friend_search)
                } else {
                    stringResource(R.string.add_friends_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = DeepWalnut.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun friendMetadata(friend: FriendListItem): String {
    val booksLabel = if (friend.booksRead == 1) {
        stringResource(R.string.one_book)
    } else {
        stringResource(R.string.books_count, friend.booksRead)
    }
    val friendsLabel = if (friend.friendsCount == 1) {
        stringResource(R.string.one_friend)
    } else {
        stringResource(R.string.friends_count, friend.friendsCount)
    }
    return "$booksLabel • $friendsLabel"
}
