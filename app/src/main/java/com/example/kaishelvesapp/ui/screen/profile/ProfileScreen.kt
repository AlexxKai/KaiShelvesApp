package com.example.kaishelvesapp.ui.screen.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.example.kaishelvesapp.data.repository.LoginProviderState
import com.example.kaishelvesapp.data.security.ProfileImageCodec
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
import kotlin.math.max

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
    onGoToRegister: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasProfileChanges =
        uiState.username.trim() != uiState.user?.usuario.orEmpty().trim() ||
            uiState.email.trim() != uiState.user?.email.orEmpty().trim()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var expandedLanguage by remember { mutableStateOf(false) }
    var selectedProfileTab by remember { mutableStateOf(ProfileTab.Identity) }
    var accountNotificationsEnabled by remember { mutableStateOf(true) }
    var keepSessionOpen by remember { mutableStateOf(true) }
    var confirmBeforeLogout by remember { mutableStateOf(false) }
    var pendingProfilePhotoUri by remember { mutableStateOf<String?>(null) }
    var showLoginOptionsDialog by remember { mutableStateOf(false) }
    val privacySettings = uiState.user?.privacySettings ?: UserPrivacySettings()
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        pendingProfilePhotoUri = uri?.toString()
    }

    pendingProfilePhotoUri?.let { selectedPhotoUri ->
        ProfilePhotoCropDialog(
            imageUri = selectedPhotoUri,
            onDismiss = { pendingProfilePhotoUri = null },
            onConfirm = { croppedPhoto ->
                pendingProfilePhotoUri = null
                viewModel.saveProfilePhoto(croppedPhoto)
            }
        )
    }

    if (showLoginOptionsDialog) {
        PasswordLoginDialog(
            email = uiState.accessEmail,
            password = uiState.accessPassword,
            passwordConfirmation = uiState.accessPasswordConfirmation,
            isLoading = uiState.isLoading,
            hasPasswordLogin = uiState.hasPasswordLogin,
            onEmailChange = viewModel::onAccessEmailChange,
            onPasswordChange = viewModel::onAccessPasswordChange,
            onPasswordConfirmationChange = viewModel::onAccessPasswordConfirmationChange,
            onDismiss = { showLoginOptionsDialog = false },
            onConfirm = viewModel::savePasswordLogin
        )
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
            showLoginOptionsDialog = false
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
                    .verticalScroll(rememberScrollState())
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
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 51.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = Obsidian),
                                border = BorderStroke(1.dp, TarnishedGold)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = onBack) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.back),
                                                tint = TarnishedGold
                                            )
                                        }

                                        if (selectedProfileTab == ProfileTab.Identity) {
                                            IconButton(
                                                onClick = viewModel::saveProfileChanges,
                                                enabled = hasProfileChanges && !uiState.isLoading
                                            ) {
                                                if (uiState.isLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = TarnishedGold
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.Save,
                                                        contentDescription = stringResource(R.string.save_profile_changes),
                                                        tint = if (hasProfileChanges) {
                                                            TarnishedGold
                                                        } else {
                                                            TarnishedGold.copy(alpha = 0.34f)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (selectedProfileTab) {
                                        ProfileTab.Identity -> {
                                            ProfileAvatarSection(
                                                displayName = uiState.username.ifBlank {
                                                    userName ?: uiState.user?.usuario ?: stringResource(R.string.app_name)
                                                },
                                                imageUrl = uiState.profilePhotoUri.ifBlank {
                                                    profileImageUrl ?: uiState.user?.photoUrl.orEmpty()
                                                },
                                                isAdmin = uiState.user?.isAdmin == true,
                                                onChangePhoto = { photoPickerLauncher.launch("image/*") }
                                            )

                                            Spacer(modifier = Modifier.height(20.dp))

                                            OutlinedTextField(
                                                value = uiState.username,
                                                onValueChange = viewModel::onUsernameChange,
                                                label = { Text(stringResource(R.string.username)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                                                singleLine = true
                                            )

                                            if (!uiState.hasGoogleLogin) {
                                                Spacer(modifier = Modifier.height(16.dp))

                                                OutlinedTextField(
                                                    value = uiState.email,
                                                    onValueChange = viewModel::onEmailChange,
                                                    label = { Text(stringResource(R.string.email)) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                                                    singleLine = true
                                                )
                                            }

                                            if (uiState.user?.isGuest != true) {
                                                Spacer(modifier = Modifier.height(20.dp))

                                                LoginProvidersSection(
                                                    providers = uiState.loginProviders,
                                                    isLoading = uiState.isLoading,
                                                    onOpenLoginOptions = {
                                                        viewModel.onAccessEmailChange("")
                                                        showLoginOptionsDialog = true
                                                    },
                                                    onUnlinkProvider = viewModel::unlinkLoginProvider
                                                )
                                            }
                                        }

                                        ProfileTab.Settings -> {
                                            ProfileSettingsContent(
                                                expandedLanguage = expandedLanguage,
                                                onExpandedChange = { expandedLanguage = it },
                                                onSelectLanguage = { language ->
                                                    expandedLanguage = false
                                                    activity?.let {
                                                        LanguageManager.setLanguage(it, language)
                                                    }
                                                },
                                                notificationsEnabled = accountNotificationsEnabled,
                                                onNotificationsEnabledChange = { accountNotificationsEnabled = it },
                                                pendingRequestCount = pendingRequestCount,
                                                onOpenNotifications = onOpenNotifications,
                                                keepSessionOpen = keepSessionOpen,
                                                onKeepSessionOpenChange = { keepSessionOpen = it },
                                                confirmBeforeLogout = confirmBeforeLogout,
                                                onConfirmBeforeLogoutChange = { confirmBeforeLogout = it },
                                                isGuest = uiState.user?.isGuest == true,
                                                onGoToRegister = onGoToRegister,
                                                onLogout = onLogout
                                            )
                                        }

                                        ProfileTab.Privacy -> {
                                            ProfilePrivacyContent(
                                                privacySettings = privacySettings,
                                                onPrivacySettingsChange = viewModel::updatePrivacySettings
                                            )
                                        }
                                    }
                                }
                            }

                            ProfileTabSelector(
                                selectedTab = selectedProfileTab,
                                onSelectTab = { selectedProfileTab = it },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 26.dp)
                                    .zIndex(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class ProfileTab {
    Identity,
    Settings,
    Privacy
}

@Composable
private fun ProfileTabSelector(
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ProfileTabButton(
            text = stringResource(R.string.profile_tab_identity),
            selected = selectedTab == ProfileTab.Identity,
            onClick = { onSelectTab(ProfileTab.Identity) },
            modifier = Modifier.weight(1f)
        )
        ProfileTabButton(
            text = stringResource(R.string.profile_tab_settings),
            selected = selectedTab == ProfileTab.Settings,
            onClick = { onSelectTab(ProfileTab.Settings) },
            modifier = Modifier.weight(1f)
        )
        ProfileTabButton(
            text = stringResource(R.string.profile_tab_privacy),
            selected = selectedTab == ProfileTab.Privacy,
            onClick = { onSelectTab(ProfileTab.Privacy) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(if (selected) 52.dp else 42.dp)
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),
            border = BorderStroke(1.dp, TarnishedGold),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected) Obsidian else Obsidian.copy(alpha = 0.86f),
                contentColor = if (selected) OldIvory else TarnishedGold
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val tabTextSize = when {
                    maxWidth < 72.dp -> 10.sp
                    maxWidth < 88.dp -> 11.sp
                    else -> 12.sp
                }

                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = tabTextSize),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 1.dp)
                    .background(Obsidian)
            )
        }
    }
}

@Composable
private fun LoginProvidersSection(
    providers: List<LoginProviderState>,
    isLoading: Boolean,
    onOpenLoginOptions: () -> Unit,
    onUnlinkProvider: (String) -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_login_methods_section_title)) {
        providers.forEachIndexed { index, provider ->
            LoginProviderRow(
                provider = provider,
                isLoading = isLoading,
                onOpenLoginOptions = onOpenLoginOptions,
                onUnlinkProvider = onUnlinkProvider
            )

            if (index < providers.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TarnishedGold.copy(alpha = 0.18f)
                )
            }
        }
    }
}

