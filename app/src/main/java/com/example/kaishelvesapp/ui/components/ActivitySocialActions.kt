package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivitySocialActions(
    item: FriendActivityItem,
    postTitle: String,
    postTimestamp: String,
    comments: List<ActivityComment>,
    isLoadingComments: Boolean,
    isSaving: Boolean,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showComments by rememberSaveable(item.id) { mutableStateOf(false) }

    if (showComments) {
        ActivityCommentsDialog(
            item = item,
            postTitle = postTitle,
            postTimestamp = postTimestamp,
            comments = comments,
            isLoadingComments = isLoadingComments,
            isSaving = isSaving,
            onDismiss = { showComments = false },
            onLoadComments = onLoadComments,
            onAddComment = onAddComment
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(enabled = !isSaving) { onToggleLike(item.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (item.social.likedByCurrentUser) {
                    stringResource(R.string.home_feed_unlike)
                } else {
                    stringResource(R.string.home_feed_like)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF66D6D6)
            )
            if (item.social.likeCount > 0) {
                Icon(
                    imageVector = Icons.Filled.ThumbUp,
                    contentDescription = stringResource(R.string.likes_count),
                    tint = if (item.social.likedByCurrentUser) Color(0xFF66D6D6) else OldIvory.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.social.likeCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF66D6D6)
                )
            }
        }

        Row(
            modifier = Modifier.clickable {
                showComments = true
                onLoadComments(item.id)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.home_feed_comment),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF66D6D6)
            )
            if (item.social.commentCount > 0) {
                Icon(
                    imageVector = Icons.Filled.ModeComment,
                    contentDescription = stringResource(R.string.comments_count),
                    tint = OldIvory.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.social.commentCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF66D6D6)
                )
            }
        }
    }
}

@Composable
private fun ActivityCommentsDialog(
    item: FriendActivityItem,
    postTitle: String,
    postTimestamp: String,
    comments: List<ActivityComment>,
    isLoadingComments: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var commentText by rememberSaveable(item.id) { mutableStateOf("") }

    LaunchedEffect(item.id) {
        onLoadComments(item.id)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Obsidian.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BloodWine.copy(alpha = 0.28f))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = OldIvory
                        )
                    }
                    Text(
                        text = stringResource(R.string.comments_title),
                        color = OldIvory,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    KaiUserAvatar(
                        displayName = item.user.usuario.ifBlank { item.user.email },
                        imageUrl = item.user.photoUrl,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = postTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = OldIvory,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = postTimestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = OldIvory.copy(alpha = 0.72f)
                        )
                    }
                }

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.22f))

                Text(
                    text = stringResource(R.string.comments_title).uppercase(),
                    color = OldIvory,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 14.dp)
                        .width(156.dp)
                        .height(1.dp)
                        .background(TarnishedGold.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isLoadingComments && comments.isEmpty() -> {
                            CircularProgressIndicator(
                                color = TarnishedGold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        comments.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.no_comments_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OldIvory.copy(alpha = 0.82f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(comments, key = { it.id }) { comment ->
                                    ActivityCommentRow(comment = comment)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.16f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KaiUserAvatar(
                        displayName = "",
                        imageUrl = "",
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.write_comment_hint)) },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val text = commentText.trim()
                                if (text.isNotBlank()) {
                                    onAddComment(item.id, text)
                                    commentText = ""
                                }
                            }
                        ),
                        colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                    )
                    IconButton(
                        onClick = {
                            val text = commentText.trim()
                            if (text.isNotBlank()) {
                                onAddComment(item.id, text)
                                commentText = ""
                            }
                        },
                        enabled = !isSaving && commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send_comment),
                            tint = if (commentText.isBlank()) OldIvory.copy(alpha = 0.45f) else OldIvory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCommentRow(comment: ActivityComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        KaiUserAvatar(
            displayName = comment.user.usuario.ifBlank { comment.user.email },
            imageUrl = comment.user.photoUrl,
            size = 38.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF66D6D6),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCommentTimestamp(comment.timestampMillis),
                style = MaterialTheme.typography.bodySmall,
                color = OldIvory.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun formatCommentTimestamp(timestampMillis: Long?): String {
    if (timestampMillis == null) return stringResource(R.string.recently_label)

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "es") {
        "d MMM HH:mm"
    } else {
        "MMM d HH:mm"
    }

    return SimpleDateFormat(pattern, locale).format(Date(timestampMillis))
}
