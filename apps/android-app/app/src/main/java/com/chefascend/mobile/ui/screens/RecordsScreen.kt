@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.BuildConfig
import com.chefascend.mobile.data.model.UserRecord
import com.chefascend.mobile.data.repository.ChefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecordsScreen(
  repository: ChefRepository,
  onBack: () -> Unit
) {
  var loading by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }
  var records by remember { mutableStateOf<List<UserRecord>>(emptyList()) }

  LaunchedEffect(Unit) {
    loading = true
    error = null
    runCatching {
      withContext(Dispatchers.IO) { repository.getUserRecords(BuildConfig.DEFAULT_USER_ID) }
    }.onSuccess {
      records = it.items
    }.onFailure {
      error = it.message ?: "Failed to load records"
    }
    loading = false
  }

  Scaffold(topBar = { TopAppBar(title = { Text("My cook records") }) }) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      when {
        loading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator()
          }
        }

        error != null -> {
          Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
        }

        records.isEmpty() -> {
          Text("No records yet", style = MaterialTheme.typography.bodyLarge)
        }

        else -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(records) { record ->
              RecordCard(record)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Back")
      }
      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

@Composable
private fun RecordCard(record: UserRecord) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
  ) {
    Text(record.dish_name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
    Text(
      text = "Result: ${record.result}  Rating: ${record.rating ?: "-"}",
      style = MaterialTheme.typography.bodyMedium
    )
    Text(
      text = "Cooked at: ${record.cooked_at}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.primary
    )
  }
}
