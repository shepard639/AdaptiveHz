package com.antigravity.adaptivehz.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Manages in-app language switching and synchronization with Android system locales.
 * Supports Android 10-15 via AppCompatDelegate and native LocaleManager.
 */
object LanguageHelper {

    private const val TAG = "LanguageHelper"

    const val LANG_SYSTEM = "system"
    const val LANG_RU = "ru"
    const val LANG_EN = "en"

    /**
     * Gets the currently active language code ("ru", "en", or "system").
     */
    fun getCurrentLanguage(): String {
        return try {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            if (appLocales.isEmpty) {
                LANG_SYSTEM
            } else {
                val tag = appLocales[0]?.language?.lowercase() ?: LANG_SYSTEM
                if (tag.startsWith("ru")) LANG_RU else if (tag.startsWith("en")) LANG_EN else tag
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read application locales: ${e.message}")
            LANG_SYSTEM
        }
    }

    /**
     * Applies the desired language code ("ru", "en", or "system").
     */
    fun setLanguage(langCode: String) {
        try {
            Log.d(TAG, "Setting application language to: $langCode")
            when (langCode) {
                LANG_RU -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                LANG_EN -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                else -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to set application locale to $langCode", e)
        }
    }
}
