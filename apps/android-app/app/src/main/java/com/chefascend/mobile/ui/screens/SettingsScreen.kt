@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.BuildConfig
import com.chefascend.mobile.R
import com.chefascend.mobile.data.model.AndroidReleaseInfo
import com.chefascend.mobile.data.repository.ChefRepository
import com.chefascend.mobile.ui.settings.LocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
  repository: ChefRepository,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var currentLanguage by remember { mutableStateOf(LocaleManager.getSavedLanguage(context)) }
  var updateChecking by remember { mutableStateOf(false) }
  var updateInfo by remember { mutableStateOf<AndroidReleaseInfo?>(null) }
  var updateAvailable by remember { mutableStateOf(false) }
  var updateMessage by remember { mutableStateOf<String?>(null) }

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
      Text(
        text = stringResource(R.string.settings_update_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(
          R.string.settings_update_current_version,
          BuildConfig.VERSION_NAME,
          BuildConfig.VERSION_CODE
        ),
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(modifier = Modifier.height(10.dp))
      Button(
        onClick = {
          updateChecking = true
          scope.launch {
            runCatching {
              withContext(Dispatchers.IO) { repository.getLatestAndroidRelease() }
            }.onSuccess { latest ->
              updateInfo = latest
              updateAvailable = latest.version_code > BuildConfig.VERSION_CODE
              updateMessage = if (updateAvailable) {
                context.getString(
                  R.string.settings_update_found,
                  latest.version_name,
                  latest.version_code
                )
              } else {
                context.getString(R.string.settings_update_latest)
              }
            }.onFailure { error ->
              updateMessage = error.message ?: context.getString(R.string.settings_update_check_failed)
            }
            updateChecking = false
          }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !updateChecking
      ) {
        Text(stringResource(R.string.settings_update_check))
      }

      if (updateChecking) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.settings_update_check),
            style = MaterialTheme.typography.bodySmall
          )
        }
      }

      if (updateInfo != null) {
        val latest = updateInfo ?: return@Column
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(
            R.string.settings_update_server_version,
            latest.version_name,
            latest.version_code
          ),
          style = MaterialTheme.typography.bodyMedium
        )
        Text(
          text = stringResource(
            R.string.settings_update_file_info,
            formatFileSize(latest.file_size_bytes),
            latest.updated_at
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (latest.release_notes != null && latest.release_notes.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.settings_update_notes, latest.release_notes),
            style = MaterialTheme.typography.bodySmall
          )
        }
      }

      if (updateAvailable && updateInfo != null) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = {
            val latest = updateInfo ?: return@OutlinedButton
            enqueueApkDownload(context, latest)
            updateMessage = context.getString(R.string.settings_update_downloading)
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(stringResource(R.string.settings_update_download))
        }
      }

      if (updateMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = updateMessage ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
      OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_back))
      }
    }
  }
}

private fun enqueueApkDownload(context: Context, latest: AndroidReleaseInfo) {
  runCatching {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(latest.download_url))
      .setTitle("Chef Ascend ${latest.version_name}")
      .setDescription("Chef Ascend APK")
      .setMimeType("application/vnd.android.package-archive")
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, latest.file_name)
    manager.enqueue(request)
  }.onFailure {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(latest.download_url))
    if (context !is Activity) {
      browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(browserIntent)
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) {
    return "0 B"
  }

  val kilo = 1024.0
  val mega = kilo * 1024
  val giga = mega * 1024

  return when {
    bytes >= giga -> String.format("%.2f GB", bytes / giga)
    bytes >= mega -> String.format("%.2f MB", bytes / mega)
    bytes >= kilo -> String.format("%.2f KB", bytes / kilo)
    else -> "$bytes B"
  }
}