@Composable
private fun LoginProviderRow(
    provider: LoginProviderState,
    isLoading: Boolean,
    onOpenLoginOptions: () -> Unit,
    onUnlinkProvider: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderLogo(
            label = providerLogoLabel(provider.providerId)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = providerDisplayName(provider.providerId),
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory
            )
            Text(
                text = when {
                    provider.isPrimary -> stringResource(R.string.profile_login_method_primary)
                    provider.isLinked -> stringResource(R.string.profile_login_method_linked)
                    else -> stringResource(R.string.profile_login_method_available)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.74f)
            )
        }

        when {
            provider.isPrimary -> {
                Text(
                    text = stringResource(R.string.profile_login_method_required),
                    style = MaterialTheme.typography.labelMedium,
                    color = TarnishedGold
                )
            }

            provider.isLinked -> {
                TextButton(
                    onClick = { onUnlinkProvider(provider.providerId) },
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.profile_remove_login_method),
                        color = TarnishedGold
                    )
                }
            }

            provider.providerId == PASSWORD_PROVIDER_ID -> {
                OutlinedButton(
                    onClick = onOpenLoginOptions,
                    enabled = !isLoading,
                    border = BorderStroke(1.dp, TarnishedGold)
                ) {
                    Text(
                        text = stringResource(R.string.profile_link_login_short_action),
                        color = TarnishedGold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderLogo(
    label: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = TarnishedGold,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )
    }
}

@Composable
private fun providerDisplayName(providerId: String): String {
    return when (providerId) {
        GOOGLE_PROVIDER_ID -> stringResource(R.string.profile_provider_google)
        PASSWORD_PROVIDER_ID -> stringResource(R.string.profile_provider_password)
        FACEBOOK_PROVIDER_ID -> stringResource(R.string.profile_provider_facebook)
        APPLE_PROVIDER_ID -> stringResource(R.string.profile_provider_apple)
        GITHUB_PROVIDER_ID -> stringResource(R.string.profile_provider_github)
        else -> providerId
    }
}

private fun providerLogoLabel(providerId: String): String {
    return when (providerId) {
        GOOGLE_PROVIDER_ID -> "G"
        PASSWORD_PROVIDER_ID -> "@"
        FACEBOOK_PROVIDER_ID -> "f"
        APPLE_PROVIDER_ID -> "A"
        GITHUB_PROVIDER_ID -> "GH"
        else -> "?"
    }
}

@Composable
private fun PasswordLoginDialog(
    email: String,
    password: String,
    passwordConfirmation: String,
    isLoading: Boolean,
    hasPasswordLogin: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        containerColor = Obsidian,
        titleContentColor = TarnishedGold,
        textContentColor = OldIvory,
        title = {
            Text(
                text = stringResource(
                    if (hasPasswordLogin) {
                        R.string.profile_password_section_title
                    } else {
                        R.string.profile_link_login_section_title
                    }
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.profile_link_login_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.78f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = {
                        Text(
                            stringResource(
                                if (hasPasswordLogin) {
                                    R.string.profile_new_password
                                } else {
                                    R.string.password
                                }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passwordConfirmation,
                    onValueChange = onPasswordConfirmationChange,
                    label = { Text(stringResource(R.string.profile_confirm_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(
                        if (hasPasswordLogin) {
                            R.string.profile_change_password
                        } else {
                            R.string.profile_link_login_action
                        }
                    ),
                    color = TarnishedGold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = OldIvory
                )
            }
        }
    )
}

@Composable
private fun ProfileSettingsContent(
    expandedLanguage: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    pendingRequestCount: Int,
    onOpenNotifications: () -> Unit,
    keepSessionOpen: Boolean,
    onKeepSessionOpenChange: (Boolean) -> Unit,
    confirmBeforeLogout: Boolean,
    onConfirmBeforeLogoutChange: (Boolean) -> Unit,
    isGuest: Boolean,
    onGoToRegister: () -> Unit,
    onLogout: () -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_settings_language)) {
        LanguageSection(
            expandedLanguage = expandedLanguage,
            onExpandedChange = onExpandedChange,
            onSelectLanguage = onSelectLanguage
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_settings_notifications)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_notifications_enabled),
            body = stringResource(R.string.profile_notifications_enabled_body),
            checked = notificationsEnabled,
            onCheckedChange = onNotificationsEnabledChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenNotifications,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Text(
                text = stringResource(R.string.profile_open_notifications, pendingRequestCount),
                color = TarnishedGold
            )
        }
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_settings_session_preferences)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_keep_session_open),
            body = stringResource(R.string.profile_keep_session_open_body),
            checked = keepSessionOpen,
            onCheckedChange = onKeepSessionOpenChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_confirm_before_logout),
            body = stringResource(R.string.profile_confirm_before_logout_body),
            checked = confirmBeforeLogout,
            onCheckedChange = onConfirmBeforeLogoutChange
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_settings_account_behavior)) {
        if (isGuest) {
            Button(
                onClick = onGoToRegister,
                modifier = Modifier.fillMaxWidth(),
                colors = KaiShelvesThemeDefaults.primaryButtonColors()
            ) {
                Text(stringResource(R.string.create_account_and_sync))
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = KaiShelvesThemeDefaults.primaryButtonColors()
        ) {
            Text(stringResource(R.string.logout))
        }
    }
}

