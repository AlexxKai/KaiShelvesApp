package com.example.kaishelvesapp.ui.screen.settings

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.AccountReport
import com.example.kaishelvesapp.data.repository.AccountReportKind
import com.example.kaishelvesapp.data.repository.AccountReportStatus
import com.example.kaishelvesapp.data.repository.REPORT_SENDER_ADMIN
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AdminReportFilter
import com.example.kaishelvesapp.ui.viewmodel.AdminReportsViewModel
import com.example.kaishelvesapp.ui.viewmodel.matchesAdminFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    viewModel: AdminReportsViewModel,
    reportKind: AccountReportKind = AccountReportKind.REPORT,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    userName: String? = null,
    profileImageUrl: String? = null,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.observeReports()
    }

    LaunchedEffect(reportKind) {
        viewModel.setReportKind(reportKind)
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    BackHandler(enabled = uiState.selectedReport != null) {
        viewModel.closeReportDetail()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.ADMIN,
                subtitle = stringResource(R.string.admin_reports_panel_title),
                userName = userName.orEmpty(),
                profileImageUrl = profileImageUrl.orEmpty(),
                expanded = drawerExpanded,
                onGoToProfile = {
                    scope.launch { drawerState.close() }
                    onGoToProfile()
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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    current = KaiSection.ADMIN,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding),
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::loadReports
            ) {
                GothicBackground(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.selectedReport != null) {
                            AdminReportDetail(
                                report = uiState.selectedReport!!,
                                selectedStatus = uiState.selectedStatus,
                                adminMessage = uiState.adminReplyDraft,
                                isSaving = uiState.isSaving,
                                onBack = viewModel::closeReportDetail,
                                onStatusChange = viewModel::onStatusChange,
                                onAdminMessageChange = viewModel::onAdminMessageChange,
                                onSave = viewModel::saveSelectedReport,
                                onSendMessage = viewModel::sendAdminReply
                            )
                        } else {
                            AdminReportsList(
                                reportKind = uiState.reportKind,
                                reports = uiState.reports.filter { it.matchesAdminFilter(uiState.filter, uiState.reportKind) },
                                selectedFilter = uiState.filter,
                                isLoading = uiState.isLoading,
                                onFilterChange = viewModel::onFilterChange,
                                onOpenReport = viewModel::selectReport
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReportsList(
    reportKind: AccountReportKind,
    reports: List<AccountReport>,
    selectedFilter: AdminReportFilter,
    isLoading: Boolean,
    onFilterChange: (AdminReportFilter) -> Unit,
    onOpenReport: (AccountReport) -> Unit
) {
    Text(
        text = if (reportKind == AccountReportKind.REQUEST) {
            stringResource(R.string.admin_requests_panel_title)
        } else {
            stringResource(R.string.admin_reports_panel_title)
        },
        style = MaterialTheme.typography.headlineSmall,
        color = TarnishedGold
    )

    AdminReportFilterBar(
        selectedFilter = selectedFilter,
        onFilterChange = onFilterChange
    )

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TarnishedGold)
            }
        }

        reports.isEmpty() -> {
            Text(
                text = if (reportKind == AccountReportKind.REQUEST) {
                    stringResource(R.string.admin_requests_empty)
                } else {
                    stringResource(R.string.admin_reports_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.82f)
            )
        }

        else -> reports.forEach { report ->
            AdminReportCard(report = report, onClick = { onOpenReport(report) })
        }
    }
}

@Composable
private fun AdminReportFilterBar(
    selectedFilter: AdminReportFilter,
    onFilterChange: (AdminReportFilter) -> Unit
) {
    val rows = listOf(
        listOf(
            AdminReportFilter.ALL to "Todas",
            AdminReportFilter.NEW to accountReportStatusLabel(AccountReportStatus.NEW),
            AdminReportFilter.IN_PROGRESS to accountReportStatusLabel(AccountReportStatus.IN_PROGRESS)
        ),
        listOf(
            AdminReportFilter.PROCESSED to accountReportStatusLabel(AccountReportStatus.PROCESSED),
            AdminReportFilter.CLOSED to accountReportStatusLabel(AccountReportStatus.CLOSED),
            AdminReportFilter.WAITING_USER_INFO to "Pendiente usuario"
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowFilters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowFilters.forEach { (filter, label) ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (selectedFilter == filter) {
                                    BloodWine.copy(alpha = 0.42f)
                                } else {
                                    Obsidian.copy(alpha = 0.74f)
                                },
                                shape = RoundedCornerShape(999.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = TarnishedGold.copy(alpha = if (selectedFilter == filter) 0.72f else 0.22f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedFilter == filter) TarnishedGold else OldIvory.copy(alpha = 0.84f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminReportCard(report: AccountReport, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.publicId.ifBlank { report.id },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = TarnishedGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusPill(status = report.status)
            }
            Text(
                text = report.subject,
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory
            )
        }
    }
}

@Composable
private fun AdminReportDetail(
    report: AccountReport,
    selectedStatus: AccountReportStatus,
    adminMessage: String,
    isSaving: Boolean,
    onBack: () -> Unit,
    onStatusChange: (AccountReportStatus) -> Unit,
    onAdminMessageChange: (String) -> Unit,
    onSave: () -> Unit,
    onSendMessage: (List<String>) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = TarnishedGold
            )
        }
        Text(
            text = report.publicId.ifBlank { report.id },
            style = MaterialTheme.typography.titleMedium,
            color = TarnishedGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    AdminReportInfoCard(
        report = report,
        selectedStatus = selectedStatus,
        adminMessage = adminMessage,
        isSaving = isSaving,
        onStatusChange = onStatusChange,
        onAdminMessageChange = onAdminMessageChange,
        onSaveStatus = onSave,
        onSendMessage = onSendMessage
    )
}

@Composable
private fun AdminReportInfoCard(
    report: AccountReport,
    selectedStatus: AccountReportStatus,
    adminMessage: String,
    isSaving: Boolean,
    onStatusChange: (AccountReportStatus) -> Unit,
    onAdminMessageChange: (String) -> Unit,
    onSaveStatus: () -> Unit,
    onSendMessage: (List<String>) -> Unit
) {
    var expandedImageSource by remember { mutableStateOf<String?>(null) }
    val chatScrollState = rememberScrollState()

    expandedImageSource?.let { imageSource ->
        ReportImagePreviewDialog(
            imageSource = imageSource,
            onDismiss = { expandedImageSource = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(report.subject, style = MaterialTheme.typography.titleLarge, color = OldIvory)
        if (report.isAdministrativeReview) {
            Text(
                text = stringResource(R.string.admin_report_administrative_review),
                style = MaterialTheme.typography.bodyMedium,
                color = TarnishedGold
            )
        } else {
            ReportLine(
                if (report.kind == AccountReportKind.REQUEST) R.string.support_request_user else R.string.admin_report_reporter,
                report.reporterUser.usuario.ifBlank { report.reporterUser.email }
            )
        }
        if (report.kind == AccountReportKind.REPORT) {
            ReportLine(R.string.admin_report_reported, report.reportedUser.usuario.ifBlank { report.reportedUser.email })
        }
        report.createdAtMillis?.let {
            ReportLine(R.string.admin_report_created_at, formatAdminReportDate(it))
        }

        AdminReportStatusControls(
            selectedStatus = selectedStatus,
            isSaving = isSaving,
            onStatusChange = onStatusChange,
            onSaveStatus = onSaveStatus
        )

        ReportConversationBox(
            report = report,
            adminMessage = adminMessage,
            isSaving = isSaving,
            scrollState = chatScrollState,
            onAdminMessageChange = onAdminMessageChange,
            onSendMessage = onSendMessage,
            onOpenImage = { expandedImageSource = it }
        )
    }
}

@Composable
private fun AdminReportStatusControls(
    selectedStatus: AccountReportStatus,
    isSaving: Boolean,
    onStatusChange: (AccountReportStatus) -> Unit,
    onSaveStatus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccountReportStatus.entries.forEach { status ->
            StatusPill(
                status = status,
                selected = selectedStatus == status,
                modifier = Modifier.clickable { onStatusChange(status) }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onSaveStatus,
            enabled = !isSaving
        ) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = stringResource(R.string.save),
                tint = TarnishedGold
            )
        }
    }
}

@Composable
private fun ReportConversationBox(
    report: AccountReport,
    adminMessage: String,
    isSaving: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onAdminMessageChange: (String) -> Unit,
    onSendMessage: (List<String>) -> Unit,
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
            .height(460.dp)
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
                    val title = if (isAdminMessage) {
                        stringResource(R.string.chat_you)
                    } else {
                        report.reporterUser.usuario.ifBlank { stringResource(R.string.chat_you) }
                    }
                    if (message.text.isNotBlank()) {
                        ReportChatBubble(
                            title = title,
                            body = message.text,
                            alignEnd = isAdminMessage
                        )
                    }
                    message.imageUris.forEach { uri ->
                        ReportImageBubble(
                            title = title,
                            imageSource = uri,
                            alignEnd = isAdminMessage,
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
                value = adminMessage,
                onValueChange = onAdminMessageChange,
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 3,
                colors = com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults.outlinedTextFieldColors()
            )
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isSaving
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.report_add_photos),
                    tint = TarnishedGold
                )
            }
            IconButton(
                onClick = {
                    onSendMessage(pendingImages)
                    pendingImages = emptyList()
                },
                enabled = (adminMessage.isNotBlank() || pendingImages.isNotEmpty()) && !isSaving
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
private fun ReportChatBubble(
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
private fun ReportImageBubble(
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
                ReportImage(
                    imageSource = imageSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable(onClick = onOpenImage)
                )
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = TarnishedGold.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                )
                }
            }
        }
    }
}

