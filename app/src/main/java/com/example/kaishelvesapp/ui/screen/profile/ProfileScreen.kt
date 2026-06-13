package com.example.kaishelvesapp.ui.screen.profile

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.example.kaishelvesapp.data.repository.AccountReport
import com.example.kaishelvesapp.data.repository.AccountReportStatus
import com.example.kaishelvesapp.data.repository.BlockedMember
import com.example.kaishelvesapp.data.repository.LoginProviderState
import com.example.kaishelvesapp.data.repository.REPORT_SENDER_ADMIN
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.components.LocalGuestUiRestrictions
import com.example.kaishelvesapp.ui.components.GoogleSignInButton
import com.example.kaishelvesapp.ui.components.PasswordOutlinedTextField
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.example.kaishelvesapp.ui.language.findActivity
import com.example.kaishelvesapp.ui.screen.friends.FriendProfileContent
import com.example.kaishelvesapp.ui.screen.library.ReaderListAutomationMode
import com.example.kaishelvesapp.ui.screen.library.readReaderListAutomationMode
import com.example.kaishelvesapp.ui.screen.library.saveReaderListAutomationMode
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendProfileViewModel
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: AuthViewModel,
    myProfileViewModel: FriendProfileViewModel,
    initialReportReviewId: String? = null,
    onInitialReportReviewHandled: () -> Unit = {},
    userName: String? = null,
    profileImageUrl: String? = null,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onGoToRegister: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenFriendProfile: (String) -> Unit = {},
    onOpenFriendLists: (String, String) -> Unit = { _, _ -> },
    onOpenBook: (Libro) -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myProfileState by myProfileViewModel.uiState.collectAsStateWithLifecycle()
    val hasProfileChanges =
        uiState.username.trim() != uiState.user?.usuario.orEmpty().trim() ||
            uiState.email.trim() != uiState.user?.email.orEmpty().trim()
    val hasPrimaryPasswordLogin = uiState.loginProviders.any { provider ->
        provider.providerId == PASSWORD_PROVIDER_ID && provider.isPrimary
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var selectedProfileTab by remember { mutableStateOf(ProfileTab.MyProfile) }
    var selectedSettingsPanel by remember { mutableStateOf(ProfileSettingsPanel.Main) }
    var readerListAutomationMode by remember {
        mutableStateOf(readReaderListAutomationMode(context))
    }
    var pendingProfilePhotoUri by remember { mutableStateOf<String?>(null) }
    var showProfilePhotoPreview by remember { mutableStateOf(false) }
    var showLoginOptionsDialog by remember { mutableStateOf(false) }
    var passwordLoginDialogMessage by remember { mutableStateOf<String?>(null) }
    val privacySettings = uiState.user?.privacySettings ?: UserPrivacySettings()
    val isGuest = uiState.user?.isGuest == true
    val guestUiRestrictions = LocalGuestUiRestrictions.current
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    var pendingInitialReportReviewId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialReportReviewId) {
        val reportId = initialReportReviewId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (isGuest) return@LaunchedEffect
        pendingInitialReportReviewId = reportId
        selectedProfileTab = ProfileTab.Settings
        selectedSettingsPanel = ProfileSettingsPanel.ReportReview
        myProfileViewModel.observeMyReports()
    }
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        pendingProfilePhotoUri = uri?.toString()
    }
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importGoodreadsCsv(it.toString()) }
    }
    val profilePhotoDisplayName = uiState.username.ifBlank {
        userName ?: uiState.user?.usuario ?: stringResource(R.string.app_name)
    }
    val profilePhotoImageUrl = uiState.profilePhotoUri.ifBlank {
        profileImageUrl ?: uiState.user?.photoUrl.orEmpty()
    }

    if (showProfilePhotoPreview) {
        ProfilePhotoPreviewDialog(
            displayName = profilePhotoDisplayName,
            imageUrl = profilePhotoImageUrl,
            onDismiss = { showProfilePhotoPreview = false }
        )
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
            message = passwordLoginDialogMessage,
            onEmailChange = {
                passwordLoginDialogMessage = null
                viewModel.onAccessEmailChange(it)
            },
            onPasswordChange = {
                passwordLoginDialogMessage = null
                viewModel.onAccessPasswordChange(it)
            },
            onPasswordConfirmationChange = {
                passwordLoginDialogMessage = null
                viewModel.onAccessPasswordConfirmationChange(it)
            },
            onDismiss = {
                passwordLoginDialogMessage = null
                showLoginOptionsDialog = false
            },
            onConfirm = viewModel::savePasswordLogin
        )
    }

    LaunchedEffect(Unit) {
        if (uiState.user == null && uiState.isLoggedIn) {
            viewModel.loadCurrentUserProfile()
        }
    }

    LaunchedEffect(selectedProfileTab, uiState.user?.uid) {
        val currentUid = uiState.user?.uid.orEmpty()
        if (!isGuest && selectedProfileTab == ProfileTab.MyProfile && currentUid.isNotBlank()) {
            myProfileViewModel.loadProfile(currentUid)
        }
        if (!isGuest && selectedProfileTab == ProfileTab.Settings) {
            myProfileViewModel.loadBlockedMembers()
            myProfileViewModel.observeMyReports()
        }
    }

    LaunchedEffect(isGuest, selectedProfileTab) {
        if (isGuest && selectedProfileTab.isGuestRestricted) {
            selectedProfileTab = ProfileTab.Identity
        }
    }

    DisposableEffect(selectedProfileTab) {
        onDispose {
            if (selectedProfileTab == ProfileTab.Identity) {
                viewModel.cancelEditingProfile()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            if (showLoginOptionsDialog) {
                passwordLoginDialogMessage = it
            } else {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.clearMessages()
        }

        uiState.successMessage?.let {
            passwordLoginDialogMessage = null
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
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.imePadding()
                )
            },
            topBar = {
                KaiPrimaryTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onScanResult = onScanResult,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    notificationCount = pendingRequestCount,
                    onOpenNotifications = onOpenNotifications,
                    showSearchBar = false
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.PROFILE,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            GothicBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
            ) {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = selectedProfileTab == ProfileTab.Settings &&
                        selectedSettingsPanel == ProfileSettingsPanel.ReportReview &&
                        myProfileState.isLoadingReports,
                    onRefresh = {
                        if (selectedProfileTab == ProfileTab.Settings &&
                            selectedSettingsPanel == ProfileSettingsPanel.ReportReview
                        ) {
                            myProfileViewModel.loadMyReports()
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 20.dp)
                                ) {
                                    if (selectedProfileTab == ProfileTab.Identity) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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

                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    when (selectedProfileTab) {
                                        ProfileTab.MyProfile -> {
                                            when {
                                                myProfileState.isLoading -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 48.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(color = TarnishedGold)
                                                    }
                                                }

                                                myProfileState.profile != null -> {
                                                    FriendProfileContent(
                                                        profile = myProfileState.profile!!,
                                                        isRemovingFriend = myProfileState.isRemovingFriend,
                                                        isSendingRequest = myProfileState.isSendingRequest,
                                                        onRemoveFriend = {},
                                                        onSendFriendRequest = {},
                                                        onOpenFriendLists = onOpenFriendLists,
                                                        onOpenFriendProfile = onOpenFriendProfile,
                                                        onOpenBook = onOpenBook,
                                                        commentsByActivityId = myProfileState.commentsByActivityId,
                                                        loadingCommentIds = myProfileState.loadingCommentIds,
                                                        socialActionIds = myProfileState.socialActionIds,
                                                        onToggleLike = myProfileViewModel::toggleLike,
                                                        onLoadComments = myProfileViewModel::loadComments,
                                                        onAddComment = myProfileViewModel::addComment,
                                                        onToggleCommentLike = myProfileViewModel::toggleCommentLike,
                                                        onReplyToComment = myProfileViewModel::replyToComment,
                                                        deletingActivityIds = myProfileState.deletingActivityIds,
                                                        onDeleteActivityUpdate = myProfileViewModel::hideActivityUpdate,
                                                        showFriendActions = false,
                                                        showActivityDeleteActions = true,
                                                        isScrollable = false,
                                                        contentHorizontalPadding = 2.dp,
                                                        transparentCards = true
                                                    )
                                                }

                                                else -> {
                                                    Text(
                                                        text = myProfileState.errorMessage
                                                            ?: stringResource(R.string.friend_profile_load_error),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 32.dp),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = OldIvory,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        ProfileTab.Identity -> {
                                            ProfileAvatarSection(
                                                displayName = profilePhotoDisplayName,
                                                imageUrl = profilePhotoImageUrl,
                                                isAdmin = uiState.user?.isAdmin == true,
                                                onOpenPhoto = { showProfilePhotoPreview = true },
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

                                            if (!uiState.hasGoogleLogin && !isGuest) {
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

                                            if (isGuest) {
                                                Spacer(modifier = Modifier.height(20.dp))

                                                Button(
                                                    onClick = onGoToRegister,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                                                ) {
                                                    Text(stringResource(R.string.create_account_and_sync))
                                                }
                                            }

                                            if (hasPrimaryPasswordLogin) {
                                                Spacer(modifier = Modifier.height(16.dp))

                                                PasswordChangeFields(
                                                    currentPassword = uiState.accessCurrentPassword,
                                                    password = uiState.accessPassword,
                                                    passwordConfirmation = uiState.accessPasswordConfirmation,
                                                    isLoading = uiState.isLoading,
                                                    onCurrentPasswordChange = viewModel::onAccessCurrentPasswordChange,
                                                    onPasswordChange = viewModel::onAccessPasswordChange,
                                                    onPasswordConfirmationChange = viewModel::onAccessPasswordConfirmationChange,
                                                    onConfirm = viewModel::changeCurrentPassword
                                                )
                                            }

                                            if (!isGuest) {
                                                Spacer(modifier = Modifier.height(20.dp))

                                                LoginProvidersSection(
                                                    providers = uiState.loginProviders,
                                                    isLoading = uiState.isLoading,
                                                    onOpenLoginOptions = {
                                                        viewModel.onAccessEmailChange("")
                                                        showLoginOptionsDialog = true
                                                    },
                                                    onUnlinkProvider = viewModel::unlinkLoginProvider,
                                                    onLinkGoogle = viewModel::linkGoogleLogin,
                                                    onGoogleError = viewModel::showError
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(20.dp))

                                            DataImportSection(
                                                isImporting = uiState.isImportingLibraryData,
                                                processedRows = uiState.importProcessedRows,
                                                totalRows = uiState.importTotalRows,
                                                importedBooks = uiState.importImportedBooks,
                                                onImportCsv = {
                                                    csvImportLauncher.launch(
                                                        arrayOf(
                                                            "text/csv",
                                                            "text/comma-separated-values",
                                                            "text/*",
                                                            "application/vnd.ms-excel"
                                                        )
                                                    )
                                                }
                                            )
                                        }

                                        ProfileTab.Settings -> {
                                            ProfileSettingsContent(
                                                selectedPanel = selectedSettingsPanel,
                                                onSelectedPanelChange = { selectedSettingsPanel = it },
                                                onSelectLanguage = { language ->
                                                    activity?.let {
                                                        LanguageManager.setLanguage(it, language)
                                                    }
                                                },
                                                readerListAutomationMode = readerListAutomationMode,
                                                onReaderListAutomationModeChange = { mode ->
                                                    readerListAutomationMode = mode
                                                    saveReaderListAutomationMode(context, mode)
                                                },
                                                searchIntroAnimationEnabled = privacySettings.searchIntroAnimationEnabled != false,
                                                onSearchIntroAnimationEnabledChange = {
                                                    viewModel.updatePrivacySettings(
                                                        privacySettings.copy(searchIntroAnimationEnabled = it)
                                                    )
                                                },
                                                blockedMembers = myProfileState.blockedMembers,
                                                reports = myProfileState.accountReports,
                                                isLoadingBlockedMembers = myProfileState.isLoadingBlockedMembers,
                                                isLoadingReports = myProfileState.isLoadingReports,
                                                isSavingReportReply = myProfileState.isSavingReportReply,
                                                onUnblockMember = myProfileViewModel::unblockMember,
                                                onReplyToReport = myProfileViewModel::replyToReportReview,
                                                initialReportReviewId = pendingInitialReportReviewId,
                                                onInitialReportReviewConsumed = {
                                                    pendingInitialReportReviewId = null
                                                    onInitialReportReviewHandled()
                                                },
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

                            Canvas(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(top = 51.dp)
                                    .height(18.dp)
                            ) {
                                val cornerRadius = 18.dp.toPx()
                                val strokeWidth = 1.dp.toPx()
                                val topBorderPath = Path().apply {
                                    moveTo(0f, cornerRadius)
                                    quadraticTo(0f, 0f, cornerRadius, 0f)
                                    lineTo(size.width - cornerRadius, 0f)
                                    quadraticTo(size.width, 0f, size.width, cornerRadius)
                                }

                                drawPath(
                                    path = topBorderPath,
                                    color = TarnishedGold.copy(alpha = 0.86f),
                                    style = Stroke(width = strokeWidth)
                                )
                            }

                            ProfileTabSelector(
                                selectedTab = selectedProfileTab,
                                guestRestricted = isGuest,
                                onSelectTab = { tab ->
                                    if (isGuest && tab.isGuestRestricted) {
                                        guestUiRestrictions.onBlockedSectionClick?.invoke(KaiSection.PROFILE)
                                    } else {
                                        selectedProfileTab = tab
                                    }
                                },
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
    }
}

@Composable
private fun DataImportSection(
    isImporting: Boolean,
    processedRows: Int,
    totalRows: Int,
    importedBooks: Int,
    onImportCsv: () -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_data_import_section_title)) {
        Text(
            text = stringResource(R.string.profile_data_import_section_body),
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory.copy(alpha = 0.76f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isImporting) {
            val progress = if (totalRows > 0) {
                processedRows.toFloat() / totalRows.toFloat()
            } else {
                0f
            }
            Text(
                text = stringResource(
                    R.string.profile_import_progress,
                    processedRows,
                    totalRows,
                    importedBooks
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = TarnishedGold,
                trackColor = TarnishedGold.copy(alpha = 0.18f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onImportCsv,
            enabled = !isImporting,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TarnishedGold,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TarnishedGold
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(R.string.profile_import_csv_action),
                color = TarnishedGold
            )
        }
    }
}

private enum class ProfileTab {
    MyProfile,
    Identity,
    Settings,
    Privacy;

    val isGuestRestricted: Boolean
        get() = this == MyProfile || this == Privacy
}

private enum class ProfileSettingsPanel {
    Main,
    BlockedMembers,
    ReportReview
}

@Composable
private fun ProfileTabSelector(
    selectedTab: ProfileTab,
    guestRestricted: Boolean,
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
            text = stringResource(R.string.profile_tab_my_profile),
            selected = selectedTab == ProfileTab.MyProfile,
            restricted = guestRestricted,
            onClick = { onSelectTab(ProfileTab.MyProfile) },
            modifier = Modifier.weight(1f)
        )
        ProfileTabButton(
            text = stringResource(R.string.profile_tab_identity),
            selected = selectedTab == ProfileTab.Identity,
            restricted = false,
            onClick = { onSelectTab(ProfileTab.Identity) },
            modifier = Modifier.weight(1f)
        )
        ProfileTabButton(
            text = stringResource(R.string.profile_tab_settings),
            selected = selectedTab == ProfileTab.Settings,
            restricted = false,
            onClick = { onSelectTab(ProfileTab.Settings) },
            modifier = Modifier.weight(1f)
        )
        ProfileTabButton(
            text = stringResource(R.string.profile_tab_privacy),
            selected = selectedTab == ProfileTab.Privacy,
            restricted = guestRestricted,
            onClick = { onSelectTab(ProfileTab.Privacy) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileTabButton(
    text: String,
    selected: Boolean,
    restricted: Boolean,
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
                containerColor = if (restricted) BloodWine.copy(alpha = 0.28f) else Color.Transparent,
                contentColor = when {
                    restricted -> OldIvory.copy(alpha = 0.56f)
                    selected -> OldIvory
                    else -> TarnishedGold
                }
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
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
private fun PasswordChangeFields(
    currentPassword: String,
    password: String,
    passwordConfirmation: String,
    isLoading: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    PasswordOutlinedTextField(
        value = currentPassword,
        onValueChange = onCurrentPasswordChange,
        label = { Text(stringResource(R.string.profile_current_password)) },
        modifier = Modifier.fillMaxWidth(),
        colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    PasswordOutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.profile_new_password)) },
        modifier = Modifier.fillMaxWidth(),
        colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    PasswordOutlinedTextField(
        value = passwordConfirmation,
        onValueChange = onPasswordConfirmationChange,
        label = { Text(stringResource(R.string.profile_confirm_password)) },
        modifier = Modifier.fillMaxWidth(),
        colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onConfirm,
        enabled = !isLoading,
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Text(
            text = stringResource(R.string.profile_change_password),
            color = TarnishedGold
        )
    }
}

@Composable
private fun LoginProvidersSection(
    providers: List<LoginProviderState>,
    isLoading: Boolean,
    onOpenLoginOptions: () -> Unit,
    onUnlinkProvider: (String) -> Unit,
    onLinkGoogle: (String) -> Unit,
    onGoogleError: (String) -> Unit
) {
    var providerPendingRemoval by remember { mutableStateOf<LoginProviderState?>(null) }
    val pendingProvider = providerPendingRemoval

    if (pendingProvider != null) {
        val providerName = providerDisplayName(pendingProvider.providerId)
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    providerPendingRemoval = null
                }
            },
            title = {
                Text(text = stringResource(R.string.profile_remove_login_method_title))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.profile_remove_login_method_message,
                        providerName
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        providerPendingRemoval = null
                        onUnlinkProvider(pendingProvider.providerId)
                    },
                    enabled = !isLoading
                ) {
                    Text(text = stringResource(R.string.profile_remove_login_method))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { providerPendingRemoval = null },
                    enabled = !isLoading
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            containerColor = Obsidian,
            titleContentColor = OldIvory,
            textContentColor = OldIvory.copy(alpha = 0.9f)
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_login_methods_section_title)) {
        providers.forEachIndexed { index, provider ->
            LoginProviderRow(
                provider = provider,
                isLoading = isLoading,
                onOpenLoginOptions = onOpenLoginOptions,
                onUnlinkProvider = { _ -> providerPendingRemoval = provider },
                onLinkGoogle = onLinkGoogle,
                onGoogleError = onGoogleError
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
    onUnlinkProvider: (String) -> Unit,
    onLinkGoogle: (String) -> Unit,
    onGoogleError: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderLogo(
            providerId = provider.providerId,
            contentDescription = providerDisplayName(provider.providerId)
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

            provider.providerId == GOOGLE_PROVIDER_ID -> {
                GoogleSignInButton(
                    enabled = !isLoading,
                    onIdTokenReceived = onLinkGoogle,
                    onError = onGoogleError,
                    fillMaxWidth = false,
                    label = stringResource(R.string.profile_link_login_short_action)
                )
            }
        }
    }
}

@Composable
private fun ProviderLogo(
    providerId: String,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OldIvory)
            .border(
                width = 1.dp,
                color = TarnishedGold,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(providerIconRes(providerId)),
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp),
            colorFilter = providerIconColorFilter(providerId)
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

private fun providerIconRes(providerId: String): Int {
    return when (providerId) {
        GOOGLE_PROVIDER_ID -> R.drawable.sesion_google
        PASSWORD_PROVIDER_ID -> R.drawable.sesion_email
        FACEBOOK_PROVIDER_ID -> R.drawable.sesion_facebook
        APPLE_PROVIDER_ID -> R.drawable.sesion_apple
        GITHUB_PROVIDER_ID -> R.drawable.sesion_github
        else -> R.drawable.sesion_email
    }
}

@Composable
private fun PasswordLoginDialog(
    email: String,
    password: String,
    passwordConfirmation: String,
    isLoading: Boolean,
    hasPasswordLogin: Boolean,
    message: String?,
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

                PasswordOutlinedTextField(
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
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                PasswordOutlinedTextField(
                    value = passwordConfirmation,
                    onValueChange = onPasswordConfirmationChange,
                    label = { Text(stringResource(R.string.profile_confirm_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                    singleLine = true
                )

                message?.let {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
    selectedPanel: ProfileSettingsPanel,
    onSelectedPanelChange: (ProfileSettingsPanel) -> Unit,
    onSelectLanguage: (String) -> Unit,
    readerListAutomationMode: ReaderListAutomationMode,
    onReaderListAutomationModeChange: (ReaderListAutomationMode) -> Unit,
    searchIntroAnimationEnabled: Boolean,
    onSearchIntroAnimationEnabledChange: (Boolean) -> Unit,
    blockedMembers: List<BlockedMember>,
    reports: List<AccountReport>,
    isLoadingBlockedMembers: Boolean,
    isLoadingReports: Boolean,
    isSavingReportReply: Boolean,
    onUnblockMember: (String) -> Unit,
    onReplyToReport: (String, String, List<String>) -> Unit,
    initialReportReviewId: String? = null,
    onInitialReportReviewConsumed: () -> Unit = {},
    onLogout: () -> Unit
) {
    when (selectedPanel) {
        ProfileSettingsPanel.BlockedMembers -> {
            ProfileSettingsBackButton(
                text = stringResource(R.string.profile_settings_back),
                onClick = { onSelectedPanelChange(ProfileSettingsPanel.Main) }
            )
            BlockedMembersSettingsSection(
                blockedMembers = blockedMembers,
                isLoading = isLoadingBlockedMembers,
                onUnblockMember = onUnblockMember
            )
            return
        }

        ProfileSettingsPanel.ReportReview -> {
            ReportReviewSettingsSection(
                reports = reports,
                isLoading = isLoadingReports,
                isSavingReply = isSavingReportReply,
                onBackToSettings = { onSelectedPanelChange(ProfileSettingsPanel.Main) },
                onReplyToReport = onReplyToReport,
                initialReportReviewId = initialReportReviewId,
                onInitialReportReviewConsumed = onInitialReportReviewConsumed
            )
            return
        }

        ProfileSettingsPanel.Main -> Unit
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_settings_language)) {
        LanguageSection(
            onSelectLanguage = onSelectLanguage
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_settings_session_preferences)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_search_intro_animation),
            body = stringResource(R.string.profile_search_intro_animation_body),
            checked = searchIntroAnimationEnabled,
            onCheckedChange = onSearchIntroAnimationEnabledChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ReaderListAutomationSettings(
            selectedMode = readerListAutomationMode,
            onSelectedModeChange = onReaderListAutomationModeChange
        )
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_settings_social_safety)) {
        ProfileSettingsActionRow(
            title = stringResource(R.string.profile_blocked_people),
            body = stringResource(R.string.profile_blocked_people_body),
            onClick = { onSelectedPanelChange(ProfileSettingsPanel.BlockedMembers) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TarnishedGold.copy(alpha = 0.18f)
        )

        ProfileSettingsActionRow(
            title = stringResource(R.string.profile_report_review),
            body = stringResource(R.string.profile_report_review_body),
            onClick = { onSelectedPanelChange(ProfileSettingsPanel.ReportReview) }
        )
    }

    Button(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth(),
        colors = KaiShelvesThemeDefaults.primaryButtonColors()
    ) {
        Text(stringResource(R.string.logout))
    }
}

@Composable
private fun ReaderListAutomationSettings(
    selectedMode: ReaderListAutomationMode,
    onSelectedModeChange: (ReaderListAutomationMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.profile_reader_list_automation),
            style = MaterialTheme.typography.bodyLarge,
            color = OldIvory
        )
        Text(
            text = stringResource(R.string.profile_reader_list_automation_body),
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory.copy(alpha = 0.74f)
        )

        ReaderListAutomationOption(
            title = stringResource(R.string.reader_list_automation_ask),
            body = stringResource(R.string.reader_list_automation_ask_body),
            selected = selectedMode == ReaderListAutomationMode.Ask,
            onClick = { onSelectedModeChange(ReaderListAutomationMode.Ask) }
        )
        ReaderListAutomationOption(
            title = stringResource(R.string.reader_list_automation_automatic),
            body = stringResource(R.string.reader_list_automation_automatic_body),
            selected = selectedMode == ReaderListAutomationMode.Automatic,
            onClick = { onSelectedModeChange(ReaderListAutomationMode.Automatic) }
        )
        ReaderListAutomationOption(
            title = stringResource(R.string.reader_list_automation_disabled),
            body = stringResource(R.string.reader_list_automation_disabled_body),
            selected = selectedMode == ReaderListAutomationMode.Disabled,
            onClick = { onSelectedModeChange(ReaderListAutomationMode.Disabled) }
        )
    }
}

@Composable
private fun ReaderListAutomationOption(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = TarnishedGold,
                unselectedColor = OldIvory.copy(alpha = 0.64f)
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = OldIvory.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun ProfileSettingsBackButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(bottom = 12.dp),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Text(text = text, color = TarnishedGold)
    }
}

@Composable
private fun ProfileSettingsActionRow(
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Text(text = stringResource(R.string.open), color = TarnishedGold)
        }
    }
}

@Composable
private fun BlockedMembersSettingsSection(
    blockedMembers: List<BlockedMember>,
    isLoading: Boolean,
    onUnblockMember: (String) -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_blocked_people)) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TarnishedGold)
                }
            }

            blockedMembers.isEmpty() -> {
                Text(
                    text = stringResource(R.string.profile_no_blocked_people),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.8f)
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    blockedMembers.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KaiUserAvatar(
                                displayName = member.user.usuario.ifBlank { member.user.email },
                                imageUrl = member.user.photoUrl,
                                modifier = Modifier.size(48.dp),
                                size = 48.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.user.usuario.ifBlank { stringResource(R.string.unknown_username) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OldIvory
                                )
                                Text(
                                    text = member.user.email.ifBlank { stringResource(R.string.no_email_available) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OldIvory.copy(alpha = 0.66f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onUnblockMember(member.user.uid) },
                                border = BorderStroke(1.dp, TarnishedGold)
                            ) {
                                Text(
                                    text = stringResource(R.string.unblock_member),
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

@Composable
private fun ReportReviewSettingsSection(
    reports: List<AccountReport>,
    isLoading: Boolean,
    isSavingReply: Boolean,
    onBackToSettings: () -> Unit,
    onReplyToReport: (String, String, List<String>) -> Unit,
    initialReportReviewId: String? = null,
    onInitialReportReviewConsumed: () -> Unit = {}
) {
    var selectedReportId by remember { mutableStateOf<String?>(null) }
    val selectedReport = reports.firstOrNull { it.id == selectedReportId }
    var replyText by remember(selectedReport?.id) {
        mutableStateOf("")
    }
    val backToReports = { selectedReportId = null }

    BackHandler(enabled = selectedReport != null) {
        backToReports()
    }

    LaunchedEffect(initialReportReviewId, reports) {
        val reportId = initialReportReviewId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (reports.any { it.id == reportId }) {
            selectedReportId = reportId
            onInitialReportReviewConsumed()
        }
    }

    if (selectedReport != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileSettingsBackButton(
                text = stringResource(R.string.profile_reports_back),
                onClick = backToReports
            )
            ReportReviewDetail(
                report = selectedReport,
                replyText = replyText,
                isSavingReply = isSavingReply,
                onReplyTextChange = { replyText = it },
                onReplyToReport = { text, imageUris ->
                    onReplyToReport(selectedReport.id, text, imageUris)
                }
            )
        }
        return
    }

    ProfileSectionBlock(title = stringResource(R.string.profile_report_review)) {
        ProfileSettingsBackButton(
            text = stringResource(R.string.profile_settings_back),
            onClick = onBackToSettings
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TarnishedGold)
                }
            }

            reports.isEmpty() -> {
                Text(
                    text = stringResource(R.string.profile_no_reports),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.8f)
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    reports.forEach { report ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReportId = report.id }
                                .border(
                                    width = 1.dp,
                                    color = TarnishedGold.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(R.string.report_identifier)}: ${report.publicId.ifBlank { report.id }}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TarnishedGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                ReportStatusPill(status = report.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.subject,
                                style = MaterialTheme.typography.titleMedium,
                                color = OldIvory
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.reportedUser.usuario.ifBlank { stringResource(R.string.unknown_username) },
                                modifier = Modifier
                                    .background(
                                        color = BloodWine.copy(alpha = 0.36f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TarnishedGold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.open),
                                style = MaterialTheme.typography.labelLarge,
                                color = TarnishedGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatusPill(status: AccountReportStatus) {
    Text(
        text = reportStatusLabel(status),
        modifier = Modifier
            .background(
                color = BloodWine.copy(alpha = 0.36f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = TarnishedGold,
        maxLines = 1
    )
}

@Composable
private fun ReportReviewDetail(
    report: AccountReport,
    replyText: String,
    isSavingReply: Boolean,
    onReplyTextChange: (String) -> Unit,
    onReplyToReport: (String, List<String>) -> Unit
) {
    var expandedImageSource by remember { mutableStateOf<String?>(null) }
    val chatScrollState = rememberScrollState()

    expandedImageSource?.let { imageSource ->
        UserReportImagePreviewDialog(
            imageSource = imageSource,
            onDismiss = { expandedImageSource = null }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = report.subject,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory
            )
            ReportStatusPill(status = report.status)
        }
        Text(
            text = "${stringResource(R.string.report_identifier)}: ${report.publicId.ifBlank { report.id }}",
            style = MaterialTheme.typography.labelMedium,
            color = TarnishedGold
        )
        Text(
            text = report.reportedUser.usuario.ifBlank { stringResource(R.string.unknown_username) },
            modifier = Modifier
                .background(
                    color = BloodWine.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = TarnishedGold
        )

        HorizontalDivider(color = TarnishedGold.copy(alpha = 0.18f))

        UserReportConversationBox(
            report = report,
            replyText = replyText,
            isSavingReply = isSavingReply,
            scrollState = chatScrollState,
            onReplyTextChange = onReplyTextChange,
            onReplyToReport = onReplyToReport,
            onOpenImage = { expandedImageSource = it }
        )
    }
}

@Composable
private fun UserReportConversationBox(
    report: AccountReport,
    replyText: String,
    isSavingReply: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onReplyTextChange: (String) -> Unit,
    onReplyToReport: (String, List<String>) -> Unit,
    onOpenImage: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val chatScope = rememberCoroutineScope()
    var pendingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pendingImages = pendingImages + uris.mapNotNull { uri ->
            runCatching {
                ProfileImageCodec.encodeImageAsDataUri(context, Uri.parse(uri.toString()))
            }.getOrNull()
        }
    }
    LaunchedEffect(report.chatMessages.size, report.chatMessages.sumOf { it.imageUris.size }) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .border(
                width = 1.dp,
                color = TarnishedGold.copy(alpha = 0.28f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                report.chatMessages.forEach { message ->
                    val isAdminMessage = message.sender == REPORT_SENDER_ADMIN
                    val alignEnd = !isAdminMessage
                    val title = stringResource(
                        if (isAdminMessage) R.string.chat_admin else R.string.chat_you
                    )
                    if (message.text.isNotBlank()) {
                        UserReportChatBubble(
                            title = title,
                            body = message.text,
                            alignEnd = alignEnd
                        )
                    }
                    message.imageUris.forEach { uri ->
                        UserReportImageBubble(
                            title = title,
                            imageSource = uri,
                            alignEnd = alignEnd,
                            onOpenImage = { onOpenImage(uri) }
                        )
                    }
                }
            }

            if (scrollState.maxValue > 0) {
                val thumbHeight = (maxHeight * 0.35f).coerceAtLeast(36.dp)
                val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                val trackRangePx = with(density) { (maxHeight - thumbHeight).toPx() }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(TarnishedGold.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (maxHeight - thumbHeight) * scrollFraction)
                        .width(3.dp)
                        .height(thumbHeight)
                        .background(TarnishedGold.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                        .pointerInput(scrollState.maxValue, thumbHeight) {
                            detectDragGestures { _, dragAmount ->
                                if (trackRangePx > 0f) {
                                    val nextValue = (
                                        scrollState.value + (dragAmount.y / trackRangePx) * scrollState.maxValue
                                        ).toInt().coerceIn(0, scrollState.maxValue)
                                    chatScope.launch {
                                        scrollState.scrollTo(nextValue)
                                    }
                                }
                            }
                        }
                )
            }
        }

        if (pendingImages.isNotEmpty()) {
            Text(
                text = stringResource(R.string.report_photos_selected, pendingImages.size),
                style = MaterialTheme.typography.labelMedium,
                color = TarnishedGold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = onReplyTextChange,
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 3,
                colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
            )
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isSavingReply
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.report_add_photos),
                    tint = TarnishedGold
                )
            }
            IconButton(
                onClick = {
                    val textToSend = replyText
                    onReplyTextChange("")
                    onReplyToReport(textToSend, pendingImages)
                    pendingImages = emptyList()
                },
                enabled = (replyText.isNotBlank() || pendingImages.isNotEmpty()) && !isSavingReply
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = TarnishedGold
                )
            }
        }
    }
}

@Composable
private fun UserReportChatBubble(
    title: String,
    body: String,
    alignEnd: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(
                    color = if (alignEnd) BloodWine.copy(alpha = 0.38f) else Obsidian.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = TarnishedGold.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TarnishedGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun UserReportImageBubble(
    title: String,
    imageSource: String,
    alignEnd: Boolean,
    onOpenImage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TarnishedGold
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    UserReportImage(
                        imageSource = imageSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clickable(onClick = onOpenImage)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserReportImagePreviewDialog(
    imageSource: String,
    onDismiss: () -> Unit
) {
    var scale by remember(imageSource) { mutableStateOf(1f) }
    var rotation by remember(imageSource) { mutableStateOf(0f) }
    var offset by remember(imageSource) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .clickable { }
                    .pointerInput(imageSource) {
                        detectTransformGestures { _, pan, zoom, gestureRotation ->
                            scale = (scale * zoom).coerceIn(0.75f, 5f)
                            rotation += gestureRotation
                            offset += pan
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentAlignment = Alignment.Center
            ) {
                UserReportImage(
                    imageSource = imageSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun UserReportImage(
    imageSource: String,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Crop
) {
    val bitmap = remember(imageSource) {
        runCatching {
            val base64Payload = imageSource.substringAfter("base64,", "")
            if (!imageSource.startsWith("data:image") || base64Payload.isBlank()) {
                null
            } else {
                val bytes = Base64.decode(base64Payload, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.admin_report_image),
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        AsyncImage(
            model = imageSource,
            contentDescription = stringResource(R.string.admin_report_image),
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
private fun reportStatusLabel(status: AccountReportStatus): String {
    return stringResource(
        when (status) {
            AccountReportStatus.NEW -> R.string.report_status_new
            AccountReportStatus.IN_PROGRESS -> R.string.report_status_in_progress
            AccountReportStatus.PROCESSED -> R.string.report_status_processed
            AccountReportStatus.CLOSED -> R.string.report_status_closed
        }
    )
}

@Composable
private fun ProfilePrivacyContent(
    privacySettings: UserPrivacySettings,
    onPrivacySettingsChange: (UserPrivacySettings) -> Unit
) {
    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_visibility)) {
        ProfileToggleRow(
            title = stringResource(
                if (privacySettings.profileVisible) {
                    R.string.profile_visibility_enabled
                } else {
                    R.string.profile_visibility_disabled
                }
            ),
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

    ProfileSectionBlock(title = stringResource(R.string.profile_privacy_personalization)) {
        ProfileToggleRow(
            title = stringResource(R.string.profile_personalized_suggestions),
            body = stringResource(R.string.profile_personalized_suggestions_body),
            checked = privacySettings.personalizedSuggestions,
            onCheckedChange = {
                onPrivacySettingsChange(privacySettings.copy(personalizedSuggestions = it))
            }
        )
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
    onOpenPhoto: () -> Unit,
    onChangePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KaiUserAvatar(
            displayName = displayName,
            imageUrl = imageUrl,
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenPhoto),
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
private fun ProfilePhotoPreviewDialog(
    displayName: String,
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.72f))
        ) {
            Box(
                modifier = Modifier
                    .padding(18.dp)
                    .size(292.dp),
                contentAlignment = Alignment.Center
            ) {
                KaiUserAvatar(
                    displayName = displayName,
                    imageUrl = imageUrl,
                    modifier = Modifier.size(292.dp),
                    size = 260.dp
                )
            }
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
    onSelectLanguage: (String) -> Unit
) {
    val currentLanguage = LanguageManager.getCurrentLanguage()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LanguageOptionButton(
            text = stringResource(R.string.spanish),
            selected = currentLanguage == "es",
            onClick = { onSelectLanguage("es") },
            modifier = Modifier.weight(1f)
        )

        LanguageOptionButton(
            text = stringResource(R.string.english),
            selected = currentLanguage == "en",
            onClick = { onSelectLanguage("en") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LanguageOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = TarnishedGold,
                contentColor = Obsidian
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = shape,
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.72f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OldIvory
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
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

private fun providerIconColorFilter(providerId: String): ColorFilter? {
    return when (providerId) {
        GITHUB_PROVIDER_ID,
        APPLE_PROVIDER_ID,
        PASSWORD_PROVIDER_ID -> ColorFilter.tint(Obsidian)
        else -> null
    }
}

private const val GOOGLE_PROVIDER_ID = "google.com"
private const val PASSWORD_PROVIDER_ID = "password"
private const val FACEBOOK_PROVIDER_ID = "facebook.com"
private const val APPLE_PROVIDER_ID = "apple.com"
private const val GITHUB_PROVIDER_ID = "github.com"