@Composable
private fun ProfilePrivacyContent(
    privacySettings: UserPrivacySettings,
    onPrivacySettingsChange: (UserPrivacySettings) -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_session_protection)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_session_protection_enabled),
            body = stringResource(R.string.profile_session_protection_enabled_body),
            checked = privacySettings.sessionProtectionEnabled,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(sessionProtectionEnabled = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_sensitive_action_confirmation),
            body = stringResource(R.string.profile_sensitive_action_confirmation_body),
            checked = privacySettings.sensitiveActionConfirmation,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(sensitiveActionConfirmation = it))
            }
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_visibility)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_visibility_enabled),
            body = stringResource(R.string.profile_visibility_enabled_body),
            checked = privacySettings.profileVisible,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(profileVisible = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_email_visibility),
            body = stringResource(R.string.profile_email_visibility_body),
            checked = privacySettings.emailVisible,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(emailVisible = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_reading_activity_visibility),
            body = stringResource(R.string.profile_reading_activity_visibility_body),
            checked = privacySettings.readingActivityVisible,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(readingActivityVisible = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_friends_visibility),
            body = stringResource(R.string.profile_friends_visibility_body),
            checked = privacySettings.friendsVisible,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(friendsVisible = it))
            }
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_permissions)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_friend_request_permissions),
            body = stringResource(R.string.profile_friend_request_permissions_body),
            checked = privacySettings.friendRequestPermissions,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(friendRequestPermissions = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_social_interaction_permissions),
            body = stringResource(R.string.profile_social_interaction_permissions_body),
            checked = privacySettings.socialInteractionPermissions,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(socialInteractionPermissions = it))
            }
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_personal_data_control)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_personal_data_control),
            body = stringResource(R.string.profile_personal_data_control_body),
            checked = privacySettings.personalDataControl,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(personalDataControl = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_personalized_suggestions),
            body = stringResource(R.string.profile_personalized_suggestions_body),
            checked = privacySettings.personalizedSuggestions,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(personalizedSuggestions = it))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileInfoLine(stringResource(R.string.profile_privacy_local_notice))
    }
}

