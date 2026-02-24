package com.chefascend.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.chefascend.mobile.ui.settings.LocaleManager
import com.chefascend.mobile.ui.theme.ChefAscendTheme

class MainActivity : ComponentActivity() {
  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(LocaleManager.wrapContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ChefAscendTheme {
        ChefAscendApp()
      }
    }
  }
}
