@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

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
import androidx.compose.material3.Button
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
import com.chefascend.mobile.data.model.DishDetail
import com.chefascend.mobile.data.model.DishIngredient
import com.chefascend.mobile.data.model.DishStep
import com.chefascend.mobile.data.repository.ChefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DishDetailScreen(
  dishId: String,
  repository: ChefRepository,
  onBack: () -> Unit,
  onStartCooking: (dishId: String, sessionId: String) -> Unit
) {
  var detail by remember { mutableStateOf<DishDetail?>(null) }
  var loading by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }
  var creatingSession by remember { mutableStateOf(false) }

  LaunchedEffect(dishId) {
    loading = true
    error = null
    runCatching {
      withContext(Dispatchers.IO) { repository.getDishDetail(dishId) }
    }.onSuccess {
      detail = it
    }.onFailure {
      error = it.message ?: "Failed to load dish detail"
    }
    loading = false
  }

  Scaffold(topBar = { TopAppBar(title = { Text("Dish detail") }) }) { padding ->
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
          Text(text = error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(onClick = onBack) {
            Text("Back")
          }
        }

        detail != null -> {
          val loadedDetail = detail ?: return@Column
          val numericDishId = loadedDetail.id.toLongOrNull()
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
              Text(
                text = loadedDetail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = loadedDetail.description ?: "No description",
                style = MaterialTheme.typography.bodyMedium
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Difficulty ${loadedDetail.difficulty} | ${loadedDetail.estimated_total_seconds / 60} min",
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = "Today cooked ${loadedDetail.today_cook_count} times",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
              )
              if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = error ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error
                )
              }
            }

            item {
              SectionTitle("Ingredients")
            }

            items(loadedDetail.ingredients) { ingredient ->
              IngredientRow(ingredient)
            }

            item {
              SectionTitle("Steps")
            }

            items(loadedDetail.steps) { step ->
              StepRow(step)
            }

            item {
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  if (creatingSession) {
                    return@Button
                  }
                  creatingSession = true
                  error = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !creatingSession
              ) {
                Text(if (creatingSession) "Starting..." else "Start cooking")
              }
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
              }
              Spacer(modifier = Modifier.height(16.dp))
            }
          }

          if (creatingSession) {
            LaunchedEffect(creatingSession) {
              runCatching {
                withContext(Dispatchers.IO) {
                  if (numericDishId == null) {
                    error("Invalid dish id")
                  }
                  repository.startSession(
                    dishId = numericDishId,
                    userId = BuildConfig.DEFAULT_USER_ID
                  )
                }
              }.onSuccess { session ->
                onStartCooking(loadedDetail.id, session.session_id)
              }.onFailure {
                error = it.message ?: "Failed to start cooking session"
              }
              creatingSession = false
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold
  )
}

@Composable
private fun IngredientRow(ingredient: DishIngredient) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = ingredient.name, style = MaterialTheme.typography.bodyMedium)
    Text(text = ingredient.amount, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun StepRow(step: DishStep) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Text(
      text = "Step ${step.step_no}: ${step.title}",
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium
    )
    Text(text = step.instruction, style = MaterialTheme.typography.bodyMedium)
    Text(
      text = "Timer ${step.timer_seconds}s | Reminder ${step.remind_mode}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.primary
    )
  }
}
