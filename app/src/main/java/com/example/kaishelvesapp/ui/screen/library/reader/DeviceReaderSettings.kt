package com.example.kaishelvesapp.ui.screen.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kaishelvesapp.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ReaderColorTheme(
    val background: String,
    val text: String
)

enum class ReaderColorThemePreset(
    val background: String,
    val text: String,
    val titleRes: Int
) {
    Day("#F6F1E5", "#1E1A12", R.string.reader_theme_day),
    Night("#2E3235", "#D8D3C0", R.string.reader_theme_night),
    Amoled("#000000", "#E7E2CF", R.string.reader_theme_amoled),
    Sepia("#E7D7AF", "#21190E", R.string.reader_theme_sepia)
}

@Composable
fun ReaderThemePanel(
    selectedTheme: ReaderColorTheme,
    onThemeSelected: (ReaderColorTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.reader_theme_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderColorThemePreset.entries.forEach { preset ->
                val isSelected = selectedTheme.background == preset.background && selectedTheme.text == preset.text
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(preset.background.toComposeColor())
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF8FB6FF) else Color.White.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onThemeSelected(ReaderColorTheme(preset.background, preset.text)) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(preset.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = preset.text.toComposeColor(),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = true,
                onCheckedChange = null,
                modifier = Modifier.size(32.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4097E8),
                    uncheckedColor = Color.White
                )
            )
            Text(
                text = stringResource(R.string.reader_theme_apply_colors_only),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

fun readReaderColorTheme(context: Context): ReaderColorTheme {
    val prefs = readerDisplaySettingsPreferences(context)
    return ReaderColorTheme(
        background = prefs.getString("reader_theme_background", ReaderColorThemePreset.Night.background)
            ?: ReaderColorThemePreset.Night.background,
        text = prefs.getString("reader_theme_text", ReaderColorThemePreset.Night.text)
            ?: ReaderColorThemePreset.Night.text
    )
}

fun saveReaderColorTheme(context: Context, theme: ReaderColorTheme) {
    readerDisplaySettingsPreferences(context)
        .edit()
        .putString("reader_theme_background", theme.background)
        .putString("reader_theme_text", theme.text)
        .apply()
}

private fun String.toComposeColor(): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(Color.White)
}

@Composable
fun ReaderTextSizePanel(
    zoomPercent: Int,
    isReflowMode: Boolean,
    canUseReflowMode: Boolean,
    showReset: Boolean,
    onZoomPercentChange: (Int) -> Unit,
    onResetZoomPercent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showReset) {
                IconButton(
                    onClick = onResetZoomPercent,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = stringResource(R.string.reader_restore_text_size),
                        tint = Color.White
                    )
                }
            }
            Text(
                text = "$zoomPercent%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(46.dp)
            )
            Slider(
                value = zoomPercent.toFloat(),
                onValueChange = { onZoomPercentChange(it.roundToInt()) },
                valueRange = READER_PDF_ZOOM_MIN.toFloat()..READER_PDF_ZOOM_MAX.toFloat(),
                modifier = Modifier.weight(1f)
            )
            ReaderStepButton(
                text = "-",
                onClick = { onZoomPercentChange(zoomPercent - 1) }
            )
            ReaderStepButton(
                text = "+",
                onClick = { onZoomPercentChange(zoomPercent + 1) }
            )
            TextButton(
                onClick = { onZoomPercentChange(READER_PDF_ZOOM_DEFAULT) },
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.reader_zoom_default_110),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun PdfDisplaySettingsPanel(
    brightnessPercent: Int,
    autoBrightness: Boolean,
    blueLightFilterEnabled: Boolean,
    blueLightOpacity: Int,
    onBrightnessChange: (Int) -> Unit,
    onAutoBrightnessChange: (Boolean) -> Unit,
    onBlueLightFilterChange: (Boolean) -> Unit,
    onBlueLightOpacityChange: (Int) -> Unit,
    onOpenBrightnessAdvancedSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = autoBrightness,
                    onCheckedChange = onAutoBrightnessChange,
                    modifier = Modifier.size(32.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFB9C7FF),
                        uncheckedColor = Color.White
                    )
                )
                Text(
                    text = stringResource(R.string.reader_brightness_auto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = blueLightFilterEnabled,
                    onCheckedChange = onBlueLightFilterChange,
                    modifier = Modifier.size(32.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFB9C7FF),
                        uncheckedColor = Color.White
                    )
                )
                Text(
                    text = stringResource(R.string.reader_blue_light_filter_short),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
        ReaderPercentControl(
            value = brightnessPercent,
            onValueChange = onBrightnessChange,
            showSettings = true,
            onSettingsClick = onOpenBrightnessAdvancedSettings,
            leadingContent = {
                Text(
                    text = stringResource(R.string.reader_brightness),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.width(58.dp)
                )
            }
        )
        ReaderPercentControl(
            value = blueLightOpacity,
            onValueChange = onBlueLightOpacityChange,
            showSettings = false,
            onSettingsClick = {},
            leadingContent = {
                Text(
                    text = stringResource(R.string.reader_blue_light_opacity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.width(58.dp)
                )
            }
        )
    }
}

