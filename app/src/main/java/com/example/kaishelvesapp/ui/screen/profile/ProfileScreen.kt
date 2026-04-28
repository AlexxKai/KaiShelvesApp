package com.example.kaishelvesapp.ui.screen.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    onGoToRegister: () -> Unit,
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
    var selectedProfileTab by remember { mutableStateOf(ProfileTab.Identity) }
    var accountNotificationsEnabled by remember { mutableStateOf(true) }
    var keepSessionOpen by remember { mutableStateOf(true) }
    var confirmBeforeLogout by remember { mutableStateOf(false) }
    var sessionProtectionEnabled by remember { mutableStateOf(true) }
    var sensitiveActionConfirmation by remember { mutableStateOf(true) }
    var profileVisible by remember { mutableStateOf(true) }
    var emailVisible by remember { mutableStateOf(false) }
    var readingActivityVisible by remember { mutableStateOf(true) }
    var friendsVisible by remember { mutableStateOf(true) }
    var friendRequestPermissions by remember { mutableStateOf(true) }
    var socialInteractionPermissions by remember { mutableStateOf(true) }
    var personalDataControl by remember { mutableStateOf(false) }
    var personalizedSuggestions by remember { mutableStateOf(true) }
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
                if (selectedProfileTab != ProfileTab.Identity) {
                    return@Scaffold
                }

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
                    .padding(top = innerPadding.calculateTopPadding())
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Obsidian),
                            border = BorderStroke(1.dp, TarnishedGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                if (!uiState.isEditingProfile) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier.align(Alignment.Start)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back),
                                            tint = TarnishedGold
                                        )
                                    }
                                }

                                ProfileTabSelector(
                                    selectedTab = selectedProfileTab,
                                    onSelectTab = { selectedProfileTab = it }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

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
                                        } else {
                                            ProfileLine(
                                                stringResource(R.string.username),
                                                uiState.user?.usuario ?: "Sin nombre"
                                            )
                                        }

                                        ProfileLine(
                                            stringResource(R.string.email),
                                            uiState.user?.email ?: "Sin email"
                                        )
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
                                            sessionProtectionEnabled = sessionProtectionEnabled,
                                            onSessionProtectionEnabledChange = { sessionProtectionEnabled = it },
                                            sensitiveActionConfirmation = sensitiveActionConfirmation,
                                            onSensitiveActionConfirmationChange = { sensitiveActionConfirmation = it },
                                            profileVisible = profileVisible,
                                            onProfileVisibleChange = { profileVisible = it },
                                            emailVisible = emailVisible,
                                            onEmailVisibleChange = { emailVisible = it },
                                            readingActivityVisible = readingActivityVisible,
                                            onReadingActivityVisibleChange = { readingActivityVisible = it },
                                            friendsVisible = friendsVisible,
                                            onFriendsVisibleChange = { friendsVisible = it },
                                            friendRequestPermissions = friendRequestPermissions,
                                            onFriendRequestPermissionsChange = { friendRequestPermissions = it },
                                            socialInteractionPermissions = socialInteractionPermissions,
                                            onSocialInteractionPermissionsChange = { socialInteractionPermissions = it },
                                            personalDataControl = personalDataControl,
                                            onPersonalDataControlChange = { personalDataControl = it },
                                            personalizedSuggestions = personalizedSuggestions,
                                            onPersonalizedSuggestionsChange = { personalizedSuggestions = it }
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

private enum class ProfileTab {
    Identity,
    Settings,
    Privacy
}

@Composable
private fun ProfileTabSelector(
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, TarnishedGold),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) TarnishedGold.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (selected) OldIvory else TarnishedGold
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
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
    sessionProtectionEnabled: Boolean,
    onSessionProtectionEnabledChange: (Boolean) -> Unit,
    sensitiveActionConfirmation: Boolean,
    onSensitiveActionConfirmationChange: (Boolean) -> Unit,
    profileVisible: Boolean,
    onProfileVisibleChange: (Boolean) -> Unit,
    emailVisible: Boolean,
    onEmailVisibleChange: (Boolean) -> Unit,
    readingActivityVisible: Boolean,
    onReadingActivityVisibleChange: (Boolean) -> Unit,
    friendsVisible: Boolean,
    onFriendsVisibleChange: (Boolean) -> Unit,
    friendRequestPermissions: Boolean,
    onFriendRequestPermissionsChange: (Boolean) -> Unit,
    socialInteractionPermissions: Boolean,
    onSocialInteractionPermissionsChange: (Boolean) -> Unit,
    personalDataControl: Boolean,
    onPersonalDataControlChange: (Boolean) -> Unit,
    personalizedSuggestions: Boolean,
    onPersonalizedSuggestionsChange: (Boolean) -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_session_protection)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_session_protection_enabled),
            body = stringResource(R.string.profile_session_protection_enabled_body),
            checked = sessionProtectionEnabled,
            onCheckedChange = onSessionProtectionEnabledChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_sensitive_action_confirmation),
            body = stringResource(R.string.profile_sensitive_action_confirmation_body),
            checked = sensitiveActionConfirmation,
            onCheckedChange = onSensitiveActionConfirmationChange
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_visibility)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_visibility_enabled),
            body = stringResource(R.string.profile_visibility_enabled_body),
            checked = profileVisible,
            onCheckedChange = onProfileVisibleChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_email_visibility),
            body = stringResource(R.string.profile_email_visibility_body),
            checked = emailVisible,
            onCheckedChange = onEmailVisibleChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_reading_activity_visibility),
            body = stringResource(R.string.profile_reading_activity_visibility_body),
            checked = readingActivityVisible,
            onCheckedChange = onReadingActivityVisibleChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_friends_visibility),
            body = stringResource(R.string.profile_friends_visibility_body),
            checked = friendsVisible,
            onCheckedChange = onFriendsVisibleChange
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_permissions)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_friend_request_permissions),
            body = stringResource(R.string.profile_friend_request_permissions_body),
            checked = friendRequestPermissions,
            onCheckedChange = onFriendRequestPermissionsChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_social_interaction_permissions),
            body = stringResource(R.string.profile_social_interaction_permissions_body),
            checked = socialInteractionPermissions,
            onCheckedChange = onSocialInteractionPermissionsChange
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_personal_data_control)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_personal_data_control),
            body = stringResource(R.string.profile_personal_data_control_body),
            checked = personalDataControl,
            onCheckedChange = onPersonalDataControlChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileToggleRow(
            title = stringResource(R.string.profile_personalized_suggestions),
            body = stringResource(R.string.profile_personalized_suggestions_body),
            checked = personalizedSuggestions,
            onCheckedChange = onPersonalizedSuggestionsChange
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
