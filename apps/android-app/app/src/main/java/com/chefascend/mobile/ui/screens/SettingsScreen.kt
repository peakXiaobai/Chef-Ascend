@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.BuildConfig
import com.chefascend.mobile.R
import com.chefascend.mobile.data.model.AndroidReleaseInfo
import com.chefascend.mobile.data.repository.ChefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
  repository: ChefRepository,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()

  var updateChecking by remember { mutableStateOf(false) }
  var updateInfo by remember { mutableStateOf<AndroidReleaseInfo?>(null) }
  var updateAvailable by remember { mutableStateOf(false) }
  var updateMessage by remember { mutableStateOf<String?>(null) }
  var activeDownloadId by remember { mutableLongStateOf(-1L) }
  var downloadProgress by remember { mutableFloatStateOf(0f) }
  var downloadBytesDone by remember { mutableLongStateOf(0L) }
  var downloadBytesTotal by remember { mutableLongStateOf(0L) }
  var downloadSpeedBytesPerSecond by remember { mutableLongStateOf(0L) }

  val isDownloading = activeDownloadId > 0L

  DisposableEffect(appContext, activeDownloadId) {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
          return
        }

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId != activeDownloadId || downloadId <= 0L) {
          return
        }

        scope.launch {
          val result = withContext(Dispatchers.IO) { resolveDownloadResult(appContext, downloadId) }
          activeDownloadId = -1L
          downloadProgress = 0f
          downloadBytesDone = 0L
          downloadBytesTotal = 0L
          downloadSpeedBytesPerSecond = 0L
          if (result.status == DownloadStatus.Success && result.uri != null) {
            val launched = launchInstallIntent(appContext, result.uri)
            updateMessage = if (launched) {
              context.getString(R.string.settings_update_install_prompt)
            } else {
              context.getString(R.string.settings_update_install_failed)
            }
          } else {
            updateMessage = context.getString(R.string.settings_update_download_failed)
          }
        }
      }
    }

    val receiverRegistered = runCatching {
      ContextCompat.registerReceiver(
        appContext,
        receiver,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        ContextCompat.RECEIVER_NOT_EXPORTED
      )
    }.isSuccess

    onDispose {
      if (receiverRegistered) {
        runCatching { appContext.unregisterReceiver(receiver) }
      }
    }
  }

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
        enabled = !updateChecking && !isDownloading
      ) {
        Text(stringResource(R.string.settings_update_check))
      }

      if (updateChecking) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.settings_update_checking),
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
            val downloadId = startApkDownload(appContext, latest)
            if (downloadId <= 0L) {
              updateMessage = context.getString(R.string.settings_update_download_failed)
              return@OutlinedButton
            }

            activeDownloadId = downloadId
            downloadProgress = 0f
            downloadBytesDone = 0L
            downloadBytesTotal = 0L
            downloadSpeedBytesPerSecond = 0L
            updateMessage = context.getString(R.string.settings_update_downloading)

            scope.launch {
              trackDownloadProgress(
                context = appContext,
                downloadId = downloadId,
                onProgress = { track ->
                  downloadProgress = track.progress
                  downloadBytesDone = track.downloadedBytes
                  downloadBytesTotal = track.totalBytes
                  downloadSpeedBytesPerSecond = track.speedBytesPerSecond
                }
              )
            }
          },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isDownloading
        ) {
          Text(stringResource(R.string.settings_update_download))
        }
      }

      if (isDownloading) {
        Spacer(modifier = Modifier.height(10.dp))
        if (downloadBytesTotal > 0L) {
          LinearProgressIndicator(
            progress = { downloadProgress },
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (downloadBytesTotal > 0L) {
            stringResource(
              R.string.settings_update_progress_detail,
              (downloadProgress * 100).toInt().coerceIn(0, 100),
              formatFileSize(downloadBytesDone),
              formatFileSize(downloadBytesTotal)
            )
          } else {
            stringResource(
              R.string.settings_update_progress_unknown_total,
              formatFileSize(downloadBytesDone)
            )
          },
          style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(
            R.string.settings_update_speed,
            formatSpeed(downloadSpeedBytesPerSecond)
          ),
          style = MaterialTheme.typography.bodySmall
        )
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

private fun startApkDownload(context: Context, latest: AndroidReleaseInfo): Long {
  return runCatching {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(latest.download_url))
      .setTitle("Chef Ascend ${latest.version_name}")
      .setDescription("Chef Ascend APK")
      .setMimeType("application/vnd.android.package-archive")
      .setAllowedOverMetered(true)
      .setAllowedOverRoaming(true)
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
      .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, latest.file_name)
    manager.enqueue(request)
  }.getOrElse {
    -1L
  }
}

