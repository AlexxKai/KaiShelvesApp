package com.example.kaishelvesapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onOpenMenu: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit
) {
    var showUserMenu by remember { mutableStateOf(false) }
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

            Box {
                IconButton(onClick = { showUserMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.profile_menu),
                        tint = TarnishedGold
                    )
                }

                DropdownMenu(
                    expanded = showUserMenu,
                    onDismissRequest = { showUserMenu = false },
                    containerColor = Obsidian
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile), color = OldIvory) },
                        onClick = {
                            showUserMenu = false
                            onGoToProfile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_privacy), color = OldIvory) },
                        onClick = {
                            showUserMenu = false
                            onGoToSettingsPrivacy()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.logout), color = TarnishedGold) },
                        onClick = {
                            showUserMenu = false
                            onLogout()
                        }
                    )
                }
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
    headerTitle: String,
    subtitle: String,
    userName: String = "",
    profileImageUrl: String = "",
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
            verticalArrangement = Arrangement.Top
        ) {
            KaiDrawerHeaderCard(
                title = headerTitle,
                subtitle = subtitle,
                userName = userName,
                profileImageUrl = profileImageUrl
            )

            Spacer(modifier = Modifier.height(24.dp))

            KaiDrawerItem(
                label = stringResource(R.string.home),
                selected = currentSection == KaiSection.HOME,
                onClick = { onSectionSelected(KaiSection.HOME) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.catalog),
                selected = currentSection == KaiSection.CATALOG,
                onClick = { onSectionSelected(KaiSection.CATALOG) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.lists),
                selected = currentSection == KaiSection.LISTS,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.LISTS) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.my_readings),
                selected = currentSection == KaiSection.READING,
                onClick = { onSectionSelected(KaiSection.READING) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.reading_statistics),
                selected = currentSection == KaiSection.STATS,
                leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.STATS) }
            )
        }
    }
}

@Composable
private fun KaiDrawerHeaderCard(
    title: String,
    subtitle: String,
    userName: String,
    profileImageUrl: String
) {
    val displayName = userName.ifBlank { stringResource(R.string.app_name) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.62f),
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
                    imageUrl = profileImageUrl
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

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = subtitle,
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
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        },
        selected = selected,
        icon = leadingIcon,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BloodWine.copy(alpha = 0.6f),
            selectedTextColor = TarnishedGold,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = OldIvory
        )
    )
}
