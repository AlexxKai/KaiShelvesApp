package com.example.kaishelvesapp.ui.util

import java.text.SimpleDateFormat
import java.util.Locale

private const val StoredReadDatePattern = "yyyy-MM-dd"
private const val DisplayReadDatePattern = "dd/MM/yyyy"

fun formatReadDateForDisplay(readDate: String): String {
    if (readDate.isBlank()) return readDate

    return runCatching {
        val storedFormat = SimpleDateFormat(StoredReadDatePattern, Locale.US).apply {
            isLenient = false
        }
        val displayFormat = SimpleDateFormat(DisplayReadDatePattern, Locale.getDefault())
        displayFormat.format(storedFormat.parse(readDate) ?: return readDate)
    }.getOrDefault(readDate)
}