@Composable
fun ReaderPercentControl(
    value: Int,
    onValueChange: (Int) -> Unit,
    showSettings: Boolean,
    onSettingsClick: () -> Unit,
    leadingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        leadingContent?.invoke()
        ReaderPercentSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
        ReaderStepButton(text = "-", onClick = { onValueChange(value - 1) })
        ReaderStepButton(text = "+", onClick = { onValueChange(value + 1) })
        if (showSettings) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.reader_brightness_settings),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ReaderPercentSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.height(34.dp)
    ) {
        val safeValue = value.coerceIn(0, 100)
        Slider(
            value = safeValue.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun ReaderStepButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(width = 28.dp, height = 30.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReaderBrightnessFeedback(
    percent: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF5C7FB9).copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ReaderBrightnessEdgeGesture(
    selectedEdge: String,
    onBrightnessDelta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val edgeModifier = Modifier
            .then(
                when (selectedEdge) {
                    "Borde derecho" -> Modifier
                        .align(Alignment.CenterEnd)
                        .width(28.dp)
                        .fillMaxHeight()
                    "Borde superior" -> Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                    "Borde inferior" -> Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                    else -> Modifier
                        .align(Alignment.CenterStart)
                        .width(28.dp)
                        .fillMaxHeight()
                }
            )
            .pointerInput(selectedEdge) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val rawDelta = when (selectedEdge) {
                        "Borde superior", "Borde inferior" -> dragAmount.x / 8f
                        else -> -dragAmount.y / 8f
                    }
                    val delta = rawDelta.roundToInt().let {
                        when {
                            it != 0 -> it
                            rawDelta > 0f -> 1
                            rawDelta < 0f -> -1
                            else -> 0
                        }
                    }
                    if (delta != 0) onBrightnessDelta(delta)
                }
            }
        Box(modifier = edgeModifier)
    }
}

