package com.example.kaishelvesapp.ui.screen.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.data.repository.FriendActivityType
import com.example.kaishelvesapp.ui.components.BookCover
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
import com.example.kaishelvesapp.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    title: String = "",
    subtitle: String = "",
    currentSection: KaiSection = KaiSection.HOME,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    userName: String? = null,
    profileImageUrl: String? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenFriendProfile: (String) -> Unit,
    onOpenBook: (Libro) -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = currentSection,
                subtitle = subtitle,
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
            }
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
                    .padding(paddingValues)
                    .padding(innerPadding),
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshFeed
            ) {
                when {
                    uiState.isLoading && uiState.activities.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }

                    uiState.errorMessage != null -> {
                        HomeMessageCard(
                            title = stringResource(R.string.home_recent_activity_title),
                            message = uiState.errorMessage!!,
                            actionLabel = stringResource(R.string.retry),
                            onAction = viewModel::loadFeed
                        )
                    }

                    uiState.activities.isEmpty() -> {
                        HomeMessageCard(
                            title = stringResource(R.string.home_empty_activity_title),
                            message = stringResource(R.string.home_empty_activity_body),
                            actionLabel = stringResource(R.string.retry),
                            onAction = viewModel::loadFeed
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.home_recent_activity_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TarnishedGold
                                )
                            }

                            items(
                                items = uiState.activities,
                                key = { item ->
                                    buildString {
                                        append(item.user.uid)
                                        append('_')
                                        append(item.type.name)
                                        append('_')
                                        append(item.book?.id ?: item.readBook?.id ?: "activity")
                                        append('_')
                                        append(item.timestampMillis ?: -1L)
                                    }
                                }
                            ) { item ->
                                FeedActivityCard(
                                    item = item,
                                    onOpenFriendProfile = onOpenFriendProfile,
                                    onOpenBook = onOpenBook
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
private fun HomeMessageCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onAction) {
                    Text(text = actionLabel, color = Color(0xFF66D6D6))
                }
            }
        }
    }
}

@Composable
private fun FeedActivityCard(
    item: FriendActivityItem,
    onOpenFriendProfile: (String) -> Unit,
    onOpenBook: (Libro) -> Unit
) {
    val book = remember(item) { item.book ?: item.readBook?.toLibro() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.12f),
                            Obsidian.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.clickable(
                        enabled = item.user.uid.isNotBlank(),
                        onClick = { onOpenFriendProfile(item.user.uid) }
                    )
                ) {
                    KaiUserAvatar(
                        displayName = item.user.usuario.ifBlank { item.user.email },
                        imageUrl = item.user.photoUrl
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activityTitle(item),
                        style = MaterialTheme.typography.titleLarge,
                        color = OldIvory,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(
                            enabled = item.user.uid.isNotBlank(),
                            onClick = { onOpenFriendProfile(item.user.uid) }
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatActivityTimestamp(item.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory.copy(alpha = 0.72f)
                    )
                }
            }

            if (book != null) {
                Spacer(modifier = Modifier.height(14.dp))
                FeedBookCard(
                    item = item,
                    book = book,
                    onOpenBook = onOpenBook
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_feed_like),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF66D6D6)
                )
                Text(
                    text = stringResource(R.string.home_feed_comment),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF66D6D6)
                )
            }
        }
    }
}

@Composable
private fun FeedBookCard(
    item: FriendActivityItem,
    book: Libro,
    onOpenBook: (Libro) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onOpenBook(book) }),
        verticalAlignment = Alignment.Top
    ) {
        BookCover(
            imageUrl = book.imagen,
            title = book.titulo,
            modifier = Modifier
                .width(92.dp)
                .height(136.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = book.titulo.ifBlank { stringResource(R.string.unknown_title) },
                style = MaterialTheme.typography.titleLarge,
                color = OldIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (book.autor.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.book_by_author, book.autor),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF66D6D6),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D9E5B))
            ) {
                Text(
                    text = statusLabel(item.type),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = OldIvory
                )
            }
        }
    }
}

@Composable
private fun activityTitle(item: FriendActivityItem): String {
    val userName = item.user.usuario.ifBlank { stringResource(R.string.unknown_username) }
    return when (item.type) {
        FriendActivityType.FRIENDSHIP -> stringResource(
            R.string.friendship_update_text,
            userName,
            item.relatedUserName.orEmpty()
        )
        FriendActivityType.WANT_TO_READ -> stringResource(R.string.friend_wants_to_read, userName)
        FriendActivityType.READING -> stringResource(R.string.friend_is_reading, userName)
        FriendActivityType.READ -> stringResource(R.string.friend_has_read, userName)
    }
}

@Composable
private fun statusLabel(type: FriendActivityType): String {
    return when (type) {
        FriendActivityType.FRIENDSHIP -> stringResource(R.string.friends)
        FriendActivityType.WANT_TO_READ -> stringResource(R.string.want_to_read)
        FriendActivityType.READING -> stringResource(R.string.currently_reading)
        FriendActivityType.READ -> stringResource(R.string.mark_as_read)
    }
}

@Composable
private fun formatActivityTimestamp(timestampMillis: Long?): String {
    if (timestampMillis == null) {
        return stringResource(R.string.recently_label)
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "es") {
        "d 'de' MMM 'a la(s)' HH:mm"
    } else {
        "MMM d 'at' HH:mm"
    }

    return remember(timestampMillis, locale) {
        SimpleDateFormat(pattern, locale)
            .format(Date(timestampMillis))
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(locale) else char.toString()
            }
    }
}

private fun LibroLeido.toLibro(): Libro {
    return Libro(
        id = id,
        isbn = isbn,
        titulo = titulo,
        autor = autor,
        editorial = editorial,
        genero = genero,
        fechaPublicacion = fechaPublicacion,
        paginas = paginas,
        imagen = imagen,
        pdf = pdf
    )
}
