package com.chefascend.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.chefascend.mobile.data.model.DishSummary
import com.chefascend.mobile.data.repository.ChefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(
  repository: ChefRepository,
  onDishClick: (dishId: String) -> Unit,
  onOpenRecords: () -> Unit
) {
  var loading by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }
  var dishes by remember { mutableStateOf<List<DishSummary>>(emptyList()) }

  LaunchedEffect(Unit) {
    loading = true
    error = null
    runCatching {
      withContext(Dispatchers.IO) { repository.getCatalog() }
    }.onSuccess { response ->
      dishes = response.items
    }.onFailure {
      error = it.message ?: "Failed to load catalog"
    }
    loading = false
  }

  Scaffold(
    topBar = {
      TopAppBar(title = { Text("Chef Ascend") })
    },
    floatingActionButton = {
      FloatingActionButton(onClick = onOpenRecords) {
        Icon(Icons.Default.History, contentDescription = "Open records")
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Today popular dishes",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

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
          Text(
            text = error ?: "Unknown error",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
          )
        }

        else -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(dishes) { dish ->
              DishCard(dish = dish, onClick = { onDishClick(dish.id) })
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DishCard(dish: DishSummary, onClick: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = dish.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Difficulty ${dish.difficulty}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${dish.estimated_total_seconds / 60} min",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Today cooked ${dish.today_cook_count} times",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}
