package com.chefascend.mobile.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
  private const val PREFS_NAME = "chef_ascend_prefs"
  private const val KEY_LANGUAGE = "language"
  private const val LANGUAGE_ZH = "zh"
  private const val LANGUAGE_EN = "en"

  fun applySavedLocale(context: Context) {
    val storedLanguage = getSavedLanguage(context)
    applyLocale(storedLanguage)
  }

  fun getSavedLanguage(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val value = prefs.getString(KEY_LANGUAGE, LANGUAGE_ZH)
    return if (value == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_ZH
  }

  fun setLanguage(context: Context, language: String) {
    val normalized = if (language == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_ZH
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_LANGUAGE, normalized).apply()
    applyLocale(normalized)
  }

  private fun applyLocale(language: String) {
    val languageTag = if (language == LANGUAGE_EN) "en" else "zh-Hans"
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
  }
}
