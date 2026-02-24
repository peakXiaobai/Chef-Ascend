package com.chefascend.mobile

import android.app.Application
import android.content.Context
import com.chefascend.mobile.ui.settings.LocaleManager

class ChefAscendApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(LocaleManager.wrapContext(base))
  }
}
