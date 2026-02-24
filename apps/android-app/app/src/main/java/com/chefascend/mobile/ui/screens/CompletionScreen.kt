package com.chefascend.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CompletionScreen(
  sessionId: String,
  recordId: String,
  result: String,
  todayCount: Int,
  onBackToCatalog: () -> Unit,
  onOpenRecords: () -> Unit
) {
  Scaffold(topBar = { TopAppBar(title = { Text("Cook completed") }) }) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 20.dp),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = if (result == "SUCCESS") "Great cooking!" else "Session recorded",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text("Session ID: $sessionId", style = MaterialTheme.typography.bodyLarge)
      Text("Record ID: $recordId", style = MaterialTheme.typography.bodyLarge)
      Text("Result: $result", style = MaterialTheme.typography.bodyLarge)
      Text(
        text = "Today cooked count: $todayCount",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.tertiary
      )

      Spacer(modifier = Modifier.height(24.dp))
      Button(onClick = onOpenRecords, modifier = Modifier.fillMaxWidth()) {
        Text("View my records")
      }
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(onClick = onBackToCatalog, modifier = Modifier.fillMaxWidth()) {
        Text("Back to catalog")
      }
    }
  }
}
