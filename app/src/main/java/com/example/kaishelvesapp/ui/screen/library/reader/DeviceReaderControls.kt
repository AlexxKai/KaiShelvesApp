package com.example.kaishelvesapp.ui.screen.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PdfReaderTopControls(
    title: String,
    onDismiss: () -> Unit,
    onTitleClick: () -> Unit,
    onOpenThemeSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151515).copy(alpha = 0.96f))
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTitleClick)
                    .padding(vertical = 10.dp)
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_option),
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF252525))
                ) {
                    listOf(
                        R.string.reader_menu_visual_options to {},
                        R.string.reader_menu_control_options to {},
                        R.string.reader_menu_other_settings to {},
                        R.string.reader_menu_themes to onOpenThemeSettings,
                        R.string.reader_menu_name_replacement to {},
                        R.string.reader_menu_more_operations to {},
                        R.string.reader_menu_share to {},
                        R.string.reader_menu_book_info to {}
                    ).forEach { (labelRes, action) ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(labelRes), color = Color.White) },
                            onClick = {
                                menuExpanded = false
                                action()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfReaderBottomControls(
    currentPage: Int,
    pageCount: Int,
    progress: Int,
    showReset: Boolean,
    onPageSelected: (Int) -> Unit,
    onResetPage: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onOpenTextSizeSettings: () -> Unit,
    annotationsCount: Int,
    onAddBookmark: () -> Unit,
    onAddHighlight: () -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bookmarkPulseTick by remember { mutableStateOf(0) }
    var bookmarkPulseActive by remember { mutableStateOf(false) }

    LaunchedBookmarkPulse(
        pulseTick = bookmarkPulseTick,
        onPulseChange = { bookmarkPulseActive = it }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF151515).copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
            Slider(
                value = (currentPage + 1).toFloat(),
                onValueChange = { value ->
                    onPageSelected(value.roundToInt() - 1)
                },
                valueRange = 1f..pageCount.toFloat().coerceAtLeast(2f),
                enabled = pageCount > 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            if (showReset) {
                IconButton(
                    onClick = onResetPage,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = stringResource(R.string.reader_return_to_previous_page),
                        tint = Color(0xFFEBC7E8)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderToolButton(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.reader_read_aloud))
            Box {
                ReaderToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, stringResource(R.string.reader_chapters_and_bookmarks), onOpenNavigation)
                if (annotationsCount > 0) {
                    Text(
                        text = annotationsCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedGold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
            ReaderToolButton(Icons.Filled.Brightness6, stringResource(R.string.reader_brightness), onOpenDisplaySettings)
            ReaderToolButton(Icons.Filled.FormatSize, stringResource(R.string.reader_text_size), onOpenTextSizeSettings)
            ReaderToolButton(
                icon = Icons.Filled.BookmarkBorder,
                contentDescription = stringResource(R.string.reader_add_bookmark),
                onClick = {
                    onAddBookmark()
                    bookmarkPulseTick += 1
                },
                pulseActive = bookmarkPulseActive
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderToolButton(Icons.AutoMirrored.Filled.FormatAlignLeft, stringResource(R.string.reader_alignment))
            ReaderToolButton(Icons.Filled.Star, stringResource(R.string.reader_highlight_selection), onAddHighlight)
            ReaderToolButton(Icons.Filled.MoreHoriz, stringResource(R.string.reader_add_note), onAddNote)
            ReaderToolButton(Icons.Filled.TableRows, stringResource(R.string.reader_page_view))
        }
    }
}

@Composable
fun ReaderToolButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    pulseActive: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (pulseActive) 1.38f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "readerToolButtonScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pulseActive) 0.34f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "readerToolButtonGlow"
    )
    val tint by animateColorAsState(
        targetValue = when {
            pulseActive -> Color(0xFFFFD166)
            enabled -> Color(0xFFEBC7E8)
            else -> Color.White.copy(alpha = 0.34f)
        },
        animationSpec = tween(durationMillis = 140),
        label = "readerToolButtonTint"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .scale(scale)
            .background(TarnishedGold.copy(alpha = glowAlpha), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
private fun LaunchedBookmarkPulse(
    pulseTick: Int,
    onPulseChange: (Boolean) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(pulseTick) {
        if (pulseTick <= 0) return@LaunchedEffect
        onPulseChange(true)
        delay(230)
        onPulseChange(false)
    }
}

@Composable
fun PdfReaderFloatingControls(
    scrollOrientation: PdfReaderScrollOrientation,
    movementLocked: Boolean,
    onToggleScrollOrientation: () -> Unit,
    onToggleMovementLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReaderFloatingButton(
            icon = if (scrollOrientation == PdfReaderScrollOrientation.Horizontal) {
                Icons.Filled.SwapHoriz
            } else {
                Icons.Filled.SwapVert
            },
            contentDescription = if (scrollOrientation == PdfReaderScrollOrientation.Horizontal) {
                stringResource(R.string.reader_pdf_scroll_horizontal)
            } else {
                stringResource(R.string.reader_pdf_scroll_vertical)
            },
            onClick = onToggleScrollOrientation
        )
        ReaderFloatingButton(
            icon = if (movementLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = if (movementLocked) {
                stringResource(R.string.reader_pdf_unlock_movement)
            } else {
                stringResource(R.string.reader_pdf_lock_movement)
            },
            onClick = onToggleMovementLock
        )
    }
}

@Composable
private fun ReaderFloatingButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .background(Color(0xCCF2F0DE), CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF5F5A48),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun ReflowReaderBottomControls(
    currentPage: Int,
    pageCount: Int,
    progress: Int,
    subtitle: String,
    onPageSelected: (Int) -> Unit,
    annotationsCount: Int,
    onOpenTextSizeSettings: () -> Unit,
    onOpenNavigation: () -> Unit,
    onAddBookmark: () -> Unit,
    onAddHighlight: () -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bookmarkPulseTick by remember { mutableStateOf(0) }
    var bookmarkPulseActive by remember { mutableStateOf(false) }

    LaunchedBookmarkPulse(
        pulseTick = bookmarkPulseTick,
        onPulseChange = { bookmarkPulseActive = it }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF151515).copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TarnishedGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
            Slider(
                value = (currentPage + 1).toFloat(),
                onValueChange = { value -> onPageSelected(value.roundToInt() - 1) },
                valueRange = 1f..pageCount.toFloat().coerceAtLeast(2f),
                enabled = pageCount > 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderToolButton(Icons.Filled.FormatSize, stringResource(R.string.reader_text_size), onOpenTextSizeSettings)
            ReaderToolButton(
                icon = Icons.Filled.BookmarkBorder,
                contentDescription = stringResource(R.string.reader_add_bookmark),
                onClick = {
                    onAddBookmark()
                    bookmarkPulseTick += 1
                },
                pulseActive = bookmarkPulseActive
            )
            ReaderToolButton(Icons.Filled.Star, stringResource(R.string.reader_highlight_page), onAddHighlight)
            ReaderToolButton(Icons.Filled.MoreHoriz, stringResource(R.string.reader_add_note), onAddNote)
            Box {
                ReaderToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, stringResource(R.string.reader_chapters_and_bookmarks), onOpenNavigation)
                if (annotationsCount > 0) {
                    Text(
                        text = annotationsCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedGold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}



