package com.example.kaishelvesapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun KaiPrimaryTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onOpenMenu: () -> Unit,
    notificationCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scanOptions = remember {
        ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.EAN_13, ScanOptions.EAN_8, ScanOptions.UPC_A, ScanOptions.UPC_E)
            setPrompt(context.getString(R.string.scan_isbn_prompt))
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
    }
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedValue = result.contents
            .orEmpty()
            .trim()
            .replace("-", "")
            .replace(" ", "")

        when {
            scannedValue.isBlank() -> Unit
            scannedValue.length == 10 || scannedValue.length == 13 -> onScanResult(scannedValue)
            else -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.invalid_isbn_barcode),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            barcodeLauncher.launch(scanOptions)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.camera_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchScanner() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            barcodeLauncher.launch(scanOptions)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.open_navigation_menu),
                    tint = TarnishedGold
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedGold
            )

            if (onOpenNotifications != null) {
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
                                .offset(x = (-6).dp, y = 6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(BloodWine)
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
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = TarnishedGold
                            )
                        }
                    }

                    IconButton(onClick = ::launchScanner) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.scan_isbn),
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
    }
}

@Composable
fun KaiNavigationDrawerContent(
    currentSection: KaiSection,
    subtitle: String,
    userName: String = "",
    profileImageUrl: String = "",
    expanded: Boolean,
    disabledSections: Set<KaiSection> = emptySet(),
    onDisabledSectionClick: ((KaiSection) -> Unit)? = null,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val guestUiRestrictions = LocalGuestUiRestrictions.current
    val effectiveDisabledSections = if (disabledSections.isEmpty()) {
        guestUiRestrictions.disabledSections
    } else {
        disabledSections
    }
    val effectiveDisabledClick = onDisabledSectionClick ?: guestUiRestrictions.onBlockedSectionClick
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxWidth(0.68f)
            .widthIn(max = 420.dp),
        drawerContainerColor = DeepWalnut,
        drawerContentColor = OldIvory
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KaiDrawerHeaderCard(
                subtitle = subtitle,
                userName = userName,
                profileImageUrl = profileImageUrl,
                expanded = expanded,
                onGoToProfile = onGoToProfile
            )

            KaiDrawerItem(
                label = stringResource(R.string.library),
                selected = currentSection == KaiSection.LIBRARY,
                leadingIcon = { Icon(Icons.Filled.LocalLibrary, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.LIBRARY) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.reading_statistics),
                selected = currentSection == KaiSection.STATS,
                leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.STATS) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.friends),
                selected = currentSection == KaiSection.FRIENDS,
                disabled = effectiveDisabledSections.contains(KaiSection.FRIENDS),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Groups,
                        contentDescription = null,
                        tint = if (effectiveDisabledSections.contains(KaiSection.FRIENDS)) {
                            TarnishedGold.copy(alpha = 0.55f)
                        } else {
                            TarnishedGold
                        }
                    )
                },
                onClick = {
                    if (effectiveDisabledSections.contains(KaiSection.FRIENDS)) {
                        effectiveDisabledClick?.invoke(KaiSection.FRIENDS)
                    } else {
                        onSectionSelected(KaiSection.FRIENDS)
                    }
                }
            )

            KaiDrawerItem(
                label = stringResource(R.string.groups),
                selected = currentSection == KaiSection.GROUPS,
                disabled = effectiveDisabledSections.contains(KaiSection.GROUPS),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Handshake,
                        contentDescription = null,
                        tint = if (effectiveDisabledSections.contains(KaiSection.GROUPS)) {
                            TarnishedGold.copy(alpha = 0.55f)
                        } else {
                            TarnishedGold
                        }
                    )
                },
                onClick = {
                    if (effectiveDisabledSections.contains(KaiSection.GROUPS)) {
                        effectiveDisabledClick?.invoke(KaiSection.GROUPS)
                    } else {
                        onSectionSelected(KaiSection.GROUPS)
                    }
                }
            )

            KaiDrawerItem(
                label = stringResource(R.string.reading_challenges),
                selected = currentSection == KaiSection.CHALLENGES,
                leadingIcon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.CHALLENGES) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.selected_for_you),
                selected = currentSection == KaiSection.FOR_YOU,
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.FOR_YOU) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.help),
                selected = currentSection == KaiSection.HELP,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.HELP) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            KaiDrawerItem(
                label = stringResource(R.string.logout),
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                labelColor = MaterialTheme.colorScheme.error,
                onClick = onLogout
            )
        }
    }
}

@Composable
private fun KaiDrawerHeaderCard(
    subtitle: String,
    userName: String,
    profileImageUrl: String,
    expanded: Boolean,
    onGoToProfile: () -> Unit
) {
    val displayName = userName.ifBlank { stringResource(R.string.app_name) }
    val headerAlpha = animateFloatAsState(
        targetValue = if (expanded) 1f else 0.72f,
        animationSpec = tween(durationMillis = 450),
        label = "drawerHeaderAlpha"
    )
    val headerScale = animateFloatAsState(
        targetValue = if (expanded) 1f else 0.94f,
        animationSpec = tween(durationMillis = 500),
        label = "drawerHeaderScale"
    )
    val headerOffset = animateFloatAsState(
        targetValue = if (expanded) 0f else -12f,
        animationSpec = tween(durationMillis = 520),
        label = "drawerHeaderOffset"
    )
    val cardShape = RoundedCornerShape(26.dp)

    Card(
        onClick = onGoToProfile,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = headerOffset.value.dp)
            .scale(headerScale.value),
        shape = cardShape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.62f * headerAlpha.value),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KaiUserAvatar(
                    displayName = displayName,
                    imageUrl = profileImageUrl,
                    modifier = Modifier,
                    size = 82.dp,
                    showBorder = false,
                    showGlow = false
                )

                Text(
                    text = displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    color = TarnishedGold
                )

                Text(
                    text = stringResource(R.string.profile),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.82f)
                )
            }

            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.95f)
            )
        }
    }
}

@Composable
private fun KaiDrawerItem(
    label: String,
    selected: Boolean,
    disabled: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    labelColor: Color = when {
        disabled -> TarnishedGold.copy(alpha = 0.55f)
        selected -> TarnishedGold
        else -> OldIvory
    },
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor
            )
        },
        selected = selected,
        icon = leadingIcon,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BloodWine.copy(alpha = 0.6f),
            selectedTextColor = labelColor,
            selectedIconColor = labelColor,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = labelColor,
            unselectedIconColor = labelColor
        )
    )
}
