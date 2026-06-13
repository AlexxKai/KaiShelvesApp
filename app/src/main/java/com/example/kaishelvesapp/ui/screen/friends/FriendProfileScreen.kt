package com.example.kaishelvesapp.ui.screen.friends

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.data.repository.FriendActivityType
import com.example.kaishelvesapp.data.repository.FriendProfileData
import com.example.kaishelvesapp.data.repository.FriendShelfBookItem
import com.example.kaishelvesapp.data.repository.FriendShelfPreview
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.example.kaishelvesapp.ui.components.ActivitySocialActions
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.util.localizedName
import com.example.kaishelvesapp.ui.viewmodel.FriendProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileScreen(
    friendUid: String,
    viewModel: FriendProfileViewModel,
    onBack: () -> Unit,
    onOpenFriendProfile: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit,
    onFriendshipChanged: () -> Unit = {},
    onOpenFriendLists: (String, String) -> Unit = { _, _ -> },
    onOpenBook: (Libro) -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(friendUid) {
        viewModel.loadProfile(friendUid)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FriendProfileTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                onBack = onBack,
                notificationCount = pendingRequestCount,
                onOpenNotifications = onOpenNotifications
            )
        },
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.FRIENDS,
                onSelect = onSectionSelected
            )
        }
        ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadProfile(friendUid, refresh = true) }
        ) {
            GothicBackground(
                modifier = Modifier.fillMaxSize()
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

                    uiState.profile != null -> {
                        FriendProfileContent(
                            profile = uiState.profile!!,
                            isRemovingFriend = uiState.isRemovingFriend,
                            isSendingRequest = uiState.isSendingRequest,
                            isRespondingRequest = uiState.isRespondingRequest,
                            isBlockingMember = uiState.isBlockingMember,
                            isSubmittingReport = uiState.isSubmittingReport,
                            onRemoveFriend = {
                                viewModel.removeFriend(friendUid) {
                                    onFriendshipChanged()
                                }
                            },
                            onSendFriendRequest = {
                                viewModel.sendFriendRequest {
                                    onFriendshipChanged()
                                }
                            },
                            onCancelSentFriendRequest = {
                                viewModel.cancelSentFriendRequest {
                                    onFriendshipChanged()
                                }
                            },
                            onAcceptFriendRequest = {
                                viewModel.acceptFriendRequest {
                                    onFriendshipChanged()
                                }
                            },
                            onRejectFriendRequest = {
                                viewModel.rejectFriendRequest {
                                    onFriendshipChanged()
                                }
                            },
                            onBlockMember = {
                                viewModel.blockCurrentProfile {
                                    onFriendshipChanged()
                                    onBack()
                                }
                            },
                            onSubmitReport = viewModel::submitReport,
                            onOpenFriendLists = onOpenFriendLists,
                            onOpenFriendProfile = onOpenFriendProfile,
                            onOpenBook = onOpenBook,
                            commentsByActivityId = uiState.commentsByActivityId,
                            loadingCommentIds = uiState.loadingCommentIds,
                            socialActionIds = uiState.socialActionIds,
                            onToggleLike = viewModel::toggleLike,
                            onLoadComments = viewModel::loadComments,
                            onAddComment = viewModel::addComment,
                            onToggleCommentLike = viewModel::toggleCommentLike,
                            onReplyToComment = viewModel::replyToComment
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.errorMessage ?: stringResource(R.string.friend_profile_load_error),
                                style = MaterialTheme.typography.titleMedium,
                                color = OldIvory,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendProfileTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    notificationCount: Int,
    onOpenNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepWalnut.copy(alpha = 0.98f),
                        Obsidian.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(stringResource(R.string.search_by_title_author_isbn))
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                leadingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = TarnishedGold
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = TarnishedGold
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
            )

            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = onOpenNotifications) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = stringResource(R.string.open_notifications_center),
                        tint = TarnishedGold
                    )
                }

                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, end = 6.dp)
                            .background(BloodWine, RoundedCornerShape(999.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notificationCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OldIvory
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendProfileContent(
    profile: FriendProfileData,
    isRemovingFriend: Boolean,
    isSendingRequest: Boolean,
    isRespondingRequest: Boolean = false,
    isBlockingMember: Boolean = false,
    isSubmittingReport: Boolean = false,
    onRemoveFriend: () -> Unit,
    onSendFriendRequest: () -> Unit,
    onCancelSentFriendRequest: () -> Unit = {},
    onAcceptFriendRequest: () -> Unit = {},
    onRejectFriendRequest: () -> Unit = {},
    onBlockMember: () -> Unit = {},
    onSubmitReport: (String, String, List<String>, () -> Unit) -> Unit = { _, _, _, _ -> },
    onOpenFriendLists: (String, String) -> Unit,
    onOpenFriendProfile: (String) -> Unit,
    onOpenBook: (Libro) -> Unit,
    commentsByActivityId: Map<String, List<ActivityComment>>,
    loadingCommentIds: Set<String>,
    socialActionIds: Set<String>,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleCommentLike: (String, String) -> Unit,
    onReplyToComment: (String, String, String) -> Unit,
    deletingActivityIds: Set<String> = emptySet(),
    onDeleteActivityUpdate: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    showFriendActions: Boolean = true,
    showActivityDeleteActions: Boolean = false,
    isScrollable: Boolean = true,
    contentHorizontalPadding: Dp = 16.dp,
    transparentCards: Boolean = true
) {
    val contentModifier = if (isScrollable) {
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = contentModifier
            .padding(horizontal = contentHorizontalPadding, vertical = 14.dp)
    ) {
        FriendProfileHero(
            profile = profile,
            transparentBackground = transparentCards
        )
        if (showFriendActions) {
            Spacer(modifier = Modifier.height(18.dp))
            FriendProfileMenuRow(
                isFriend = profile.isFriend,
                isRequestSent = profile.isRequestSent,
                isRequestReceived = profile.isRequestReceived,
                isRemovingFriend = isRemovingFriend,
                isSendingRequest = isSendingRequest,
                isRespondingRequest = isRespondingRequest,
                isBlockingMember = isBlockingMember,
                isSubmittingReport = isSubmittingReport,
                onRemoveFriend = onRemoveFriend,
                onSendFriendRequest = onSendFriendRequest,
                onCancelSentFriendRequest = onCancelSentFriendRequest,
                onAcceptFriendRequest = onAcceptFriendRequest,
                onRejectFriendRequest = onRejectFriendRequest,
                onBlockMember = onBlockMember,
                onSubmitReport = onSubmitReport
            )
        }
        Spacer(modifier = Modifier.height(18.dp))

        if (profile.isPrivateProfile) {
            PrivateProfileSection(transparentBackground = transparentCards)
        } else {
            FriendShelvesCarouselSection(
                title = stringResource(R.string.books_count, profile.booksReadCount),
                shelves = profile.predefinedShelves,
                friendUid = profile.user.uid,
                friendName = profile.user.usuario.ifBlank { profile.user.email },
                onOpenFriendLists = onOpenFriendLists,
                onOpenBook = onOpenBook,
                transparentBackground = transparentCards
            )
            Spacer(modifier = Modifier.height(18.dp))

            FriendsPreviewSection(
                friends = profile.friendPreviews,
                friendsCount = profile.friendsCount,
                onOpenFriendProfile = onOpenFriendProfile,
                transparentBackground = transparentCards
            )
            Spacer(modifier = Modifier.height(18.dp))
            GroupsSection(
                user = profile.user,
                groupsCount = profile.groupsCount,
                transparentBackground = transparentCards
            )
            Spacer(modifier = Modifier.height(18.dp))
            UpdatesSection(
                profile = profile,
                commentsByActivityId = commentsByActivityId,
                loadingCommentIds = loadingCommentIds,
                socialActionIds = socialActionIds,
                onToggleLike = onToggleLike,
                onLoadComments = onLoadComments,
                onAddComment = onAddComment,
                onToggleCommentLike = onToggleCommentLike,
                onReplyToComment = onReplyToComment,
                deletingActivityIds = deletingActivityIds,
                onDeleteActivityUpdate = onDeleteActivityUpdate,
                showActivityDeleteActions = showActivityDeleteActions,
                onOpenBook = onOpenBook,
                transparentBackground = transparentCards
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FriendProfileHero(
    profile: FriendProfileData,
    transparentBackground: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (transparentBackground) {
                        Modifier.background(Color.Transparent)
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BloodWine.copy(alpha = 0.16f),
                                    Obsidian.copy(alpha = 0.96f)
                                )
                            )
                        )
                    }
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KaiUserAvatar(
                displayName = profile.user.usuario.ifBlank { profile.user.email },
                imageUrl = profile.user.photoUrl
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = profile.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                style = MaterialTheme.typography.headlineMedium,
                color = OldIvory
            )

            if (profile.user.isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                AdminProfileBadge()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = friendProfileStats(profile.booksReadCount, profile.friendsCount),
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun AdminProfileBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TarnishedGold.copy(alpha = 0.28f),
                        BloodWine.copy(alpha = 0.54f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            tint = TarnishedGold,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(R.string.admin_badge),
            style = MaterialTheme.typography.labelMedium,
            color = TarnishedGold,
            maxLines = 1
        )
    }
}

@Composable
private fun FriendProfileMenuRow(
    isFriend: Boolean,
    isRequestSent: Boolean,
    isRequestReceived: Boolean,
    isRemovingFriend: Boolean,
    isSendingRequest: Boolean,
    isRespondingRequest: Boolean,
    isBlockingMember: Boolean,
    isSubmittingReport: Boolean,
    onRemoveFriend: () -> Unit,
    onSendFriendRequest: () -> Unit,
    onCancelSentFriendRequest: () -> Unit,
    onAcceptFriendRequest: () -> Unit,
    onRejectFriendRequest: () -> Unit,
    onBlockMember: () -> Unit,
    onSubmitReport: (String, String, List<String>, () -> Unit) -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportSuccessDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRemovingFriend) {
                    showRemoveDialog = false
                }
            },
            title = {
                Text(text = stringResource(R.string.remove_friend_title))
            },
            text = {
                Text(text = stringResource(R.string.remove_friend_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveFriend()
                        showRemoveDialog = false
                    },
                    enabled = !isRemovingFriend
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = false },
                    enabled = !isRemovingFriend
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            containerColor = Obsidian,
            titleContentColor = OldIvory,
            textContentColor = OldIvory.copy(alpha = 0.9f)
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBlockingMember) {
                    showBlockDialog = false
                }
            },
            title = {
                Text(text = stringResource(R.string.block_member_title))
            },
            text = {
                Text(text = stringResource(R.string.block_member_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBlockMember()
                        showBlockDialog = false
                    },
                    enabled = !isBlockingMember
                ) {
                    Text(text = stringResource(R.string.block_member))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockDialog = false },
                    enabled = !isBlockingMember
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            containerColor = Obsidian,
            titleContentColor = OldIvory,
            textContentColor = OldIvory.copy(alpha = 0.9f)
        )
    }

    if (showReportDialog) {
        ReportAccountDialog(
            isSubmitting = isSubmittingReport,
            onDismiss = { showReportDialog = false },
            onSubmit = { subject, message, photoUris ->
                onSubmitReport(subject, message, photoUris) {
                    showReportDialog = false
                    showReportSuccessDialog = true
                }
            }
        )
    }

    if (showReportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showReportSuccessDialog = false },
            title = {
                Text(text = stringResource(R.string.report_sent_title))
            },
            text = {
                Text(text = stringResource(R.string.report_sent_message))
            },
            confirmButton = {
                TextButton(onClick = { showReportSuccessDialog = false }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            containerColor = Obsidian,
            titleContentColor = OldIvory,
            textContentColor = OldIvory.copy(alpha = 0.9f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isFriend && isRequestReceived) {
            Row(
                modifier = Modifier.clickable(
                    enabled = !isRespondingRequest,
                    onClick = onAcceptFriendRequest
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF66D6D6),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.accept_request),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF66D6D6)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Row(
                modifier = Modifier.clickable(
                    enabled = !isRespondingRequest,
                    onClick = onRejectFriendRequest
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = OldIvory.copy(alpha = 0.82f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.titleMedium,
                    color = OldIvory.copy(alpha = 0.82f)
                )
            }
        } else {
            Row(
                modifier = Modifier.clickable(
                    enabled = if (isFriend) !isRemovingFriend else !isSendingRequest
                ) {
                    if (isFriend) {
                        showRemoveDialog = true
                    } else if (isRequestSent) {
                        onCancelSentFriendRequest()
                    } else {
                        onSendFriendRequest()
                    }
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFriend) Icons.Filled.Check else Icons.Filled.PersonAddAlt1,
                    contentDescription = null,
                    tint = if (isFriend) OldIvory else Color(0xFF66D6D6),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFriend) {
                        stringResource(R.string.friends)
                    } else if (isRequestSent) {
                        stringResource(R.string.friend_request_pending)
                    } else {
                        stringResource(R.string.add_friend_short)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRequestSent) {
                        OldIvory.copy(alpha = 0.72f)
                    } else {
                        Color(0xFF66D6D6)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Box {
            Row(
                modifier = Modifier.clickable { showMoreMenu = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = null,
                    tint = OldIvory,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.more_option),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF66D6D6)
                )
            }

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                containerColor = Obsidian
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.report_account),
                            color = OldIvory
                        )
                    },
                    onClick = {
                        showMoreMenu = false
                        showReportDialog = true
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.block_member),
                            color = OldIvory
                        )
                    },
                    onClick = {
                        showMoreMenu = false
                        showBlockDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportAccountDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, List<String>) -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        photoUris = uris.map { it.toString() }
    }
    val canSubmit = subject.isNotBlank() && message.isNotBlank() && !isSubmitting

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },
        title = {
            Text(text = stringResource(R.string.report_account))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.report_subject)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.report_message)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    enabled = !isSubmitting,
                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                ) {
                    Text(text = stringResource(R.string.report_add_photos))
                }
                if (photoUris.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.report_photos_selected, photoUris.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory.copy(alpha = 0.82f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val encodedPhotos = photoUris.mapNotNull { photoUri ->
                        runCatching {
                            if (photoUri.startsWith("content://")) {
                                ProfileImageCodec.encodeImageAsDataUri(context, Uri.parse(photoUri))
                            } else {
                                photoUri
                            }
                        }.getOrNull()
                    }
                    onSubmit(subject, message, encodedPhotos)
                },
                enabled = canSubmit
            ) {
                Text(text = stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        containerColor = Obsidian,
        titleContentColor = OldIvory,
        textContentColor = OldIvory.copy(alpha = 0.9f)
    )
}

