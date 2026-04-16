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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
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
    onOpenMenu: () -> Unit
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
            Spacer(modifier = Modifier.width(48.dp))
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
                IconButton(onClick = ::launchScanner) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = stringResource(R.string.scan_isbn),
                        tint = TarnishedGold
                    )
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
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KaiDrawerHeaderCard(
                subtitle = subtitle,
                userName = userName,
                profileImageUrl = profileImageUrl,
                expanded = expanded,
                onGoToProfile = onGoToProfile
            )

            KaiDrawerItem(
                label = stringResource(R.string.profile),
                selected = currentSection == KaiSection.PROFILE,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = TarnishedGold
                    )
                },
                onClick = onGoToProfile
            )

            KaiDrawerItem(
                label = stringResource(R.string.settings_privacy),
                selected = false,
                onClick = onGoToSettingsPrivacy
            )

            KaiDrawerItem(
                label = stringResource(R.string.reading_statistics),
                selected = currentSection == KaiSection.STATS,
                leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.STATS) }
            )

            Spacer(modifier = Modifier.weight(1f))

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = headerOffset.value.dp)
            .scale(headerScale.value),
        shape = RoundedCornerShape(26.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaiUserAvatar(
                    displayName = displayName,
                    imageUrl = profileImageUrl,
                    modifier = Modifier.clickable(onClick = onGoToProfile)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TarnishedGold
                    )

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )
                }
            }

            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.95f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TarnishedGold.copy(alpha = 0.22f),
                                BloodWine.copy(alpha = 0.35f),
                                DeepWalnut,
                                TarnishedGold.copy(alpha = 0.18f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun KaiDrawerItem(
    label: String,
    selected: Boolean,
    leadingIcon: @Composable (() -> Unit)? = null,
    labelColor: Color = if (selected) TarnishedGold else OldIvory,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
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
