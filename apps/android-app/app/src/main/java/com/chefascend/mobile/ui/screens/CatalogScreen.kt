@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.R
import com.chefascend.mobile.data.model.DishSummary
import com.chefascend.mobile.data.repository.ChefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(
  repository: ChefRepository,
  onDishClick: (dishId: String) -> Unit,
  onOpenRecords: () -> Unit,
  onOpenSettings: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var loading by remember { mutableStateOf(true) }
  var refreshing by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var dishes by remember { mutableStateOf<List<DishSummary>>(emptyList()) }
  val errorLoadCatalog = stringResource(R.string.error_load_catalog)

  suspend fun loadCatalog(showLoading: Boolean) {
    if (showLoading) {
      loading = true
    } else {
      refreshing = true
    }
    error = null
    runCatching {
      withContext(Dispatchers.IO) { repository.getCatalog() }
    }.onSuccess { response ->
      dishes = response.items
    }.onFailure {
      error = errorLoadCatalog
    }
    if (showLoading) {
      loading = false
    } else {
      refreshing = false
    }
  }

  LaunchedEffect(Unit) {
    loadCatalog(showLoading = true)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.catalog_title)) },
        actions = {
          IconButton(onClick = onOpenSettings) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = stringResource(R.string.settings_title)
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = onOpenRecords) {
        Icon(Icons.Default.History, contentDescription = stringResource(R.string.records_title))
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
        text = stringResource(R.string.catalog_header),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
          if (!loading) {
            scope.launch {
              loadCatalog(showLoading = false)
            }
          }
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
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
              text = error ?: stringResource(R.string.error_generic),
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
          text = stringResource(R.string.catalog_difficulty, dish.difficulty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = stringResource(R.string.catalog_minutes, dish.estimated_total_seconds / 60),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.catalog_today_count, dish.today_cook_count),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}