@Composable
private fun FriendProfileMenuRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.friends),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF66D6D6)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = "•••",
            style = MaterialTheme.typography.titleMedium,
            color = OldIvory
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = stringResource(R.string.more_option),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF66D6D6)
        )
    }
}

@Composable
private fun FriendShelvesCarouselSection(
    title: String,
    shelves: List<FriendShelfPreview>,
    friendUid: String,
    friendName: String,
    onOpenFriendLists: (String, String) -> Unit,
    onOpenBook: (Libro) -> Unit,
    transparentBackground: Boolean = false
) {
    val visibleShelves = shelves.filter { it.bookCount > 0 || it.books.isNotEmpty() }

    ProfileSectionCard(
        title = title,
        transparentBackground = transparentBackground
    ) {
        if (visibleShelves.isEmpty()) {
            Text(
                text = stringResource(R.string.no_books_in_friend_shelves),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                visibleShelves.forEach { shelf ->
                    item(key = "shelf_${shelf.listId}") {
                        FriendShelfRow(
                            shelf = shelf,
                            onOpenBook = onOpenBook
                        )
                    }
                }

                item(key = "see_more") {
                    SeeMoreShelfTile(
                        onClick = { onOpenFriendLists(friendUid, friendName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendShelfRow(
    shelf: FriendShelfPreview,
    onOpenBook: (Libro) -> Unit
) {
    val visibleBooks = shelf.books.take(4)
    val rowWidth = 84.dp * visibleBooks.size + 14.dp * (visibleBooks.size - 1).coerceAtLeast(0)

    Column(
        modifier = Modifier.width(rowWidth)
    ) {
        Text(
            text = shelf.localizedName(),
            style = MaterialTheme.typography.titleMedium,
            color = OldIvory
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            visibleBooks.forEach { item ->
                FriendShelfBookTile(
                    item = item,
                    isReadList = shelf.isReadList,
                    onOpenBook = onOpenBook
                )
            }
        }
    }
}

@Composable
private fun FriendShelfBookTile(
    item: FriendShelfBookItem,
    isReadList: Boolean,
    onOpenBook: (Libro) -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable { onOpenBook(item.book) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BookCover(
            imageUrl = item.book.imagen,
            title = item.book.titulo,
            modifier = Modifier
                .width(84.dp)
                .height(126.dp),
            containerColor = Color.Transparent,
            borderColor = Color.Transparent
        )

        if (isReadList) {
            Spacer(modifier = Modifier.height(8.dp))
            RatingStars(
                rating = (item.rating ?: 0).coerceIn(0, 5),
                iconSize = 13.dp
            )
        }
    }
}

@Composable
private fun SeeMoreShelfTile(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(34.dp))

        Box(
            modifier = Modifier
                .width(84.dp)
                .height(126.dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, OldIvory.copy(alpha = 0.38f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = OldIvory,
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.see_more),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendsPreviewSection(
    friends: List<Usuario>,
    friendsCount: Int,
    onOpenFriendProfile: (String) -> Unit,
    transparentBackground: Boolean = false
) {
    ProfileSectionCard(
        title = if (friendsCount == 1) {
            stringResource(R.string.one_friend)
        } else {
            stringResource(R.string.friends_count, friendsCount)
        },
        transparentBackground = transparentBackground
    ) {
        if (friends.isEmpty()) {
            Text(
                text = stringResource(R.string.no_visible_friends),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(friends) { friend ->
                    Column(
                        modifier = Modifier.clickable(
                            enabled = friend.uid.isNotBlank(),
                            onClick = { onOpenFriendProfile(friend.uid) }
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KaiUserAvatar(
                            displayName = friend.usuario.ifBlank { friend.email },
                            imageUrl = friend.photoUrl
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = friend.usuario.ifBlank { stringResource(R.string.unknown_username) },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF66D6D6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsSection(
    user: Usuario,
    groupsCount: Int,
    transparentBackground: Boolean = false
) {
    ProfileSectionCard(
        title = stringResource(R.string.groups_count, groupsCount),
        transparentBackground = transparentBackground
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color.Transparent, RoundedCornerShape(14.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = OldIvory,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(
                    R.string.no_groups_member_message,
                    user.usuario.ifBlank { stringResource(R.string.unknown_username) }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrivateProfileSection(
    transparentBackground: Boolean = false
) {
    ProfileSectionCard(
        title = stringResource(R.string.private_profile_title),
        transparentBackground = transparentBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = TarnishedGold,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.private_profile_message),
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UpdatesSection(
    profile: FriendProfileData,
    commentsByActivityId: Map<String, List<ActivityComment>>,
    loadingCommentIds: Set<String>,
    socialActionIds: Set<String>,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleCommentLike: (String, String) -> Unit,
    onReplyToComment: (String, String, String) -> Unit,
    deletingActivityIds: Set<String>,
    onDeleteActivityUpdate: (String) -> Unit,
    showActivityDeleteActions: Boolean,
    onOpenBook: (Libro) -> Unit,
    transparentBackground: Boolean = false
) {
    ProfileSectionCard(
        title = stringResource(R.string.updates_title),
        transparentBackground = transparentBackground
    ) {
        if (profile.updates.isEmpty()) {
            Text(
                text = stringResource(R.string.no_updates_available),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                profile.updates.forEach { item ->
                    UpdateItem(
                        item = item,
                        comments = commentsByActivityId[item.id].orEmpty(),
                        isLoadingComments = item.id in loadingCommentIds,
                        isSocialActionRunning = item.id in socialActionIds,
                        onToggleLike = onToggleLike,
                        onLoadComments = onLoadComments,
                        onAddComment = onAddComment,
                        onToggleCommentLike = onToggleCommentLike,
                        onReplyToComment = onReplyToComment,
                        isDeleting = item.id in deletingActivityIds,
                        canDelete = showActivityDeleteActions,
                        onDeleteActivityUpdate = onDeleteActivityUpdate,
                        onOpenBook = onOpenBook
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateItem(
    item: FriendActivityItem,
    comments: List<ActivityComment>,
    isLoadingComments: Boolean,
    isSocialActionRunning: Boolean,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleCommentLike: (String, String) -> Unit,
    onReplyToComment: (String, String, String) -> Unit,
    isDeleting: Boolean,
    canDelete: Boolean,
    onDeleteActivityUpdate: (String) -> Unit,
    onOpenBook: (Libro) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 300.dp
        val avatarSize = if (compact) 46.dp else 52.dp
        val horizontalGap = if (compact) 10.dp else 12.dp
        val titleStyle = if (compact) {
            MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 21.sp)
        } else {
            MaterialTheme.typography.titleMedium
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            KaiUserAvatar(
                displayName = item.user.usuario.ifBlank { item.user.email },
                imageUrl = item.user.photoUrl,
                modifier = Modifier.size(avatarSize),
                size = avatarSize
            )

            Spacer(modifier = Modifier.width(horizontalGap))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val title = when (item.type) {
                    FriendActivityType.FRIENDSHIP -> stringResource(
                        R.string.friendship_update_text,
                        item.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                        item.relatedUserName.orEmpty()
                    )
                    FriendActivityType.WANT_TO_READ -> stringResource(
                        R.string.friend_wants_to_read,
                        item.user.usuario.ifBlank { stringResource(R.string.unknown_username) }
                    )
                    FriendActivityType.READING -> stringResource(
                        R.string.friend_is_reading,
                        item.user.usuario.ifBlank { stringResource(R.string.unknown_username) }
                    )
                    FriendActivityType.READ -> stringResource(
                        R.string.friend_has_read,
                        item.user.usuario.ifBlank { stringResource(R.string.unknown_username) }
                    )
                    FriendActivityType.LIST_ADDED -> stringResource(
                        R.string.friend_added_to_list,
                        item.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                        item.listName.orEmpty()
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!isDeleting) {
                                showDeleteDialog = false
                            }
                        },
                        title = {
                            Text(text = stringResource(R.string.delete_activity_update_title))
                        },
                        text = {
                            Text(text = stringResource(R.string.delete_activity_update_message))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDeleteActivityUpdate(item.id)
                                    showDeleteDialog = false
                                },
                                enabled = !isDeleting
                            ) {
                                Text(text = stringResource(R.string.delete))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteDialog = false },
                                enabled = !isDeleting
                            ) {
                                Text(text = stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = titleStyle,
                        color = OldIvory,
                        modifier = Modifier.weight(1f)
                    )

                    if (canDelete) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isDeleting,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.delete_activity_update),
                                tint = Color(0xFF66D6D6)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val formattedTimestamp = formatActivityTimestamp(item.timestampMillis)

                Text(
                    text = formattedTimestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = OldIvory.copy(alpha = 0.72f)
                )

                val book = item.book ?: item.readBook?.let {
                    Libro(
                        id = it.id,
                        isbn = it.isbn,
                        titulo = it.titulo,
                        autor = it.autor,
                        editorial = it.editorial,
                        genero = it.genero,
                        fechaPublicacion = it.fechaPublicacion,
                        paginas = it.paginas,
                        imagen = it.imagen,
                        pdf = it.pdf
                    )
                }

                if (book != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ActivityBookCard(
                        book = book,
                        onOpenBook = onOpenBook
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ActivitySocialActions(
                    item = item,
                    postTitle = title,
                    postTimestamp = formattedTimestamp,
                    comments = comments,
                    isLoadingComments = isLoadingComments,
                    isSaving = isSocialActionRunning,
                    onToggleLike = onToggleLike,
                    onLoadComments = onLoadComments,
                    onAddComment = onAddComment,
                    onToggleCommentLike = onToggleCommentLike,
                    onReplyToComment = onReplyToComment
                )
            }
        }
    }
}

@Composable
private fun ActivityBookCard(
    book: Libro,
    onOpenBook: (Libro) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenBook(book) }
    ) {
        val compact = maxWidth < 230.dp
        val coverWidth = if (compact) 74.dp else 86.dp
        val coverHeight = if (compact) 108.dp else 126.dp
        val gap = if (compact) 10.dp else 14.dp
        val titleStyle = if (compact) {
            MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 21.sp)
        } else {
            MaterialTheme.typography.titleLarge.copy(lineHeight = 27.sp)
        }
        val authorStyle = if (compact) {
            MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp)
        } else {
            MaterialTheme.typography.bodyLarge
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            BookCover(
                imageUrl = book.imagen,
                title = book.titulo,
                modifier = Modifier
                    .width(coverWidth)
                    .height(coverHeight)
            )

            Spacer(modifier = Modifier.width(gap))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.titulo.ifBlank { stringResource(R.string.unknown_title) },
                    style = titleStyle,
                    color = OldIvory
                )

                if (book.autor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.book_by_author, book.autor),
                        style = authorStyle,
                        color = Color(0xFF66D6D6)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    transparentBackground: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 330.dp
        val horizontalPadding = if (compact) 12.dp else 16.dp
        val titleStyle = if (compact) {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.headlineMedium
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (transparentBackground) {
                            Color.Transparent
                        } else {
                            Obsidian.copy(alpha = 0.95f)
                        }
                    )
                    .padding(horizontal = horizontalPadding, vertical = 18.dp)
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = OldIvory,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(160.dp)
                        .height(2.dp)
                        .background(TarnishedGold.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
private fun friendProfileStats(booksReadCount: Int, friendsCount: Int): String {
    val booksLabel = if (booksReadCount == 1) {
        stringResource(R.string.one_book)
    } else {
        stringResource(R.string.books_count, booksReadCount)
    }
    val friendsLabel = if (friendsCount == 1) {
        stringResource(R.string.one_friend)
    } else {
        stringResource(R.string.friends_count, friendsCount)
    }
    return "$booksLabel • $friendsLabel"
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

