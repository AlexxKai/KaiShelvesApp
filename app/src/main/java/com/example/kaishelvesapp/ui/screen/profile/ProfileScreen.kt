package com.example.kaishelvesapp.ui.screen.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.example.kaishelvesapp.ui.language.findActivity
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: AuthViewModel,
    userName: String? = null,
    profileImageUrl: String? = null,
    onBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasProfileChanges = uiState.isEditingProfile && (
        uiState.username.trim() != uiState.user?.usuario.orEmpty().trim() ||
            uiState.profilePhotoUri.trim() != uiState.user?.photoUrl.orEmpty().trim()
        )
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var expandedLanguage by remember { mutableStateOf(false) }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.toString()?.let(viewModel::onProfilePhotoSelected)
    }

    LaunchedEffect(Unit) {
        if (uiState.user == null && uiState.isLoggedIn) {
            viewModel.loadCurrentUserProfile()
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }

        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.PROFILE,
                subtitle = stringResource(R.string.profile_subtitle),
                userName = userName.orEmpty(),
                profileImageUrl = profileImageUrl.orEmpty(),
                expanded = drawerExpanded,
                onGoToProfile = {
                    scope.launch { drawerState.close() }
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
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
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
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when {
                            uiState.isLoading -> Unit
                            !uiState.isEditingProfile -> viewModel.startEditingProfile()
                            hasProfileChanges -> viewModel.saveProfileChanges()
                            else -> viewModel.cancelEditingProfile()
                        }
                    },
                    containerColor = TarnishedGold,
                    contentColor = Obsidian
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Obsidian)
                    } else {
                        Icon(
                            imageVector = when {
                                !uiState.isEditingProfile -> Icons.Filled.Edit
                                hasProfileChanges -> Icons.Filled.Save
                                else -> Icons.AutoMirrored.Filled.Undo
                            },
                            contentDescription = when {
                                !uiState.isEditingProfile -> stringResource(R.string.edit_profile)
                                hasProfileChanges -> stringResource(R.string.save_profile_changes)
                                else -> stringResource(R.string.cancel)
                            }
                        )
                    }
                }
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.PROFILE,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    uiState.isLoading && uiState.user == null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }

                    else -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Obsidian),
                            border = BorderStroke(1.dp, TarnishedGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                ProfileAvatarSection(
                                    displayName = uiState.username.ifBlank {
                                        userName ?: uiState.user?.usuario ?: stringResource(R.string.app_name)
                                    },
                                    imageUrl = uiState.profilePhotoUri.ifBlank {
                                        profileImageUrl ?: uiState.user?.photoUrl.orEmpty()
                                    },
                                    isEditing = uiState.isEditingProfile,
                                    onChangePhoto = { photoPickerLauncher.launch("image/*") }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                if (uiState.isEditingProfile) {
                                    OutlinedTextField(
                                        value = uiState.username,
                                        onValueChange = viewModel::onUsernameChange,
                                        label = { Text(stringResource(R.string.username)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    ProfileLine(
                                        stringResource(R.string.email),
                                        uiState.user?.email ?: "Sin email"
                                    )

                                    ProfileLine(
                                        "UID",
                                        uiState.user?.uid ?: "No disponible"
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    LanguageSection(
                                        expandedLanguage = expandedLanguage,
                                        onExpandedChange = { expandedLanguage = it },
                                        onSelectLanguage = { language ->
                                            expandedLanguage = false
                                            activity?.let {
                                                LanguageManager.setLanguage(it, language)
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                } else {
                                    ProfileLine(
                                        stringResource(R.string.username),
                                        uiState.user?.usuario ?: "Sin nombre"
                                    )

                                    ProfileLine(
                                        stringResource(R.string.email),
                                        uiState.user?.email ?: "Sin email"
                                    )

                                    ProfileLine(
                                        "UID",
                                        uiState.user?.uid ?: "No disponible"
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    LanguageSection(
                                        expandedLanguage = expandedLanguage,
                                        onExpandedChange = { expandedLanguage = it },
                                        onSelectLanguage = { language ->
                                            expandedLanguage = false
                                            activity?.let {
                                                LanguageManager.setLanguage(it, language)
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = onLogout,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = KaiShelvesThemeDefaults.primaryButtonColors()
                                    ) {
                                        Text(stringResource(R.string.logout))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = onBack,
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, TarnishedGold)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.back),
                                            color = TarnishedGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarSection(
    displayName: String,
    imageUrl: String,
    isEditing: Boolean,
    onChangePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KaiUserAvatar(
            displayName = displayName,
            imageUrl = imageUrl,
            modifier = Modifier.size(84.dp)
        )

        if (isEditing) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onChangePhoto,
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text(
                    text = stringResource(R.string.change_profile_photo),
                    color = TarnishedGold
                )
            }
        }
    }
}

@Composable
private fun LanguageSection(
    expandedLanguage: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.app_language),
        style = MaterialTheme.typography.labelLarge,
        color = TarnishedGold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Text(
                text = when (LanguageManager.getCurrentLanguage()) {
                    "en" -> stringResource(R.string.english)
                    else -> stringResource(R.string.spanish)
                },
                color = TarnishedGold
            )
        }

        DropdownMenu(
            expanded = expandedLanguage,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = Obsidian,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.spanish),
                        color = OldIvory
                    )
                },
                onClick = { onSelectLanguage("es") }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.english),
                        color = OldIvory
                    )
                },
                onClick = { onSelectLanguage("en") }
            )
        }
    }
}

@Composable
private fun ProfileLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = OldIvory
        )
    }
}
