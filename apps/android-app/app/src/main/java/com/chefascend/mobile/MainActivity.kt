package com.chefascend.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.chefascend.mobile.ui.theme.ChefAscendTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ChefAscendTheme {
        ChefAscendApp()
      }
    }
  }
}
