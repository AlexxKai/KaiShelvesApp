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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.FriendRequestsViewModel

private enum class NotificationCenterTab {
    NOTIFICATIONS,
    MESSAGES,
    REQUESTS
}

@Composable
fun NotificationCenterScreen(
    viewModel: FriendRequestsViewModel,
    onBack: () -> Unit,
    onRequestsChanged: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(NotificationCenterTab.REQUESTS) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadReceivedRequests()
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.FRIENDS,
                onSelect = onSectionSelected
            )
        }
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
                title = stringResource(R.string.notification_center_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = stringResource(R.string.back),
                onNavigationClick = onBack,
                actionIcon = Icons.Filled.NotificationsNone,
                actionIconContentDescription = stringResource(R.string.notifications_tab),
                onActionIconClick = {},
                centerTitle = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            NotificationTabs(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedTab) {
                NotificationCenterTab.NOTIFICATIONS -> {
                    NotificationPlaceholder(
                        text = stringResource(R.string.notifications_coming_soon)
                    )
                }

                NotificationCenterTab.MESSAGES -> {
                    NotificationPlaceholder(
                        text = stringResource(R.string.messages_coming_soon)
                    )
                }

                NotificationCenterTab.REQUESTS -> {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    } else if (uiState.receivedRequests.isEmpty()) {
                        NotificationPlaceholder(
                            text = stringResource(R.string.no_friend_requests)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.receivedRequests,
                                key = { it.uid }
                            ) { request ->
                                FriendRequestCard(
                                    request = request,
                                    onAccept = {
                                        viewModel.acceptRequest(request, onSuccess = onRequestsChanged)
                                    },
                                    onReject = {
                                        viewModel.rejectRequest(request, onSuccess = onRequestsChanged)
                                    }
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
private fun NotificationTabs(
    selectedTab: NotificationCenterTab,
    onSelect: (NotificationCenterTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NotificationTabItem(
            label = stringResource(R.string.notifications_tab),
            selected = selectedTab == NotificationCenterTab.NOTIFICATIONS,
            onClick = { onSelect(NotificationCenterTab.NOTIFICATIONS) },
            modifier = Modifier.weight(1f)
        )
        NotificationTabItem(
            label = stringResource(R.string.messages_tab),
            selected = selectedTab == NotificationCenterTab.MESSAGES,
            onClick = { onSelect(NotificationCenterTab.MESSAGES) },
            modifier = Modifier.weight(1f)
        )
        NotificationTabItem(
            label = stringResource(R.string.requests_tab),
            selected = selectedTab == NotificationCenterTab.REQUESTS,
            onClick = { onSelect(NotificationCenterTab.REQUESTS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NotificationTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) OldIvory else OldIvory.copy(alpha = 0.72f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (selected) Color(0xFF10B8C5) else Color.Transparent,
                    RoundedCornerShape(999.dp)
                )
        )
    }
}

@Composable
private fun NotificationPlaceholder(
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = OldIvory,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FriendRequestCard(
    request: Usuario,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                displayName = request.usuario.ifBlank { request.email },
                imageUrl = request.photoUrl
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.usuario.ifBlank { stringResource(R.string.unknown_username) },
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepWalnut,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = request.email.ifBlank { stringResource(R.string.no_email_available) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepWalnut.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.friend_request_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = BloodWine
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloodWine,
                            contentColor = OldIvory
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.accept_request))
                    }

                    Button(
                        onClick = onReject,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepWalnut.copy(alpha = 0.9f),
                            contentColor = OldIvory
                        ),
                        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.reject_request))
                    }
                }
            }
        }
    }
}
