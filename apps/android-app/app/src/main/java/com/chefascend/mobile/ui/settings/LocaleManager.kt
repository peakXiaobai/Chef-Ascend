package com.chefascend.mobile.ui.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {
  private const val PREFS_NAME = "chef_ascend_prefs"
  private const val KEY_LANGUAGE = "language"
  private const val LANGUAGE_ZH = "zh"
  private const val LANGUAGE_EN = "en"

  fun getSavedLanguage(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val value = prefs.getString(KEY_LANGUAGE, LANGUAGE_ZH)
    return if (value == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_ZH
  }

  fun setLanguage(context: Context, language: String) {
    val normalized = if (language == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_ZH
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_LANGUAGE, normalized).apply()
  }

  fun wrapContext(context: Context): Context {
    val language = getSavedLanguage(context)
    val locale = if (language == LANGUAGE_EN) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
    Locale.setDefault(locale)
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return context.createConfigurationContext(configuration)
  }
}
