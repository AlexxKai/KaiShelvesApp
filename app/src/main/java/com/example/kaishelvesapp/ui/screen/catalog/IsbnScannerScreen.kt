package com.example.kaishelvesapp.ui.screen.catalog

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.BookShelfActions
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.ColdAsh
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun IsbnScannerScreen(
    viewModel: CatalogViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onBookClick: (Libro) -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannerLocked by remember { mutableStateOf(false) }
    var lastIsbn by remember { mutableStateOf<String?>(null) }

    fun resetCurrentScan() {
        viewModel.clearScannedBook()
        scannerLocked = false
        lastIsbn = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        viewModel.clearScannedBook()
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(
        uiState.scannedBook,
        uiState.isbnLookupError,
        uiState.isIsbnLookupLoading
    ) {
        when {
            uiState.scannedBook != null -> scannerLocked = true
            !uiState.isbnLookupError.isNullOrBlank() -> scannerLocked = true
            uiState.isIsbnLookupLoading -> scannerLocked = true
        }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            ScannerTopBar(
                title = stringResource(R.string.scan_books_title),
                onBack = onBack,
                onScanAgain = ::resetCurrentScan
            )
        },
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.DISCOVER,
                onSelect = onSectionSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .background(Obsidian)
        ) {
            ScannerTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (hasCameraPermission) {
                            BarcodeCameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                enabled = !scannerLocked,
                                onIsbnDetected = { isbn ->
                                    if (!scannerLocked && isbn != lastIsbn) {
                                        scannerLocked = true
                                        lastIsbn = isbn
                                        viewModel.buscarPorIsbn(isbn)
                                    }
                                }
                            )

                            ScannerAimOverlay(
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CameraPermissionMessage(
                                modifier = Modifier.align(Alignment.Center),
                                onRequestPermission = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    }

                    1 -> {
                        ScannerHistoryPlaceholder(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                ScannedBookBottomArea(
                    book = uiState.scannedBook,
                    scannedIsbn = uiState.scannedIsbn ?: lastIsbn,
                    errorMessage = uiState.isbnLookupError,
                    isLoading = uiState.isIsbnLookupLoading,
                    onDismiss = ::resetCurrentScan,
                    onScanAgain = ::resetCurrentScan,
                    onRetry = {
                        val isbnToRetry = uiState.scannedIsbn ?: lastIsbn
                        if (!isbnToRetry.isNullOrBlank()) {
                            scannerLocked = true
                            viewModel.buscarPorIsbn(isbnToRetry)
                        }
                    },
                    onOpenBook = {
                        uiState.scannedBook?.let(onBookClick)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ScannerTopBar(
    title: String,
    onBack: () -> Unit,
    onScanAgain: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepWalnut
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = OldIvory
                )
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = OldIvory,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = onScanAgain) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = stringResource(R.string.scan_another_book),
                    tint = TarnishedGold
                )
            }
        }
    }
}

@Composable
private fun ScannerTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        stringResource(R.string.scan_tab_scan),
        stringResource(R.string.scan_tab_history)
    )

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = DeepWalnut,
        contentColor = TarnishedGold,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = Color(0xFF00B8B8)
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = if (index == 1) "$title (0)" else title,
                        color = if (selectedTab == index) OldIvory else ColdAsh,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}

@Composable
private fun CameraPermissionMessage(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = modifier.padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut),
        border = BorderStroke(1.dp, TarnishedGold),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.scan_camera_permission_message),
                color = OldIvory,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.grant_permission),
                color = TarnishedGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRequestPermission)
            )
        }
    }
}

@Composable
private fun ScannerHistoryPlaceholder(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.scan_history_placeholder),
        modifier = modifier.padding(24.dp),
        color = OldIvory,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun ScannerAimOverlay(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f)
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Transparent)
        ) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF00B8B8),
                thickness = 2.dp
            )
        }
    }
}