@Composable
private fun ReportImagePreviewDialog(
    imageSource: String,
    onDismiss: () -> Unit
) {
    var scale by remember(imageSource) { mutableFloatStateOf(1f) }
    var rotation by remember(imageSource) { mutableFloatStateOf(0f) }
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
                ReportImage(
                    imageSource = imageSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun ReportImage(
    imageSource: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
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
private fun ReportLine(labelRes: Int, value: String) {
    Text(
        text = "${stringResource(labelRes)}: ${value.ifBlank { stringResource(R.string.not_available) }}",
        style = MaterialTheme.typography.bodyMedium,
        color = OldIvory.copy(alpha = 0.78f)
    )
}

@Composable
private fun StatusPill(
    status: AccountReportStatus,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = accountReportStatusLabel(status),
        modifier = modifier
            .background(
                color = if (selected) BloodWine.copy(alpha = 0.78f) else BloodWine.copy(alpha = 0.36f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = TarnishedGold,
        maxLines = 1
    )
}

@Composable
private fun accountReportStatusLabel(status: AccountReportStatus): String {
    return stringResource(
        when (status) {
            AccountReportStatus.NEW -> R.string.report_status_new
            AccountReportStatus.IN_PROGRESS -> R.string.report_status_in_progress
            AccountReportStatus.PROCESSED -> R.string.report_status_processed
            AccountReportStatus.CLOSED -> R.string.report_status_closed
        }
    )
}

private fun formatAdminReportDate(timestampMillis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(timestampMillis))
}
