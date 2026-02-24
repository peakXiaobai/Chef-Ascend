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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.BuildConfig
import com.chefascend.mobile.R
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

  val errorLoadDetail = stringResource(R.string.error_load_detail)
  val errorStartSession = stringResource(R.string.error_start_session)
  val errorInvalidDishId = stringResource(R.string.error_invalid_dish_id)

  LaunchedEffect(dishId) {
    loading = true
    error = null
    runCatching {
      withContext(Dispatchers.IO) { repository.getDishDetail(dishId) }
    }.onSuccess {
      detail = it
    }.onFailure {
      error = errorLoadDetail
    }
    loading = false
  }

  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.detail_title)) }) }) { padding ->
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
          Text(text = error ?: stringResource(R.string.error_generic), color = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.common_back))
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
                text = loadedDetail.description ?: stringResource(R.string.detail_no_description),
                style = MaterialTheme.typography.bodyMedium
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(
                  R.string.detail_difficulty_minutes,
                  loadedDetail.difficulty,
                  loadedDetail.estimated_total_seconds / 60
                ),
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = stringResource(R.string.catalog_today_count, loadedDetail.today_cook_count),
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
              SectionTitle(stringResource(R.string.detail_ingredients_title))
            }

            items(loadedDetail.ingredients) { ingredient ->
              IngredientRow(ingredient)
            }

            item {
              SectionTitle(stringResource(R.string.detail_steps_title))
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
                Text(
                  if (creatingSession) {
                    stringResource(R.string.detail_starting)
                  } else {
                    stringResource(R.string.detail_start_cooking)
                  }
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_back))
              }
              Spacer(modifier = Modifier.height(16.dp))
            }
          }

          if (creatingSession) {
            LaunchedEffect(creatingSession) {
              runCatching {
                withContext(Dispatchers.IO) {
                  val selectedDishId = numericDishId ?: error(errorInvalidDishId)
                  repository.startSession(
                    dishId = selectedDishId,
                    userId = BuildConfig.DEFAULT_USER_ID
                  )
                }
              }.onSuccess { session ->
                onStartCooking(loadedDetail.id, session.session_id)
              }.onFailure {
                error = errorStartSession
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
      text = stringResource(R.string.detail_step_title, step.step_no, step.title),
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium
    )
    Text(text = step.instruction, style = MaterialTheme.typography.bodyMedium)
    Text(
      text = stringResource(R.string.detail_step_timer_mode, step.timer_seconds, step.remind_mode),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.primary
    )
  }
}