@Composable
private fun ScannedBookBottomArea(
    book: Libro?,
    scannedIsbn: String?,
    errorMessage: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onScanAgain: () -> Unit,
    onRetry: () -> Unit,
    onOpenBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = book != null || isLoading || !errorMessage.isNullOrBlank(),
        modifier = modifier
    ) {
        when {
            book != null -> {
                ScannedBookCardWithShelfActions(
                    book = book,
                    onOpenBook = onOpenBook,
                    onDismiss = onDismiss,
                    onScanAgain = onScanAgain
                )
            }

            isLoading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.98f)),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = TarnishedGold
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.scan_book_searching),
                                color = OldIvory,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (!scannedIsbn.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.isbn_value, scannedIsbn),
                                    color = ColdAsh,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.email_verification_close),
                                tint = TarnishedGold
                            )
                        }
                    }
                }
            }

            !errorMessage.isNullOrBlank() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.98f)),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = errorMessage,
                                    color = OldIvory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (!scannedIsbn.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(R.string.isbn_value, scannedIsbn),
                                        color = ColdAsh,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.email_verification_close),
                                    tint = TarnishedGold
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.retry_lookup),
                            color = TarnishedGold,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onRetry)
                        )

                        OutlinedButton(
                            onClick = onScanAgain,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.scan_another_book))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedBookCardWithShelfActions(
    book: Libro,
    onOpenBook: () -> Unit,
    onDismiss: () -> Unit,
    onScanAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBook)
                        .padding(end = 38.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BookCover(
                        imageUrl = book.imagen,
                        title = book.titulo,
                        modifier = Modifier.size(width = 76.dp, height = 112.dp),
                        showFrame = false
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.titulo.ifBlank { book.isbn },
                            style = MaterialTheme.typography.titleMedium,
                            color = OldIvory,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (book.autor.isNotBlank()) {
                            Text(
                                text = book.autor,
                                modifier = Modifier.padding(top = 2.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TarnishedGold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.email_verification_close),
                        tint = TarnishedGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BookShelfActions(
                book = book,
                viewModelKeyPrefix = "scanner_shelf"
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.scan_another_book))
            }
        }
    }
}

@Composable
private fun BarcodeCameraPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onIsbnDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnIsbnDetected by rememberUpdatedState(onIsbnDetected)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { cameraPreview ->
                    cameraPreview.setSurfaceProvider(previewView.surfaceProvider)
                }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        IsbnBarcodeAnalyzer(
                            scanner = scanner,
                            enabledProvider = { currentEnabled },
                            onIsbnDetected = { currentOnIsbnDetected(it) }
                        )
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analyzer
                )
            } catch (_: Exception) {
                cameraProvider.unbindAll()
            }
        }

        cameraProviderFuture.addListener(
            listener,
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private class IsbnBarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val enabledProvider: () -> Boolean,
    private val onIsbnDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var isProcessing = false
    private var lockedIsbn: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!enabledProvider()) {
            lockedIsbn = null
            imageProxy.close()
            return
        }

        if (isProcessing || lockedIsbn != null) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val isbn = barcodes
                    .asSequence()
                    .mapNotNull { it.rawValue }
                    .mapNotNull { normalizeIsbnFromBarcode(it) }
                    .firstOrNull()

                if (!isbn.isNullOrBlank()) {
                    lockedIsbn = isbn
                    onIsbnDetected(isbn)
                }
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }
}

private fun normalizeIsbnFromBarcode(rawValue: String): String? {
    val value = rawValue
        .trim()
        .uppercase()
        .removePrefix("ISBN")
        .replace(":", "")
        .replace("-", "")
        .replace(" ", "")

    return when {
        value.length == 13 &&
            (value.startsWith("978") || value.startsWith("979")) &&
            isValidIsbn13(value) -> value

        value.length == 10 && isValidIsbn10(value) -> value

        else -> null
    }
}

private fun isValidIsbn13(isbn: String): Boolean {
    if (isbn.length != 13 || isbn.any { !it.isDigit() }) return false

    val sum = isbn.take(12).mapIndexed { index, char ->
        val digit = char.digitToInt()
        if (index % 2 == 0) digit else digit * 3
    }.sum()

    val checkDigit = (10 - (sum % 10)) % 10
    return checkDigit == isbn.last().digitToInt()
}

private fun isValidIsbn10(isbn: String): Boolean {
    if (isbn.length != 10) return false

    var sum = 0

    isbn.forEachIndexed { index, char ->
        val value = when {
            char.isDigit() -> char.digitToInt()
            index == 9 && char == 'X' -> 10
            else -> return false
        }

        sum += value * (10 - index)
    }

    return sum % 11 == 0
}
