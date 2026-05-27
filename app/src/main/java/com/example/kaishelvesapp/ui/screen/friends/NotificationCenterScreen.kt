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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.repository.ActivityNotificationItem
import com.example.kaishelvesapp.data.repository.ActivityNotificationType
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.data.repository.FriendActivityType
import com.example.kaishelvesapp.ui.components.ActivitySocialActions
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.FriendRequestsUiState
import com.example.kaishelvesapp.ui.viewmodel.FriendRequestsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotificationCenterTab {
    NOTIFICATIONS,
    REQUESTS
}

@Composable
fun NotificationCenterScreen(
    viewModel: FriendRequestsViewModel,
    initialSelectedNotificationId: String? = null,
    onInitialSelectedNotificationHandled: () -> Unit = {},
    onBack: () -> Unit,
    onOpenFriendProfile: (String) -> Unit = {},
    onRequestsChanged: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(initialNotificationCenterTab(uiState)) }
    var initialTabResolved by rememberSaveable {
        mutableStateOf(
            !initialSelectedNotificationId.isNullOrBlank() ||
                (uiState.hasLoadedReceivedRequests && uiState.hasLoadedActivityNotifications)
        )
    }
    var selectedNotificationId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val closeSelectedNotification = {
        selectedNotificationId?.let(viewModel::markNotificationAsRead)
        selectedNotificationId = null
    }

    LaunchedEffect(Unit) {
        viewModel.ensureReceivedRequestsLoaded()
        viewModel.ensureActivityNotificationsLoaded()
    }

    LaunchedEffect(initialSelectedNotificationId) {
        val notificationId = initialSelectedNotificationId?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect
        selectedTab = NotificationCenterTab.NOTIFICATIONS
        initialTabResolved = true
        selectedNotificationId = notificationId
        viewModel.loadActivityNotifications()
        onInitialSelectedNotificationHandled()
    }

    LaunchedEffect(
        initialSelectedNotificationId,
        uiState.isLoading,
        uiState.isLoadingNotifications,
        uiState.hasLoadedReceivedRequests,
        uiState.hasLoadedActivityNotifications,
        uiState.unreadNotifications.size,
        uiState.receivedRequests.size
    ) {
        if (initialTabResolved || !initialSelectedNotificationId.isNullOrBlank()) return@LaunchedEffect
        if (!uiState.hasLoadedReceivedRequests || !uiState.hasLoadedActivityNotifications) return@LaunchedEffect
        if (uiState.isLoading || uiState.isLoadingNotifications) return@LaunchedEffect

        // Prioriza solicitudes solo cuando no hay actividad pendiente de leer.
        selectedTab = if (uiState.unreadNotifications.isEmpty() && uiState.receivedRequests.isNotEmpty()) {
            NotificationCenterTab.REQUESTS
        } else {
            NotificationCenterTab.NOTIFICATIONS
        }
        initialTabResolved = true
    }

    selectedNotificationId
        ?.let { notificationId -> uiState.notifications.firstOrNull { it.id == notificationId } }
        ?.let { notification ->
        ActivityNotificationDialog(
            notification = notification,
            comments = uiState.commentsByActivityId[notification.activityId].orEmpty(),
            isLoadingComments = notification.activityId in uiState.loadingCommentIds,
            isSaving = uiState.socialActionIds.any { actionId ->
                actionId == notification.activityId || actionId.startsWith("${notification.activityId}:")
            },
            onDismiss = closeSelectedNotification,
            onOpenUserProfile = { userUid ->
                closeSelectedNotification()
                onOpenFriendProfile(userUid)
            },
            onToggleLike = viewModel::toggleLike,
            onLoadComments = viewModel::loadComments,
            onAddComment = viewModel::addComment,
            onToggleCommentLike = viewModel::toggleCommentLike,
            onReplyToComment = viewModel::replyToComment
        )
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
                    when {
                        uiState.isLoadingNotifications -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TarnishedGold)
                            }
                        }

                        uiState.notifications.isEmpty() -> {
                            NotificationPlaceholder(
                                text = stringResource(R.string.notifications_coming_soon)
                            )
                        }

                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = notificationsListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (uiState.unreadNotifications.isNotEmpty()) {
                                        items(
                                            items = uiState.unreadNotifications,
                                            key = { it.id }
                                        ) { notification ->
                                            ActivityNotificationCard(
                                                notification = notification,
                                                onClick = { selectedNotificationId = notification.id }
                                            )
                                        }
                                    }

                                    if (uiState.readNotifications.isNotEmpty()) {
                                        item {
                                            NotificationHistoryHeader()
                                        }
                                        items(
                                            items = uiState.readNotifications,
                                            key = { it.id }
                                        ) { notification ->
                                            ActivityNotificationCard(
                                                notification = notification,
                                                onClick = { selectedNotificationId = notification.id }
                                            )
                                        }
                                    }
                                }

                                if (
                                    notificationsListState.firstVisibleItemIndex > 0 ||
                                    notificationsListState.firstVisibleItemScrollOffset > 0
                                ) {
                                    FloatingActionButton(
                                        onClick = {
                                            scope.launch { notificationsListState.animateScrollToItem(0) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp),
                                        containerColor = BloodWine,
                                        contentColor = OldIvory
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowUpward,
                                            contentDescription = stringResource(R.string.scroll_to_top)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                                    onOpenProfile = {
                                        onOpenFriendProfile(request.uid)
                                    },
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

private fun initialNotificationCenterTab(uiState: FriendRequestsUiState): NotificationCenterTab {
    return if (
        uiState.hasLoadedReceivedRequests &&
        uiState.hasLoadedActivityNotifications &&
        uiState.unreadNotifications.isEmpty() &&
        uiState.receivedRequests.isNotEmpty()
    ) {
        NotificationCenterTab.REQUESTS
    } else {
        NotificationCenterTab.NOTIFICATIONS
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
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, lineHeight = 15.sp),
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
private fun NotificationHistoryHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Antiguas",
            style = MaterialTheme.typography.titleMedium,
            color = TarnishedGold,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TarnishedGold.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun ActivityNotificationCard(
    notification: ActivityNotificationItem,
    onClick: () -> Unit
) {
    val cardAlpha = if (notification.isRead) 0.82f else 0.985f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OldIvory.copy(alpha = cardAlpha)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = if (notification.isRead) 0.14f else 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationTypeIcon(
                    icon = notificationIcon(notification.type),
                    tint = notificationIconTint(notification.type),
                    background = notificationIconTint(notification.type).copy(alpha = 0.13f)
                )

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = notificationMessage(notification),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepWalnut,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            NotificationActivityPreview(item = notification.activity)
        }
    }
}

@Composable
private fun ActivityNotificationDialog(
    notification: ActivityNotificationItem,
    comments: List<com.example.kaishelvesapp.data.repository.ActivityComment>,
    isLoadingComments: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleCommentLike: (String, String) -> Unit,
    onReplyToComment: (String, String, String) -> Unit
) {
    LaunchedEffect(notification.activityId, notification.commentId, notification.type) {
        if (notification.type == ActivityNotificationType.COMMENT ||
            notification.type == ActivityNotificationType.COMMENT_LIKE ||
            notification.type == ActivityNotificationType.COMMENT_REPLY
        ) {
            onLoadComments(notification.activityId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            NotificationInteractionHeader(
                notification = notification,
                onOpenUserProfile = onOpenUserProfile
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (notification.type == ActivityNotificationType.COMMENT ||
                    notification.type == ActivityNotificationType.COMMENT_LIKE ||
                    notification.type == ActivityNotificationType.COMMENT_REPLY
                ) {
                    NotificationCommentActions(
                        notification = notification,
                        comment = comments.firstOrNull { it.id == notification.commentId },
                        isSaving = isSaving,
                        onToggleCommentLike = onToggleCommentLike,
                        onReplyToComment = onReplyToComment
                    )
                }

                OriginalActivityCard(
                    item = notification.activity,
                    comments = comments,
                    isLoadingComments = isLoadingComments,
                    isSaving = isSaving,
                    onToggleLike = onToggleLike,
                    onLoadComments = onLoadComments,
                    onAddComment = onAddComment
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        },
        containerColor = Obsidian,
        titleContentColor = OldIvory,
        textContentColor = OldIvory.copy(alpha = 0.9f)
    )
}

@Composable
private fun NotificationInteractionHeader(
    notification: ActivityNotificationItem,
    onOpenUserProfile: (String) -> Unit
) {
    val userUid = notification.user.uid
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clickable(
                enabled = userUid.isNotBlank(),
                onClick = { onOpenUserProfile(userUid) }
            )
        ) {
            KaiUserAvatar(
                displayName = notification.user.usuario.ifBlank { notification.user.email },
                imageUrl = notification.user.photoUrl,
                size = 42.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .background(notificationIconTint(notification.type), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notificationIcon(notification.type),
                    contentDescription = null,
                    tint = OldIvory,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = notificationMessage(notification),
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory,
                fontWeight = FontWeight.SemiBold
            )

            if (
                (notification.type == ActivityNotificationType.COMMENT ||
                    notification.type == ActivityNotificationType.COMMENT_LIKE ||
                    notification.type == ActivityNotificationType.COMMENT_REPLY) &&
                notification.text.isNotBlank()
            ) {
                Text(
                    text = "\"${notification.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory,
                    fontStyle = FontStyle.Italic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(TarnishedGold.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationCommentActions(
    notification: ActivityNotificationItem,
    comment: ActivityComment?,
    isSaving: Boolean,
    onToggleCommentLike: (String, String) -> Unit,
    onReplyToComment: (String, String, String) -> Unit
) {
    var replyText by rememberSaveable(notification.id) { mutableStateOf("") }
    val canInteract = notification.commentId.isNotBlank()
    val displayComment = comment?.text?.takeIf { it.isNotBlank() } ?: notification.text
    val likedByCurrentUser = comment?.likedByCurrentUser == true
    val likeCount = comment?.likeCount ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (displayComment.isNotBlank()) {
                Text(
                    text = "\"$displayComment\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(enabled = canInteract && !isSaving) {
                        onToggleCommentLike(notification.activityId, notification.commentId)
                    },
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = if (likedByCurrentUser) {
                            stringResource(R.string.comment_unlike)
                        } else {
                            stringResource(R.string.comment_like)
                        },
                        tint = if (likedByCurrentUser) Color(0xFF66D6D6) else OldIvory.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (likeCount > 0) likeCount.toString() else stringResource(R.string.home_feed_like),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF66D6D6)
                    )
                }
            }

            comment?.replies?.takeIf { it.isNotEmpty() }?.let { replies ->
                Text(
                    text = stringResource(R.string.comment_replies_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = TarnishedGold,
                    fontWeight = FontWeight.SemiBold
                )
                replies.forEach { reply ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = reply.user.usuario.ifBlank { reply.user.email }
                                .ifBlank { stringResource(R.string.unknown_username) },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF66D6D6),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = reply.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = OldIvory
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.comment_reply_hint)) },
                    singleLine = true,
                    enabled = canInteract && !isSaving,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val text = replyText.trim()
                            if (text.isNotBlank() && canInteract) {
                                onReplyToComment(notification.activityId, notification.commentId, text)
                                replyText = ""
                            }
                        }
                    ),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )
                IconButton(
                    onClick = {
                        val text = replyText.trim()
                        if (text.isNotBlank() && canInteract) {
                            onReplyToComment(notification.activityId, notification.commentId, text)
                            replyText = ""
                        }
                    },
                    enabled = canInteract && !isSaving && replyText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_reply),
                        tint = if (replyText.isBlank()) OldIvory.copy(alpha = 0.45f) else OldIvory
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginalActivityCard(
    item: FriendActivityItem,
    comments: List<com.example.kaishelvesapp.data.repository.ActivityComment>,
    isLoadingComments: Boolean,
    isSaving: Boolean,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaiUserAvatar(
                    displayName = item.user.usuario.ifBlank { item.user.email },
                    imageUrl = item.user.photoUrl,
                    size = 38.dp
                )

                Spacer(modifier = Modifier.size(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activityTitle(item),
                        style = MaterialTheme.typography.titleMedium,
                        color = OldIvory,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatNotificationActivityTimestamp(item.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory.copy(alpha = 0.74f)
                    )
                }
            }

            activityBookInfo(item)?.let { book ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TarnishedGold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    BookCover(
                        imageUrl = book.imageUrl,
                        title = book.title,
                        modifier = Modifier.size(width = 42.dp, height = 62.dp)
                    )
                }
            }

            ActivitySocialActions(
                item = item,
                postTitle = activityTitle(item),
                postTimestamp = formatNotificationActivityTimestamp(item.timestampMillis),
                comments = comments,
                isLoadingComments = isLoadingComments,
                isSaving = isSaving,
                onToggleLike = onToggleLike,
                onLoadComments = onLoadComments,
                onAddComment = onAddComment
            )
        }
    }
}

@Composable
private fun notificationMessage(notification: ActivityNotificationItem): String {
    val userName = notification.user.usuario
        .ifBlank { notification.user.email }
        .ifBlank { stringResource(R.string.unknown_username) }
    return when (notification.type) {
        ActivityNotificationType.LIKE -> "$userName le ha dado me gusta a tu publicaci\u00f3n"
        ActivityNotificationType.COMMENT -> "$userName ha comentado en tu publicaci\u00f3n."
        ActivityNotificationType.COMMENT_LIKE -> "$userName le ha dado me gusta a tu comentario."
        ActivityNotificationType.COMMENT_REPLY -> "$userName ha respondido a tu comentario."
    }
}

@Composable
private fun NotificationTypeIcon(
    icon: ImageVector,
    tint: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(background, RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

private fun notificationIcon(type: ActivityNotificationType): ImageVector {
    return when (type) {
        ActivityNotificationType.LIKE -> Icons.Filled.Favorite
        ActivityNotificationType.COMMENT -> Icons.Filled.ChatBubbleOutline
        ActivityNotificationType.COMMENT_LIKE -> Icons.Filled.ThumbUp
        ActivityNotificationType.COMMENT_REPLY -> Icons.AutoMirrored.Filled.FormatListBulleted
    }
}

private fun notificationIconTint(type: ActivityNotificationType): Color {
    return when (type) {
        ActivityNotificationType.LIKE -> BloodWine
        ActivityNotificationType.COMMENT -> Color(0xFF0E7C86)
        ActivityNotificationType.COMMENT_LIKE -> Color(0xFF5B6EA6)
        ActivityNotificationType.COMMENT_REPLY -> Color(0xFF0D7C79)
    }
}

@Composable
private fun NotificationActivityPreview(item: FriendActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepWalnut.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (item.type) {
            FriendActivityType.FRIENDSHIP -> {
                KaiUserAvatar(
                    displayName = item.relatedUserName.orEmpty().ifBlank { stringResource(R.string.unknown_username) },
                    imageUrl = "",
                    size = 36.dp
                )
            }
            else -> {
                val book = activityBookInfo(item)
                if (book != null) {
                    BookCover(
                        imageUrl = book.imageUrl,
                        title = book.title,
                        modifier = Modifier.size(width = 32.dp, height = 46.dp)
                    )
                } else {
                    NotificationTypeIcon(
                        icon = publicationIcon(item.type),
                        tint = publicationIconTint(item.type),
                        background = publicationIconTint(item.type).copy(alpha = 0.12f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activityTitle(item),
                style = MaterialTheme.typography.bodySmall,
                color = DeepWalnut,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            activityBookInfo(item)?.let { book ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepWalnut.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun publicationIcon(type: FriendActivityType): ImageVector {
    return when (type) {
        FriendActivityType.FRIENDSHIP -> Icons.Filled.PersonAdd
        FriendActivityType.WANT_TO_READ -> Icons.Filled.BookmarkBorder
        FriendActivityType.READING -> Icons.Filled.AutoStories
        FriendActivityType.READ -> Icons.Filled.CheckCircleOutline
        FriendActivityType.LIST_ADDED -> Icons.AutoMirrored.Filled.FormatListBulleted
    }
}

private fun publicationIconTint(type: FriendActivityType): Color {
    return when (type) {
        FriendActivityType.FRIENDSHIP -> Color(0xFF0E7C86)
        FriendActivityType.WANT_TO_READ -> TarnishedGold
        FriendActivityType.READING -> Color(0xFF0E7C86)
        FriendActivityType.READ -> BloodWine
        FriendActivityType.LIST_ADDED -> DeepWalnut
    }
}

@Composable
private fun activityTitle(item: FriendActivityItem): String {
    val userName = item.user.usuario.ifBlank { stringResource(R.string.unknown_username) }
    return when (item.type) {
        FriendActivityType.FRIENDSHIP -> item.relatedUserName
            ?.takeIf { it.isNotBlank() }
            ?.let { "$userName y $it ahora son amigos" }
            ?: "$userName tiene una nueva amistad"
        FriendActivityType.WANT_TO_READ -> "$userName quiere leer"
        FriendActivityType.READING -> "$userName está leyendo"
        FriendActivityType.READ -> "$userName ha leído"
        FriendActivityType.LIST_ADDED -> "$userName ha actualizado una lista"
    }
}

private data class NotificationActivityBookInfo(
    val title: String,
    val imageUrl: String
)

private fun activityBookInfo(item: FriendActivityItem): NotificationActivityBookInfo? {
    item.book?.let { book ->
        return NotificationActivityBookInfo(
            title = book.titulo.takeIf { it.isNotBlank() } ?: book.isbn,
            imageUrl = book.imagen
        )
    }

    item.readBook?.let { book ->
        return NotificationActivityBookInfo(
            title = book.titulo.takeIf { it.isNotBlank() } ?: book.isbn,
            imageUrl = book.imagen
        )
    }

    return null
}

@Composable
private fun formatNotificationActivityTimestamp(timestampMillis: Long?): String {
    if (timestampMillis == null) {
        return stringResource(R.string.recently_label)
    }

    return remember(timestampMillis) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestampMillis))
    }
}

@Composable
private fun FriendRequestCard(
    request: Usuario,
    onOpenProfile: () -> Unit,
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
            Box(
                modifier = Modifier.clickable(
                    enabled = request.uid.isNotBlank(),
                    onClick = onOpenProfile
                )
            ) {
                KaiUserAvatar(
                    displayName = request.usuario.ifBlank { request.email },
                    imageUrl = request.photoUrl
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            NotificationTypeIcon(
                icon = Icons.Filled.PersonAdd,
                tint = Color(0xFF0E7C86),
                background = Color(0xFF0E7C86).copy(alpha = 0.13f)
            )

            Spacer(modifier = Modifier.size(10.dp))

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
                    text = request.email,
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

