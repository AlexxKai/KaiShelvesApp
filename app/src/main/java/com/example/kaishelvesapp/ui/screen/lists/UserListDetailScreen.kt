package com.example.kaishelvesapp.ui.screen.lists

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.BookShelfActions
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.util.formatReadDateForDisplay
import com.example.kaishelvesapp.ui.viewmodel.UserListDetailBookItem
import com.example.kaishelvesapp.ui.viewmodel.UserListDetailViewModel

private enum class ListDetailSortOption {
    TITLE,
    AUTHOR,
    RATING,
    READ_DATE
}

@Composable
fun UserListDetailScreen(
    listId: String,
    viewModel: UserListDetailViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onBookClick: (Libro) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var sortOption by remember(listId) { mutableStateOf(ListDetailSortOption.TITLE) }
    val isReadList = listId == com.example.kaishelvesapp.data.repository.UserListsRepository.SYSTEM_LIST_READ_ID
    val sortedBooks = remember(uiState.books, sortOption, isReadList) {
        when (sortOption) {
            ListDetailSortOption.TITLE -> uiState.books.sortedBy { it.book.titulo.lowercase() }
            ListDetailSortOption.AUTHOR -> uiState.books.sortedBy { it.book.autor.lowercase() }
            ListDetailSortOption.RATING -> uiState.books.sortedWith(
                compareByDescending<UserListDetailBookItem> { it.rating ?: -1 }
                    .thenBy { it.book.titulo.lowercase() }
            )
            ListDetailSortOption.READ_DATE -> uiState.books.sortedWith(
                compareByDescending<UserListDetailBookItem> { it.readDate.orEmpty() }
                    .thenBy { it.book.titulo.lowercase() }
            )
        }
    }

    LaunchedEffect(listId) {
        viewModel.loadListDetail(listId)
        sortOption = if (isReadList) ListDetailSortOption.READ_DATE else ListDetailSortOption.TITLE
    }

    LaunchedEffect(uiState.errorMessageRes, uiState.successMessageRes) {
        uiState.errorMessageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.clearMessages()
        }

        uiState.successMessageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ListDetailHeaderCard(
                    userList = uiState.userList,
                    books = sortedBooks,
                    sortOption = sortOption,
                    isReadList = isReadList,
                    onBack = onBack,
                    onSortChange = {
                        sortOption = when {
                            !isReadList && sortOption == ListDetailSortOption.TITLE -> ListDetailSortOption.AUTHOR
                            !isReadList -> ListDetailSortOption.TITLE
                            sortOption == ListDetailSortOption.READ_DATE -> ListDetailSortOption.RATING
                            sortOption == ListDetailSortOption.RATING -> ListDetailSortOption.TITLE
                            else -> ListDetailSortOption.READ_DATE
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
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_list_books_title),
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
                        ListBookCard(
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
private fun ListDetailHeaderCard(
    userList: com.example.kaishelvesapp.data.model.UserBookList?,
    books: List<UserListDetailBookItem>,
    sortOption: ListDetailSortOption,
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                text = stringResource(
                    R.string.list_books_count,
                    userList?.bookCount ?: books.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )

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
                        !isReadList && sortOption == ListDetailSortOption.AUTHOR -> stringResource(R.string.sort_option_author)
                        !isReadList -> stringResource(R.string.sort_option_title)
                        sortOption == ListDetailSortOption.READ_DATE -> stringResource(R.string.sort_option_read_date)
                        sortOption == ListDetailSortOption.RATING -> stringResource(R.string.sort_option_rating)
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

@Composable
private fun ListBookCard(
    item: UserListDetailBookItem,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                        style = MaterialTheme.typography.titleLarge,
                        color = TarnishedGold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (libro.autor.isNotBlank()) {
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

                        if ((item.rating ?: 0) > 0) {
                            RatingStars(
                                rating = item.rating ?: 0,
                                iconSize = 14.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.not_rated_yet),
                                style = MaterialTheme.typography.bodySmall,
                                color = OldIvory
                            )
                        }

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

            Spacer(modifier = Modifier.height(14.dp))

            BookShelfActions(
                book = libro,
                viewModelKeyPrefix = "list_detail_shelf"
            )
        }
    }
}
