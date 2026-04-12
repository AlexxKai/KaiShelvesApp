package com.example.kaishelvesapp.data.localization

import android.icu.text.Transliterator
import com.example.kaishelvesapp.data.model.Libro
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object BookMetadataLocalizer {

    private val languageIdentifier by lazy {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build()
        )
    }

    private val translators = ConcurrentHashMap<String, Translator>()
    private val translatedTextCache = ConcurrentHashMap<String, String>()
    private val latinTransliterator by lazy {
        Transliterator.getInstance("Any-Latin; Latin-ASCII")
    }

    suspend fun localize(book: Libro, targetLanguageTag: String): Libro {
        val normalizedTarget = normalizeLanguageTag(targetLanguageTag)

        return book.copy(
            titulo = localizeTitle(book.titulo, normalizedTarget),
            autor = localizeAuthor(book.autor)
        )
    }

    private suspend fun localizeTitle(title: String, targetLanguageTag: String): String {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return normalizedTitle

        val sourceLanguageTag = detectLanguage(normalizedTitle)
        if (sourceLanguageTag == null || sourceLanguageTag == targetLanguageTag) {
            return buildFriendlyFallback(normalizedTitle)
        }

        val sourceLanguage = TranslateLanguage.fromLanguageTag(sourceLanguageTag)
            ?: return buildFriendlyFallback(normalizedTitle)

        val targetLanguage = TranslateLanguage.fromLanguageTag(targetLanguageTag)
            ?: return buildFriendlyFallback(normalizedTitle)

        if (sourceLanguage == targetLanguage) {
            return buildFriendlyFallback(normalizedTitle)
        }

        val cacheKey = "$sourceLanguage->$targetLanguage::$normalizedTitle"
        translatedTextCache[cacheKey]?.let { return it }

        val translated = runCatching {
            val translator = getTranslator(sourceLanguage, targetLanguage)
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            translator.translate(normalizedTitle).await().trim()
        }.getOrNull()

        val result = translated
            ?.takeIf { it.isNotBlank() }
            ?: buildFriendlyFallback(normalizedTitle)

        translatedTextCache[cacheKey] = result
        return result
    }

    private fun localizeAuthor(author: String): String {
        val normalizedAuthor = author.trim()
        if (normalizedAuthor.isBlank()) return normalizedAuthor

        return normalizedAuthor
            .split(",")
            .map { segment -> buildFriendlyFallback(segment.trim()) }
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    private suspend fun detectLanguage(text: String): String? {
        val languageTag = runCatching {
            languageIdentifier.identifyLanguage(text).await()
        }.getOrNull()

        if (languageTag.isNullOrBlank() || languageTag == "und") {
            return null
        }

        return normalizeLanguageTag(languageTag)
    }

    private fun getTranslator(sourceLanguage: String, targetLanguage: String): Translator {
        val key = "$sourceLanguage->$targetLanguage"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }
    }

    private fun transliterateIfNeeded(text: String): String {
        if (!containsNonLatinScript(text)) return text

        return latinTransliterator
            .transliterate(text)
            .replace("\\s+".toRegex(), " ")
            .trim()
            .ifBlank { text }
    }

    private fun buildFriendlyFallback(text: String): String {
        if (!containsNonLatinScript(text)) return text

        val transliterated = transliterateIfNeeded(text)
        if (transliterated.equals(text, ignoreCase = true)) return text

        return "$transliterated ($text)"
    }

    private fun containsNonLatinScript(text: String): Boolean {
        return text.any { char ->
            if (!char.isLetter()) {
                false
            } else {
                when (Character.UnicodeScript.of(char.code)) {
                    Character.UnicodeScript.LATIN,
                    Character.UnicodeScript.COMMON,
                    Character.UnicodeScript.INHERITED -> false
                    else -> true
                }
            }
        }
    }

    private fun normalizeLanguageTag(languageTag: String): String {
        return languageTag
            .trim()
            .ifBlank { Locale.getDefault().language }
            .substringBefore('-')
            .lowercase(Locale.ROOT)
    }
}
