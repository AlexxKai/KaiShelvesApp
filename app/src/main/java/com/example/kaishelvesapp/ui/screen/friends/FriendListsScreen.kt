package com.example.kaishelvesapp.ui.screen.friends

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.FriendBookListSummary
import com.example.kaishelvesapp.data.repository.FriendBookTagSummary
import com.example.kaishelvesapp.data.repository.FRIEND_TAG_DETAIL_PREFIX
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.FriendListsViewModel

@Composable
fun FriendListsScreen(
    friendUid: String,
    friendName: String,
    viewModel: FriendListsViewModel,
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAllCollections by rememberSaveable(friendUid) { androidx.compose.runtime.mutableStateOf(false) }
    val predefinedListIds = setOf(
        UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID,
        UserListsRepository.SYSTEM_LIST_READING_ID,
        UserListsRepository.SYSTEM_LIST_READ_ID,
        UserListsRepository.SYSTEM_LIST_PENDING_ID,
        UserListsRepository.SYSTEM_LIST_UNFINISHED_ID
    )

    LaunchedEffect(friendUid) {
        viewModel.loadFriendLists(friendUid)
        showAllCollections = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                KaiTopBar(
                    title = stringResource(R.string.friend_lists_title, friendName),
                    subtitle = stringResource(R.string.friend_lists_subtitle),
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    navigationContentDescription = stringResource(R.string.back),
                    onNavigationClick = onBack,
                    centerTitle = true
                )
            }
        },
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.FRIENDS,
                onSelect = onSectionSelected
            )
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
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }
                }

                uiState.lists.isEmpty() && uiState.tags.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Obsidian),
                            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
                        ) {
                            Text(
                                text = uiState.errorMessage ?: stringResource(R.string.no_friend_lists_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = OldIvory,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 28.dp)
                            )
                        }
                    }
                }

                else -> {
                    if (showAllCollections) {
                        items(
                            items = uiState.lists,
                            key = { it.id }
                        ) { list ->
                            FriendListSummaryCard(
                                list = list,
                                onOpen = { onOpenList(list.id) }
                            )
                        }

                        items(
                            items = uiState.tags,
                            key = { it.id }
                        ) { tag ->
                            FriendTagSummaryCard(
                                tag = tag,
                                onOpen = { onOpenList(FRIEND_TAG_DETAIL_PREFIX + tag.id) }
                            )
                        }
                    } else {
                        items(
                            items = uiState.lists.filter { it.id in predefinedListIds },
                            key = { it.id }
                        ) { list ->
                            FriendListSummaryCard(
                                list = list,
                                onOpen = { onOpenList(list.id) }
                            )
                        }

                        item {
                            FriendTagsPreviewSection(
                                tags = uiState.tags.filter { it.bookCount > 0 }.take(6),
                                onOpenTag = { tag -> onOpenList(FRIEND_TAG_DETAIL_PREFIX + tag.id) }
                            )
                        }

                        item {
                            TextButton(
                                onClick = { showAllCollections = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.view_all).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF66D6D6),
                                    fontWeight = FontWeight.SemiBold
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
private fun FriendTagsPreviewSection(
    tags: List<FriendBookTagSummary>,
    onOpenTag: (FriendBookTagSummary) -> Unit
) {
    if (tags.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tags_title).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = OldIvory,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(172.dp)
                .height(1.dp)
                .background(TarnishedGold.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.height(22.dp))

        tags.chunked(3).forEach { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowTags.forEachIndexed { index, tag ->
                    FriendTagChip(
                        tag = tag,
                        onClick = { onOpenTag(tag) }
                    )
                    if (index < rowTags.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FriendTagChip(
    tag: FriendBookTagSummary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Color(0xFF244845).copy(alpha = 0.72f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF66D6D6).copy(alpha = 0.18f))
    ) {
        Text(
            text = "${tag.name} (${tag.bookCount})",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9DE6DD),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FriendTagSummaryCard(
    tag: FriendBookTagSummary,
    onOpen: () -> Unit
) {
    FriendListSummaryCard(
        list = FriendBookListSummary(
            id = FRIEND_TAG_DETAIL_PREFIX + tag.id,
            name = tag.name,
            bookCount = tag.bookCount,
            previewImageUrls = tag.previewImageUrls
        ),
        onOpen = onOpen
    )
}

@Composable
private fun FriendListSummaryCard(
    list: FriendBookListSummary,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (list.previewImageUrls.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    list.previewImageUrls.take(3).forEach { imageUrl ->
                        BookCover(
                            imageUrl = imageUrl,
                            title = list.name,
                            modifier = Modifier
                                .width(34.dp)
                                .height(52.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(64.dp)
                        .background(
                            color = TarnishedGold.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = TarnishedGold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (list.bookCount == 1) {
                        stringResource(R.string.one_book)
                    } else {
                        stringResource(R.string.books_count, list.bookCount)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory.copy(alpha = 0.9f)
                )
            }
        }
    }
}