@Composable
fun BrightnessAdvancedSettingsDialog(
    selectedEdge: String,
    resumeAutoBrightnessAfterInactivity: Boolean,
    resumeAutoBrightnessMinutes: String,
    onSelectedEdgeChange: (String) -> Unit,
    onResumeAutoBrightnessAfterInactivityChange: (Boolean) -> Unit,
    onOpenResumeMinutesSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var edgeMenuExpanded by remember { mutableStateOf(false) }
    val edgeOptions = listOf(
        "Borde izquierdo",
        "Borde derecho",
        "Borde superior",
        "Borde inferior"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202020))
                    .clickable(onClick = {})
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.reader_brightness_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = stringResource(R.string.reader_adjust_brightness_to),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { edgeMenuExpanded = true }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = readerBrightnessEdgeLabel(selectedEdge),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.62f)
                            )
                        }
                        DropdownMenu(
                            expanded = edgeMenuExpanded,
                            onDismissRequest = { edgeMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF252525))
                        ) {
                            edgeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = readerBrightnessEdgeLabel(option),
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        onSelectedEdgeChange(option)
                                        edgeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = resumeAutoBrightnessAfterInactivity,
                        onCheckedChange = onResumeAutoBrightnessAfterInactivityChange,
                        modifier = Modifier.size(32.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFB9C7FF),
                            uncheckedColor = Color.White
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.reader_resume_auto_brightness_after_inactivity,
                            formatReaderDuration(
                                minutesText = resumeAutoBrightnessMinutes,
                                minuteSingular = stringResource(R.string.reader_minute),
                                minutePlural = stringResource(R.string.reader_minutes),
                                hourSingular = stringResource(R.string.reader_hour),
                                hourPlural = stringResource(R.string.reader_hours),
                                conjunction = stringResource(R.string.reader_duration_conjunction)
                            )
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenResumeMinutesSettings,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.reader_adjust_minutes),
                            tint = Color(0xFFEBC7E8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.ok).uppercase(), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AutoBrightnessMinutesDialog(
    minutes: String,
    onMinutesChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draftTotalMinutes by remember(minutes) {
        mutableStateOf(minutes.toIntOrNull()?.coerceIn(0, 23 * 60 + 59) ?: 60)
    }
    val selectedHour = draftTotalMinutes / 60
    val selectedMinute = draftTotalMinutes % 60

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202020))
                    .clickable(onClick = {})
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimePickerColumn(
                        label = stringResource(R.string.reader_hours),
                        values = 0..23,
                        selectedValue = selectedHour,
                        onValueSelected = { hour ->
                            draftTotalMinutes = ((hour * 60) + selectedMinute).coerceIn(0, 23 * 60 + 59)
                        }
                    )
                    TimePickerSeparator()
                    TimePickerColumn(
                        label = stringResource(R.string.reader_minutes),
                        values = 0..59,
                        selectedValue = selectedMinute,
                        onValueSelected = { minute ->
                            draftTotalMinutes = ((selectedHour * 60) + minute).coerceIn(0, 23 * 60 + 59)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(), color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            onMinutesChange(draftTotalMinutes.toString())
                            onDismiss()
                        }
                    ) {
                        Text(text = stringResource(R.string.ok).uppercase(), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerSeparator() {
    Column(
        modifier = Modifier.width(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.height(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TimePickerColumn(
    label: String,
    values: IntRange,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit
) {
    val valuesList = remember(values) { values.toList() }
    val selectedIndex = valuesList.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val layoutInfo = listState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
            kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
        }
        centeredItem?.let { item ->
            valuesList.getOrNull(item.index)?.let { centeredValue ->
                if (centeredValue != selectedValue) {
                    onValueSelected(centeredValue)
                }
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
            } ?: return@LaunchedEffect
            valuesList.getOrNull(centeredItem.index)?.let { centeredValue ->
                if (centeredValue != selectedValue) {
                    onValueSelected(centeredValue)
                }
                listState.animateScrollToItem(centeredItem.index)
            }
        }
    }

    Column(
        modifier = Modifier.width(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(144.dp)
                .width(92.dp),
            contentPadding = PaddingValues(vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(valuesList) { value ->
                val selected = value == selectedValue
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.32f else 0.9f,
                    animationSpec = tween(durationMillis = 180),
                    label = "timePickerScale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.32f,
                    animationSpec = tween(durationMillis = 180),
                    label = "timePickerAlpha"
                )
                Text(
                    text = value.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = alpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable {
                            onValueSelected(value)
                            scope.launch {
                                listState.animateScrollToItem(valuesList.indexOf(value).coerceAtLeast(0))
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun ApplyReaderBrightness(
    brightnessPercent: Int,
    autoBrightness: Boolean
) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }
    val originalBrightness = remember(window) { window?.attributes?.screenBrightness }
    DisposableEffect(view, brightnessPercent, autoBrightness) {
        if (window != null) {
            val attributes = window.attributes
            attributes.screenBrightness = if (autoBrightness) {
                android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                (brightnessPercent.coerceIn(1, 100) / 100f)
            }
            window.attributes = attributes
        }
        onDispose {
            if (window != null && originalBrightness != null) {
                val attributes = window.attributes
                attributes.screenBrightness = originalBrightness
                window.attributes = attributes
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun readerBrightnessEdgeLabel(edge: String): String {
    return when (edge) {
        "Borde derecho" -> stringResource(R.string.reader_brightness_edge_right)
        "Borde superior" -> stringResource(R.string.reader_brightness_edge_top)
        "Borde inferior" -> stringResource(R.string.reader_brightness_edge_bottom)
        else -> stringResource(R.string.reader_brightness_edge_left)
    }
}

fun formatReaderDuration(
    minutesText: String,
    minuteSingular: String,
    minutePlural: String,
    hourSingular: String,
    hourPlural: String,
    conjunction: String
): String {
    val totalMinutes = minutesText.toIntOrNull()?.coerceAtLeast(0) ?: 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours <= 0 -> "$minutes ${if (minutes == 1) minuteSingular else minutePlural}"
        minutes == 0 -> "$hours ${if (hours == 1) hourSingular else hourPlural}"
        else -> "$hours ${if (hours == 1) hourSingular else hourPlural} $conjunction $minutes ${if (minutes == 1) minuteSingular else minutePlural}"
    }
}

fun readerDisplaySettingsPreferences(context: Context) =
    context.getSharedPreferences("device_reader_display_settings", Context.MODE_PRIVATE)

const val READER_PDF_ZOOM_MIN = 50
const val READER_PDF_ZOOM_MAX = 150
const val READER_PDF_ZOOM_DEFAULT = 110
const val READER_ANNOTATION_TEXT_LIMIT = 480

fun readReaderBrightnessPercent(context: Context): Int {
    return readerDisplaySettingsPreferences(context)
        .getInt("brightness_percent", 50)
        .coerceIn(0, 100)
}

fun readReaderAutoBrightness(context: Context): Boolean {
    return readerDisplaySettingsPreferences(context)
        .getBoolean("auto_brightness", true)
}

fun readReaderBlueLightFilterEnabled(context: Context): Boolean {
    return readerDisplaySettingsPreferences(context)
        .getBoolean("blue_light_filter_enabled", false)
}

fun readReaderBlueLightOpacity(context: Context): Int {
    return readerDisplaySettingsPreferences(context)
        .getInt("blue_light_opacity", 35)
        .coerceIn(0, 100)
}

fun readReaderPdfZoomPercent(context: Context): Int {
    return readerDisplaySettingsPreferences(context)
        .getInt("pdf_zoom_percent", READER_PDF_ZOOM_DEFAULT)
        .coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX)
}

fun saveReaderTextSizePercent(context: Context, textSizePercent: Int) {
    readerDisplaySettingsPreferences(context)
        .edit()
        .putInt("pdf_zoom_percent", textSizePercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX))
        .apply()
}

fun readerPdfZoomPercentToScale(zoomPercent: Int): Float {
    return zoomPercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX) / 100f
}

data class ReaderPaginationResult(
    val pages: List<String>,
    val sourceStartPages: List<Int>,
    val pageTextRanges: List<ReaderPageTextRange> = emptyList(),
    val pageHeaders: List<String?> = emptyList(),
    val pageKinds: List<ReaderSourcePageKind> = emptyList()
)

data class ReaderPageTextRange(
    val sourcePage: Int,
    val start: Int,
    val end: Int
)

fun paginateReflowTextWithStarts(
    sourcePages: List<String>,
    sourcePageTitles: List<String?> = emptyList(),
    sourcePageKinds: List<ReaderSourcePageKind> = emptyList(),
    fontSizePx: Float,
    lineHeightPx: Float,
    maxWidthPx: Int,
    maxHeightPx: Int
): ReaderPaginationResult {
    if (maxWidthPx <= 0 || maxHeightPx <= 0) {
        return ReaderPaginationResult(pages = listOf(""), sourceStartPages = listOf(0))
    }
    val normalizedSources = sourcePages.map { source ->
        source
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
    if (normalizedSources.all { it.isBlank() }) {
        return ReaderPaginationResult(pages = listOf(""), sourceStartPages = listOf(0))
    }

    val paint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSizePx
        color = android.graphics.Color.rgb(26, 18, 11)
    }
    val blocks = mutableListOf<String>()
    val pageTextRanges = mutableListOf<ReaderPageTextRange>()
    val pageHeaders = mutableListOf<String?>()
    val pageKinds = mutableListOf<ReaderSourcePageKind>()
    val startPages = mutableListOf<Int>()

    normalizedSources.forEachIndexed { sourceIndex, sourceText ->
        startPages += blocks.size.coerceAtLeast(0)
        if (sourceText.isBlank()) return@forEachIndexed
        val sourceHeader = sourcePageTitles.getOrNull(sourceIndex)?.takeIf { it.isNotBlank() }
        val sourceKind = sourcePageKinds.getOrNull(sourceIndex) ?: ReaderSourcePageKind.Body
        val firstPageReservedHeight = if (sourceHeader != null) {
            (lineHeightPx * if (sourceKind == ReaderSourcePageKind.Cover) 9f else 3.2f).toInt()
        } else {
            0
        }
        val layout = StaticLayout.Builder
            .obtain(sourceText, 0, sourceText.length, paint, maxWidthPx)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(lineHeightPx - fontSizePx, 1f)
            .build()
        var startLine = 0

        // Se corta por líneas ya maquetadas fuera de Compose para que el cambio de página sea estable.
        while (startLine < layout.lineCount) {
            val startTop = layout.getLineTop(startLine)
            val availableHeight = if (startLine == 0) {
                (maxHeightPx - firstPageReservedHeight).coerceAtLeast(lineHeightPx.toInt())
            } else {
                maxHeightPx
            }
            var endLine = startLine
            while (endLine < layout.lineCount) {
                val blockHeight = layout.getLineBottom(endLine) - startTop
                if (blockHeight > availableHeight && endLine > startLine) break
                if (blockHeight > availableHeight) break
                endLine += 1
            }
            val safeEndLine = endLine.coerceAtLeast(startLine + 1).coerceAtMost(layout.lineCount)
            val startOffset = layout.getLineStart(startLine)
            val endOffset = layout.getLineEnd(safeEndLine - 1)
            val rawBlock = sourceText.substring(startOffset, endOffset)
            val trimmedBlock = rawBlock.trim()
            val firstContentOffset = rawBlock.indexOfFirst { !it.isWhitespace() }
            val lastContentOffset = rawBlock.indexOfLast { !it.isWhitespace() }
            if (trimmedBlock.isNotBlank() && firstContentOffset >= 0 && lastContentOffset >= firstContentOffset) {
                // La página visible mantiene el mismo texto que antes; los offsets solo se usan para anotaciones.
                blocks += trimmedBlock
                pageTextRanges += ReaderPageTextRange(
                    sourcePage = sourceIndex,
                    start = startOffset + firstContentOffset,
                    end = startOffset + lastContentOffset + 1
                )
                pageHeaders += if (startLine == 0) sourceHeader else null
                pageKinds += if (startLine == 0) sourceKind else ReaderSourcePageKind.Body
            }
            startLine = safeEndLine
        }
    }
    val pages = blocks.ifEmpty { listOf("") }
    return ReaderPaginationResult(
        pages = pages,
        sourceStartPages = startPages.map { it.coerceIn(0, pages.lastIndex.coerceAtLeast(0)) }.ifEmpty { listOf(0) },
        pageTextRanges = pageTextRanges.takeIf { it.size == pages.size }.orEmpty(),
        pageHeaders = pageHeaders.takeIf { it.size == pages.size }.orEmpty(),
        pageKinds = pageKinds.takeIf { it.size == pages.size }.orEmpty()
    )
}

fun readReaderBrightnessEdge(context: Context): String {
    return readerDisplaySettingsPreferences(context)
        .getString("brightness_edge", "Borde izquierdo")
        .orEmpty()
        .takeIf {
            it in setOf("Borde izquierdo", "Borde derecho", "Borde superior", "Borde inferior")
        }
        ?: "Borde izquierdo"
}

fun readReaderResumeAutoBrightness(context: Context): Boolean {
    return readerDisplaySettingsPreferences(context)
        .getBoolean("resume_auto_brightness", false)
}

fun readReaderResumeAutoBrightnessMinutes(context: Context): String {
    return readerDisplaySettingsPreferences(context)
        .getString("resume_auto_brightness_minutes", "60")
        .orEmpty()
        .filter(Char::isDigit)
        .takeIf { it.isNotBlank() }
        ?: "60"
}

fun saveReaderDisplaySettings(
    context: Context,
    brightnessPercent: Int,
    autoBrightness: Boolean,
    blueLightFilterEnabled: Boolean,
    blueLightOpacity: Int,
    pdfZoomPercent: Int,
    brightnessEdge: String,
    resumeAutoBrightness: Boolean,
    resumeAutoBrightnessMinutes: String
) {
    readerDisplaySettingsPreferences(context)
        .edit()
        .putInt("brightness_percent", brightnessPercent.coerceIn(0, 100))
        .putBoolean("auto_brightness", autoBrightness)
        .putBoolean("blue_light_filter_enabled", blueLightFilterEnabled)
        .putInt("blue_light_opacity", blueLightOpacity.coerceIn(0, 100))
        .putInt("pdf_zoom_percent", pdfZoomPercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX))
        .putString("brightness_edge", brightnessEdge)
        .putBoolean("resume_auto_brightness", resumeAutoBrightness)
        .putString(
            "resume_auto_brightness_minutes",
            resumeAutoBrightnessMinutes.filter(Char::isDigit).takeIf { it.isNotBlank() } ?: "60"
        )
        .apply()
}




