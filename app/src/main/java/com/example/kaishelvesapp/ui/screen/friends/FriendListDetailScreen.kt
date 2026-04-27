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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.repository.FriendBookListDetailBookItem
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.util.formatReadDateForDisplay
import com.example.kaishelvesapp.ui.viewmodel.FriendListDetailViewModel

private enum class FriendListDetailSortOption {
    TITLE,
    AUTHOR,
    RATING,
    READ_DATE
}

@Composable
fun FriendListDetailScreen(
    friendUid: String,
    listId: String,
    viewModel: FriendListDetailViewModel,
    onBack: () -> Unit,
    onBookClick: (Libro) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortOption by remember(friendUid, listId) { mutableStateOf(FriendListDetailSortOption.TITLE) }
    val isReadList = listId == UserListsRepository.SYSTEM_LIST_READ_ID
    val sortedBooks = remember(uiState.books, sortOption, isReadList) {
        when (sortOption) {
            FriendListDetailSortOption.TITLE -> uiState.books.sortedBy { it.book.titulo.lowercase() }
            FriendListDetailSortOption.AUTHOR -> uiState.books.sortedBy { it.book.autor.lowercase() }
            FriendListDetailSortOption.RATING -> uiState.books.sortedWith(
                compareByDescending<FriendBookListDetailBookItem> { it.rating ?: -1 }
                    .thenBy { it.book.titulo.lowercase() }
            )
            FriendListDetailSortOption.READ_DATE -> uiState.books.sortedWith(
                compareByDescending<FriendBookListDetailBookItem> { it.readDate.orEmpty() }
                    .thenBy { it.book.titulo.lowercase() }
            )
        }
    }

    LaunchedEffect(friendUid, listId) {
        viewModel.loadListDetail(friendUid, listId)
        sortOption = if (isReadList) FriendListDetailSortOption.READ_DATE else FriendListDetailSortOption.TITLE
    }

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
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
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FriendListDetailHeaderCard(
                    userList = uiState.userList,
                    books = sortedBooks,
                    sortOption = sortOption,
                    isReadList = isReadList,
                    onBack = onBack,
                    onSortChange = {
                        sortOption = when {
                            !isReadList && sortOption == FriendListDetailSortOption.TITLE -> FriendListDetailSortOption.AUTHOR
                            !isReadList -> FriendListDetailSortOption.TITLE
                            sortOption == FriendListDetailSortOption.READ_DATE -> FriendListDetailSortOption.RATING
                            sortOption == FriendListDetailSortOption.RATING -> FriendListDetailSortOption.TITLE
                            else -> FriendListDetailSortOption.READ_DATE
                        }
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }
                }

                uiState.books.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Obsidian),
                            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.75f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = uiState.errorMessage ?: stringResource(R.string.empty_list_books_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TarnishedGold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.empty_list_books_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OldIvory
                                )
                            }
                        }
                    }
                }

                else -> {
                    items(sortedBooks, key = { it.book.id.ifBlank { it.book.isbn } }) { item ->
                        FriendListBookCard(
                            item = item,
                            isReadList = isReadList,
                            onOpen = { onBookClick(item.book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendListDetailHeaderCard(
    userList: UserBookList?,
    books: List<FriendBookListDetailBookItem>,
    sortOption: FriendListDetailSortOption,
    isReadList: Boolean,
    onBack: () -> Unit,
    onSortChange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut.copy(alpha = 0.96f),
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TarnishedGold
                    )
                }

                Text(
                    text = userList?.name ?: stringResource(R.string.list_detail_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (books.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        books.take(3).forEach { item ->
                            BookCover(
                                imageUrl = item.book.imagen,
                                title = item.book.titulo,
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(42.dp)
                            )
                        }
                    }
                }
            }

            userList?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.list_books_count, userList?.bookCount ?: books.size),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )

            if (books.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = BloodWine.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSortChange() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sort_by_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = when {
                            !isReadList && sortOption == FriendListDetailSortOption.AUTHOR -> stringResource(R.string.sort_option_author)
                            !isReadList -> stringResource(R.string.sort_option_title)
                            sortOption == FriendListDetailSortOption.READ_DATE -> stringResource(R.string.sort_option_read_date)
                            sortOption == FriendListDetailSortOption.RATING -> stringResource(R.string.sort_option_rating)
                            else -> stringResource(R.string.sort_option_title)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TarnishedGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort_action),
                        tint = TarnishedGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendListBookCard(
    item: FriendBookListDetailBookItem,
    isReadList: Boolean,
    onOpen: () -> Unit
) {
    val libro = item.book
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                imageUrl = libro.imagen,
                title = libro.titulo,
                modifier = Modifier
                    .width(62.dp)
                    .height(92.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                    style = MaterialTheme.typography.titleLarge,
                    color = TarnishedGold
                )

                if (libro.autor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = libro.autor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )
                }

                if (libro.genero.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = libro.genero,
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory
                    )
                }

                if (isReadList) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if ((item.rating ?: 0) > 0) {
                            stringResource(R.string.rating_value, item.rating ?: 0)
                        } else {
                            stringResource(R.string.not_rated_yet)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory
                    )

                    item.readDate?.takeIf { it.isNotBlank() }?.let { readDate ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.read_date_value,
                                formatReadDateForDisplay(readDate)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = OldIvory.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }
    }
}