private suspend fun trackDownloadProgress(
  context: Context,
  downloadId: Long,
  onProgress: (DownloadTrack) -> Unit
) {
  val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
  var lastBytes = -1L
  var lastTimestampMs = 0L

  while (true) {
    val nowMs = System.currentTimeMillis()
    val query = DownloadManager.Query().setFilterById(downloadId)
    val result = manager.query(query).use { cursor ->
      if (!cursor.moveToFirst()) {
        DownloadTrack(
          status = DownloadManager.STATUS_FAILED,
          progress = 0f,
          downloadedBytes = 0L,
          totalBytes = 0L,
          speedBytesPerSecond = 0L
        )
      } else {
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        val progress = if (total > 0) {
          (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
          0f
        }
        DownloadTrack(
          status = status,
          progress = progress,
          downloadedBytes = downloaded.coerceAtLeast(0L),
          totalBytes = total.coerceAtLeast(0L),
          speedBytesPerSecond = 0L
        )
      }
    }

    val speedBytesPerSecond = if (
      result.status == DownloadManager.STATUS_RUNNING &&
      lastBytes >= 0 &&
      nowMs > lastTimestampMs &&
      result.downloadedBytes >= lastBytes
    ) {
      ((result.downloadedBytes - lastBytes) * 1000L / (nowMs - lastTimestampMs)).coerceAtLeast(0L)
    } else {
      0L
    }

    onProgress(result.copy(speedBytesPerSecond = speedBytesPerSecond))

    lastBytes = result.downloadedBytes
    lastTimestampMs = nowMs

    if (result.status == DownloadManager.STATUS_SUCCESSFUL || result.status == DownloadManager.STATUS_FAILED) {
      return
    }
    delay(200)
  }
}

private fun resolveDownloadResult(context: Context, downloadId: Long): DownloadResult {
  val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
  val query = DownloadManager.Query().setFilterById(downloadId)
  val status = manager.query(query).use { cursor ->
    if (!cursor.moveToFirst()) {
      return DownloadResult(DownloadStatus.Failed, null)
    }
    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
  }

  if (status != DownloadManager.STATUS_SUCCESSFUL) {
    return DownloadResult(DownloadStatus.Failed, null)
  }

  val uri = manager.getUriForDownloadedFile(downloadId)
  if (uri == null) {
    return DownloadResult(DownloadStatus.Failed, null)
  }

  return DownloadResult(DownloadStatus.Success, uri)
}

private fun launchInstallIntent(context: Context, apkUri: Uri): Boolean {
  return runCatching {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(apkUri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
    true
  }.getOrElse {
    false
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

private fun formatSpeed(bytesPerSecond: Long): String {
  if (bytesPerSecond <= 0L) {
    return "0 B/s"
  }
  return "${formatFileSize(bytesPerSecond)}/s"
}

private data class DownloadTrack(
  val status: Int,
  val progress: Float,
  val downloadedBytes: Long,
  val totalBytes: Long,
  val speedBytesPerSecond: Long
)

private enum class DownloadStatus {
  Success,
  Failed
}

private data class DownloadResult(
  val status: DownloadStatus,
  val uri: Uri?
)