@Composable
private fun ProfileSectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(
                width = 1.dp,
                color = TarnishedGold.copy(alpha = 0.34f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

@Composable
private fun ProfileInfoLine(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = OldIvory.copy(alpha = 0.72f)
    )
}

@Composable
private fun ProfileToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.74f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Obsidian,
                checkedTrackColor = TarnishedGold,
                uncheckedThumbColor = TarnishedGold,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = TarnishedGold.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun ProfileAvatarSection(
    displayName: String,
    imageUrl: String,
    isAdmin: Boolean,
    onChangePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KaiUserAvatar(
            displayName = displayName,
            imageUrl = imageUrl,
            modifier = Modifier.size(132.dp),
            size = 104.dp
        )

        if (isAdmin) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(999.dp),
                colors = CardDefaults.cardColors(containerColor = TarnishedGold.copy(alpha = 0.18f)),
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text(
                    text = stringResource(R.string.admin_badge),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = TarnishedGold
                )
            }
        }

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

@Composable
private fun ProfilePhotoCropDialog(
    imageUri: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val cropSize = 260.dp
    val density = LocalDensity.current
    val viewportSizePx = with(density) { cropSize.roundToPx() }
    val bitmap = remember(imageUri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }
    var zoom by remember(imageUri) { mutableStateOf(1f) }
    var offset by remember(imageUri) { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ajustar foto de perfil",
                    style = MaterialTheme.typography.titleMedium,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (bitmap == null) {
                    Box(
                        modifier = Modifier
                            .size(cropSize)
                            .border(1.dp, TarnishedGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se pudo cargar la imagen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val baseScale = max(
                        viewportSizePx.toFloat() / bitmap.width.toFloat(),
                        viewportSizePx.toFloat() / bitmap.height.toFloat()
                    )
                    val scaledWidth = bitmap.width * baseScale * zoom
                    val scaledHeight = bitmap.height * baseScale * zoom
                    val maxOffsetX = ((scaledWidth - viewportSizePx) / 2f).coerceAtLeast(0f)
                    val maxOffsetY = ((scaledHeight - viewportSizePx) / 2f).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .size(cropSize)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.28f))
                            .border(1.dp, TarnishedGold, RoundedCornerShape(18.dp))
                            .pointerInput(bitmap) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                    val nextScaledWidth = bitmap.width * baseScale * nextZoom
                                    val nextScaledHeight = bitmap.height * baseScale * nextZoom
                                    val nextMaxOffsetX = ((nextScaledWidth - viewportSizePx) / 2f)
                                        .coerceAtLeast(0f)
                                    val nextMaxOffsetY = ((nextScaledHeight - viewportSizePx) / 2f)
                                        .coerceAtLeast(0f)

                                    zoom = nextZoom
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-nextMaxOffsetX, nextMaxOffsetX),
                                        y = (offset.y + pan.y).coerceIn(-nextMaxOffsetY, nextMaxOffsetY)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto de perfil seleccionada",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = offset.x.coerceIn(-maxOffsetX, maxOffsetX)
                                    translationY = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = zoom,
                        onValueChange = { nextZoom ->
                            val coercedZoom = nextZoom.coerceIn(1f, 4f)
                            val nextScaledWidth = bitmap.width * baseScale * coercedZoom
                            val nextScaledHeight = bitmap.height * baseScale * coercedZoom
                            val nextMaxOffsetX = ((nextScaledWidth - viewportSizePx) / 2f)
                                .coerceAtLeast(0f)
                            val nextMaxOffsetY = ((nextScaledHeight - viewportSizePx) / 2f)
                                .coerceAtLeast(0f)

                            zoom = coercedZoom
                            offset = Offset(
                                x = offset.x.coerceIn(-nextMaxOffsetX, nextMaxOffsetX),
                                y = offset.y.coerceIn(-nextMaxOffsetY, nextMaxOffsetY)
                            )
                        },
                        valueRange = 1f..4f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TarnishedGold)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = TarnishedGold
                        )
                    }

                    Button(
                        onClick = {
                            val croppedPhoto = ProfileImageCodec.cropImageAsDataUri(
                                context = context,
                                uri = Uri.parse(imageUri),
                                viewportSizePx = viewportSizePx,
                                zoom = zoom,
                                offsetX = offset.x,
                                offsetY = offset.y
                            )
                            onConfirm(croppedPhoto)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = bitmap != null,
                        colors = KaiShelvesThemeDefaults.primaryButtonColors()
                    ) {
                        Text(stringResource(R.string.save_profile_changes))
                    }
                }
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

private const val GOOGLE_PROVIDER_ID = "google.com"
private const val PASSWORD_PROVIDER_ID = "password"
private const val FACEBOOK_PROVIDER_ID = "facebook.com"
private const val APPLE_PROVIDER_ID = "apple.com"
private const val GITHUB_PROVIDER_ID = "github.com"
