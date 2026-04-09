package com.example.kaishelvesapp.ui.language

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {

    fun setLanguage(activity: Activity, languageTag: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)
        activity.recreate()
    }

    fun getCurrentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = locales.toLanguageTags()

        return when {
            tag.startsWith("en", ignoreCase = true) -> "en"
            tag.startsWith("es", ignoreCase = true) -> "es"
            else -> "es"
        }
    }
}