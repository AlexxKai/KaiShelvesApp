package com.example.kaishelvesapp.ui.screen.catalog

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.BookShelfActions
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.SearchResultsSortMode
import com.example.kaishelvesapp.ui.viewmodel.SearchResultsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: SearchResultsViewModel,
    onBack: () -> Unit,
    onBookClick: (Libro) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTopButton = !uiState.isLoading &&
        uiState.errorMessage == null &&
        (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240)

    LaunchedEffect(
        listState,
        uiState.libros.size,
        uiState.canLoadMore,
        uiState.isLoading,
        uiState.isLoadingMore
    ) {
        snapshotFlow {
            if (!uiState.canLoadMore || uiState.isLoading || uiState.isLoadingMore || uiState.libros.isEmpty()) {
                false
            } else {
                val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItemsCount = listState.layoutInfo.totalItemsCount
                totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 4
            }
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            SearchResultsTopBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = { viewModel.search() },
                onClear = viewModel::clearSearchQuery,
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (showScrollToTopButton) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = DeepWalnut,
                    contentColor = TarnishedGold
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.scroll_to_top)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TarnishedGold)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    SearchResultsList(
                        books = uiState.libros,
                        totalResults = uiState.totalResults,
                        submittedQuery = uiState.submittedQuery,
                        sortMode = uiState.sortMode,
                        listState = listState,
                        isLoadingMore = uiState.isLoadingMore,
                        onSortModeChange = viewModel::onSortModeChange,
                        onBookClick = onBookClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchResultsTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun submitSearch() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onSearch()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepWalnut,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = OldIvory
                )
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = OldIvory
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = OldIvory
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Obsidian.copy(alpha = 0.82f),
                    unfocusedContainerColor = Obsidian.copy(alpha = 0.82f),
                    disabledContainerColor = Obsidian.copy(alpha = 0.82f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = OldIvory,
                    unfocusedTextColor = OldIvory,
                    cursorColor = TarnishedGold
                ),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() })
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    books: List<Libro>,
    totalResults: Int,
    submittedQuery: String,
    sortMode: SearchResultsSortMode,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onSortModeChange: (SearchResultsSortMode) -> Unit,
    onBookClick: (Libro) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            SearchResultsHeader(
                totalResults = totalResults,
                submittedQuery = submittedQuery,
                sortMode = sortMode,
                onSortModeChange = onSortModeChange
            )
            HorizontalDivider(color = OldIvory.copy(alpha = 0.76f))
        }

        if (books.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_books_found),
                        color = OldIvory
                    )
                }
            }
        }

        items(
            items = books,
            key = { book -> book.id.ifBlank { book.isbn.ifBlank { "${book.titulo}-${book.autor}" } } }
        ) { book ->
            SearchResultBookRow(
                libro = book,
                onClick = { onBookClick(book) }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TarnishedGold,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(
    totalResults: Int,
    submittedQuery: String,
    sortMode: SearchResultsSortMode,
    onSortModeChange: (SearchResultsSortMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.search_results_count, totalResults, submittedQuery),
            color = OldIvory,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        SearchResultsSortControls(
            sortMode = sortMode,
            onSortModeChange = onSortModeChange
        )
    }
}

@Composable
private fun SearchResultsSortControls(
    sortMode: SearchResultsSortMode,
    onSortModeChange: (SearchResultsSortMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SearchResultsSortChip(
            text = stringResource(R.string.sort_newest),
            selected = sortMode == SearchResultsSortMode.NEWEST,
            onClick = { onSortModeChange(SearchResultsSortMode.NEWEST) },
            modifier = Modifier.weight(1f)
        )
        SearchResultsSortChip(
            text = stringResource(R.string.sort_best_rated),
            selected = sortMode == SearchResultsSortMode.RATING,
            onClick = { onSortModeChange(SearchResultsSortMode.RATING) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SearchResultsSortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(30.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) TarnishedGold.copy(alpha = 0.24f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) TarnishedGold else OldIvory.copy(alpha = 0.42f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) OldIvory else OldIvory.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchResultBookRow(
    libro: Libro,
    onClick: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BookCover(
                    imageUrl = libro.imagen,
                    title = libro.titulo,
                    modifier = Modifier
                        .width(118.dp)
                        .height(176.dp)
                        .clickable(onClick = onClick)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Column(
                        modifier = Modifier.clickable(onClick = onClick),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                            color = Color(0xFF6BC6C0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (libro.autor.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.book_by_author, libro.autor),
                                color = OldIvory,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (libro.editorial.isNotBlank()) {
                        Text(
                            text = libro.editorial,
                            color = OldIvory.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (libro.fechaPublicacion != 0) {
                        Text(
                            text = libro.fechaPublicacion.toString(),
                            color = OldIvory.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (libro.averageRating > 0.0) {
                        SearchResultRating(
                            averageRating = libro.averageRating,
                            ratingsCount = libro.ratingsCount
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    BookShelfActions(
                        book = libro,
                        viewModelKeyPrefix = "search_results_shelf",
                        compact = true
                    )
                }
            }
        }

        HorizontalDivider(color = OldIvory.copy(alpha = 0.72f))
    }
}

@Composable
private fun SearchResultRating(
    averageRating: Double,
    ratingsCount: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFFB326),
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = stringResource(
                R.string.google_books_rating_summary,
                String.format(Locale.getDefault(), "%.1f", averageRating),
                ratingsCount
            ),
            color = OldIvory.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
