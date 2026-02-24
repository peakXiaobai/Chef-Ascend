@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.BuildConfig
import com.chefascend.mobile.data.model.DishDetail
import com.chefascend.mobile.data.model.SessionState
import com.chefascend.mobile.data.repository.ChefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompletionSummary(
  val sessionId: String,
  val recordId: String,
  val result: String,
  val todayCount: Int
)

@Composable
fun CookModeScreen(
  dishId: String,
  sessionId: String,
  repository: ChefRepository,
  onBack: () -> Unit,
  onComplete: (CompletionSummary) -> Unit
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var loading by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }
  var detail by remember { mutableStateOf<DishDetail?>(null) }
  var session by remember { mutableStateOf<SessionState?>(null) }
  var remainingSeconds by remember { mutableIntStateOf(0) }
  var isPaused by remember { mutableStateOf(true) }
  var timerJob by remember { mutableStateOf<Job?>(null) }

  fun stopCountdown() {
    timerJob?.cancel()
    timerJob = null
  }

  fun triggerReminder() {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(400)
    }
  }

  fun startCountdown() {
    stopCountdown()
    if (isPaused || remainingSeconds <= 0) {
      return
    }

    timerJob = scope.launch {
      while (isActive && !isPaused && remainingSeconds > 0) {
        delay(1000)
        remainingSeconds -= 1
      }

      if (remainingSeconds <= 0) {
        isPaused = true
        triggerReminder()
      }
    }
  }

  suspend fun refreshState() {
    val latest = withContext(Dispatchers.IO) { repository.getSessionState(sessionId) }
    session = latest
    remainingSeconds = latest.timer.remaining_seconds
    isPaused = latest.timer.is_paused
    if (!isPaused) {
      startCountdown()
    } else {
      stopCountdown()
    }
  }

  LaunchedEffect(dishId, sessionId) {
    loading = true
    error = null
    runCatching {
      val loadedDetail = withContext(Dispatchers.IO) { repository.getDishDetail(dishId) }
      val loadedSession = withContext(Dispatchers.IO) { repository.getSessionState(sessionId) }
      loadedDetail to loadedSession
    }.onSuccess { (loadedDetail, loadedSession) ->
      detail = loadedDetail
      session = loadedSession
      remainingSeconds = loadedSession.timer.remaining_seconds
      isPaused = loadedSession.timer.is_paused
      if (!isPaused) {
        startCountdown()
      }
    }.onFailure {
      error = it.message ?: "Failed to load cooking mode"
    }
    loading = false
  }

  DisposableEffect(Unit) {
    onDispose { stopCountdown() }
  }

  Scaffold(topBar = { TopAppBar(title = { Text("Cook mode") }) }) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(padding)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Spacer(modifier = Modifier.height(10.dp))

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
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(onClick = onBack) {
            Text("Back")
          }
        }

        detail != null && session != null -> {
          val currentStepNo = session?.current_step_no ?: 1
          val currentStep = detail?.steps?.find { it.step_no == currentStepNo }

          Text(
            text = detail?.name ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Session #$sessionId | Status ${session?.status}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Step $currentStepNo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = currentStep?.title ?: "No step information",
            style = MaterialTheme.typography.titleMedium
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = currentStep?.instruction ?: "",
            style = MaterialTheme.typography.bodyLarge
          )

          Spacer(modifier = Modifier.height(20.dp))
          Text(
            text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
          )

          Text(
            text = if (isPaused) "Paused" else "Running",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) { repository.startStep(sessionId, currentStepNo) }
                    refreshState()
                  }.onFailure {
                    error = it.message ?: "Failed to start step"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Start step")
            }

            Button(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) { repository.completeStep(sessionId, currentStepNo) }
                    refreshState()
                  }.onFailure {
                    error = it.message ?: "Failed to complete step"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Complete step")
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) { repository.pauseTimer(sessionId) }
                    refreshState()
                  }.onFailure {
                    error = it.message ?: "Failed to pause timer"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Pause")
            }

            OutlinedButton(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) { repository.resumeTimer(sessionId) }
                    refreshState()
                  }.onFailure {
                    error = it.message ?: "Failed to resume timer"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Resume")
            }

            OutlinedButton(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) { repository.resetTimer(sessionId) }
                    refreshState()
                  }.onFailure {
                    error = it.message ?: "Failed to reset timer"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Reset")
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
          Text(
            text = "Finish this dish",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) {
                      repository.completeSession(
                        sessionId = sessionId,
                        userId = BuildConfig.DEFAULT_USER_ID,
                        result = "SUCCESS",
                        rating = 5
                      )
                    }
                  }.onSuccess { response ->
                    stopCountdown()
                    onComplete(
                      CompletionSummary(
                        sessionId = response.session_id,
                        recordId = response.record_id,
                        result = response.result,
                        todayCount = response.today_cook_count
                      )
                    )
                  }.onFailure {
                    error = it.message ?: "Failed to finish session"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Mark success")
            }

            OutlinedButton(
              onClick = {
                scope.launch {
                  runCatching {
                    withContext(Dispatchers.IO) {
                      repository.completeSession(
                        sessionId = sessionId,
                        userId = BuildConfig.DEFAULT_USER_ID,
                        result = "FAILED",
                        note = "Need more practice"
                      )
                    }
                  }.onSuccess { response ->
                    stopCountdown()
                    onComplete(
                      CompletionSummary(
                        sessionId = response.session_id,
                        recordId = response.record_id,
                        result = response.result,
                        todayCount = response.today_cook_count
                      )
                    )
                  }.onFailure {
                    error = it.message ?: "Failed to finish session"
                  }
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Mark failed")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
          }
          Spacer(modifier = Modifier.height(18.dp))
        }
      }
    }
  }
}
