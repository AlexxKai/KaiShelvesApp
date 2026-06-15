package com.example.kaishelvesapp.ui.screen.foryou

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.BookRecommendation
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.BookShelfActions
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.ForYouViewModel
import kotlinx.coroutines.launch

@Composable
fun ForYouScreen(
    viewModel: ForYouViewModel,
    subtitle: String = "",
    personalizedSuggestionsEnabled: Boolean,
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
    onOpenBook: (Libro) -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()

    LaunchedEffect(personalizedSuggestionsEnabled) {
        viewModel.loadRecommendations(personalizedSuggestionsEnabled)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.FOR_YOU,
                subtitle = stringResource(R.string.for_you_subtitle),
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
                    current = KaiSection.FOR_YOU,
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
                when {
                    !personalizedSuggestionsEnabled -> {
                        ForYouDisabledCard(onGoToProfile = onGoToProfile)
                    }

                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }

                    uiState.errorMessage != null -> {
                        ForYouMessageCard(
                            title = stringResource(R.string.for_you_error_title),
                            body = uiState.errorMessage.orEmpty(),
                            actionText = stringResource(R.string.retry),
                            onAction = { viewModel.loadRecommendations(true) }
                        )
                    }

                    uiState.recommendations.isEmpty() -> {
                        ForYouMessageCard(
                            title = stringResource(R.string.for_you_empty_title),
                            body = stringResource(R.string.for_you_empty_body),
                            actionText = null,
                            onAction = {}
                        )
                    }

                    else -> {
                        ForYouRecommendationsList(
                            recommendations = uiState.recommendations,
                            onOpenBook = onOpenBook
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForYouRecommendationsList(
    recommendations: List<BookRecommendation>,
    onOpenBook: (Libro) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = recommendations,
            key = { recommendation ->
                recommendation.book.id.ifBlank {
                    "${recommendation.book.titulo}-${recommendation.book.autor}"
                }
            }
        ) { recommendation ->
            RecommendationCard(
                recommendation = recommendation,
                onClick = { onOpenBook(recommendation.book) }
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: BookRecommendation,
    onClick: () -> Unit
) {
    val book = recommendation.book
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.26f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            BookCover(
                imageUrl = book.imagen,
                title = book.titulo,
                modifier = Modifier
                    .width(74.dp)
                    .height(112.dp),
                containerColor = Color.Transparent,
                borderColor = TarnishedGold.copy(alpha = 0.22f)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.titulo.ifBlank { stringResource(R.string.unknown_title) },
                    style = MaterialTheme.typography.titleMedium,
                    color = OldIvory,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.autor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.autor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = listOf(
                            book.genero.takeIf { it.isNotBlank() },
                            book.fechaPublicacion.takeIf { it > 0 }?.toString()
                        ).filterNotNull().joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    BookShelfActions(
                        book = book,
                        modifier = Modifier.width(112.dp),
                        viewModelKeyPrefix = "for_you_shelf",
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ForYouDisabledCard(
    onGoToProfile: () -> Unit
) {
    ForYouMessageCard(
        title = stringResource(R.string.for_you_disabled_title),
        body = stringResource(R.string.for_you_disabled_body),
        actionText = stringResource(R.string.for_you_open_privacy),
        onAction = onGoToProfile
    )
}

@Composable
private fun ForYouMessageCard(
    title: String,
    body: String,
    actionText: String?,
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
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                BloodWine.copy(alpha = 0.18f),
                                DeepWalnut,
                                Obsidian
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (actionText == null) Icons.Filled.AutoAwesome else Icons.Filled.Settings,
                    contentDescription = null,
                    tint = TarnishedGold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (actionText != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TarnishedGold,
                            contentColor = Obsidian
                        )
                    ) {
                        Text(text = actionText)
                    }
                }
            }
        }
    }
}

