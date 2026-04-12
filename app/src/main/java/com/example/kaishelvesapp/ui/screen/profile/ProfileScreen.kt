package com.example.kaishelvesapp.ui.screen.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var expandedLanguage by remember { mutableStateOf(false) }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                headerTitle = stringResource(R.string.profile),
                subtitle = stringResource(R.string.profile_subtitle),
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
                    onGoToProfile = {},
                    onGoToSettingsPrivacy = onGoToSettingsPrivacy,
                    onLogout = onLogout
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

                                    Button(
                                        onClick = { viewModel.saveProfileChanges() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = KaiShelvesThemeDefaults.primaryButtonColors(),
                                        enabled = !uiState.isLoading
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(color = OldIvory)
                                        } else {
                                            Text(stringResource(R.string.save))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = { viewModel.cancelEditingProfile() },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, TarnishedGold)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cancel),
                                            color = TarnishedGold
                                        )
                                    }
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
                                        onClick = { viewModel.startEditingProfile() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                                    ) {
                                        Text(stringResource(R.string.edit_profile))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

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
