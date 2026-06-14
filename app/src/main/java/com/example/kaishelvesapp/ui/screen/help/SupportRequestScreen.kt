package com.example.kaishelvesapp.ui.screen.help

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.SupportRequestViewModel

@Composable
fun SupportRequestScreen(
    viewModel: SupportRequestViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addImages(
            uris.mapNotNull { uri ->
                runCatching {
                    ProfileImageCodec.encodeImageAsDataUri(context, Uri.parse(uri.toString()))
                }.getOrNull()
            }
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.prepareNewRequest()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        GothicBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = stringResource(R.string.support_request_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TarnishedGold
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.32f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.support_request_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory.copy(alpha = 0.86f)
                        )
                        OutlinedTextField(
                            value = uiState.subject,
                            onValueChange = viewModel::onSubjectChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.support_request_subject)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                            colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.message,
                            onValueChange = viewModel::onMessageChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.support_request_message)) },
                            minLines = 5,
                            colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.report_add_photos),
                                    tint = TarnishedGold
                                )
                            }
                            Text(
                                text = stringResource(R.string.report_photos_selected, uiState.imageUris.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = OldIvory.copy(alpha = 0.78f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.submit(onSuccess = onBack) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    color = OldIvory,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.support_request_send))
                            }
                        }
                    }
                }
            }
        }
    }
}
