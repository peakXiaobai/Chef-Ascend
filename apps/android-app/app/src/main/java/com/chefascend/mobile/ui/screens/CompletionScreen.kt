@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.R

@Composable
fun CompletionScreen(
  sessionId: String,
  recordId: String,
  result: String,
  todayCount: Int,
  onBackToCatalog: () -> Unit,
  onOpenRecords: () -> Unit
) {
  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.completion_title)) }) }) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 20.dp),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = if (result == "SUCCESS") {
          stringResource(R.string.completion_success_message)
        } else {
          stringResource(R.string.completion_failure_message)
        },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        stringResource(R.string.completion_session_id, sessionId),
        style = MaterialTheme.typography.bodyLarge
      )
      Text(
        stringResource(R.string.completion_record_id, recordId),
        style = MaterialTheme.typography.bodyLarge
      )
      Text(
        stringResource(R.string.completion_result, resultLabel(result)),
        style = MaterialTheme.typography.bodyLarge
      )
      Text(
        text = stringResource(R.string.completion_today_count, todayCount),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.tertiary
      )

      if (result == "FAILED") {
        Spacer(modifier = Modifier.height(14.dp))
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = stringResource(R.string.completion_failure_advice_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(R.string.completion_failure_advice_body),
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
      Button(onClick = onOpenRecords, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.completion_view_records))
      }
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(onClick = onBackToCatalog, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.completion_back_catalog))
      }
    }
  }
}

@Composable
private fun resultLabel(result: String): String {
  return when (result) {
    "SUCCESS" -> stringResource(R.string.result_success)
    "FAILED" -> stringResource(R.string.result_failed)
    else -> result
  }
}
