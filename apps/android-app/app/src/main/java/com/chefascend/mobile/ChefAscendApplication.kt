package com.chefascend.mobile

import android.app.Application
import com.chefascend.mobile.ui.settings.LocaleManager

class ChefAscendApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    LocaleManager.applySavedLocale(this)
  }
}
