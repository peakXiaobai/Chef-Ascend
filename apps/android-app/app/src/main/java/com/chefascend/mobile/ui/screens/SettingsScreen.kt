@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.R
import com.chefascend.mobile.ui.settings.LocaleManager

@Composable
fun SettingsScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  var currentLanguage by remember { mutableStateOf(LocaleManager.getSavedLanguage(context)) }

  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.Top
    ) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(R.string.settings_language_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = if (currentLanguage == "en") {
          stringResource(R.string.settings_language_current_en)
        } else {
          stringResource(R.string.settings_language_current_zh)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          LocaleManager.setLanguage(context, "zh")
          currentLanguage = "zh"
          (context as? Activity)?.recreate()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = currentLanguage != "zh"
      ) {
        Text(stringResource(R.string.settings_language_zh))
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(
        onClick = {
          LocaleManager.setLanguage(context, "en")
          currentLanguage = "en"
          (context as? Activity)?.recreate()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = currentLanguage != "en"
      ) {
        Text(stringResource(R.string.settings_language_en))
      }

      Spacer(modifier = Modifier.height(24.dp))
      OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_back))
      }
    }
  }
}
