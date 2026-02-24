@file:OptIn(ExperimentalMaterial3Api::class)

package com.chefascend.mobile.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefascend.mobile.BuildConfig
import com.chefascend.mobile.R
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
  var actionLoading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var detail by remember { mutableStateOf<DishDetail?>(null) }
  var session by remember { mutableStateOf<SessionState?>(null) }
  var remainingSeconds by remember { mutableIntStateOf(0) }
  var previewStepNo by remember { mutableIntStateOf(1) }
  var isPaused by remember { mutableStateOf(true) }
  var showFinishActions by remember { mutableStateOf(false) }
  var timerJob by remember { mutableStateOf<Job?>(null) }

  val errorLoadCookMode = stringResource(R.string.error_load_cook_mode)
  val errorCompleteStep = stringResource(R.string.error_complete_step)
  val errorPauseTimer = stringResource(R.string.error_pause_timer)
  val errorResumeTimer = stringResource(R.string.error_resume_timer)
  val errorFinishSession = stringResource(R.string.error_finish_session)
  val failedNote = stringResource(R.string.cook_mode_failed_note)
  val errorNeedCurrentStep = stringResource(R.string.cook_mode_need_current_step)
  val finishReadyMessage = stringResource(R.string.cook_mode_finish_ready)

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

  suspend fun refreshState(resetPreviewStep: Boolean) {
    val latest = withContext(Dispatchers.IO) { repository.getSessionState(sessionId) }
    session = latest
    remainingSeconds = latest.timer.remaining_seconds
    isPaused = latest.timer.is_paused

    if (resetPreviewStep) {
      previewStepNo = latest.current_step_no
    }

    val maxStepNo = detail?.steps?.maxOfOrNull { it.step_no } ?: latest.current_step_no
    if (latest.current_step_no >= maxStepNo && latest.timer.remaining_seconds <= 0) {
      showFinishActions = true
      error = null
    }

    if (isPaused) {
      stopCountdown()
    } else {
      startCountdown()
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
      previewStepNo = loadedSession.current_step_no
      remainingSeconds = loadedSession.timer.remaining_seconds
      isPaused = loadedSession.timer.is_paused
      val maxStepNo = loadedDetail.steps.maxOfOrNull { it.step_no } ?: 1
      showFinishActions = loadedSession.current_step_no >= maxStepNo && loadedSession.timer.remaining_seconds <= 0
      if (!isPaused) {
        startCountdown()
      }
    }.onFailure {
      error = errorLoadCookMode
    }
    loading = false
  }

  DisposableEffect(Unit) {
    onDispose { stopCountdown() }
  }

  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.cook_mode_title)) }) }) { padding ->
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

        detail == null || session == null -> {
          Text(error ?: stringResource(R.string.error_generic), color = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.common_back))
          }
        }

        else -> {
          val loadedDetail = detail ?: return@Column
          val loadedSession = session ?: return@Column
          val maxStepNo = loadedDetail.steps.maxOfOrNull { it.step_no } ?: 1
          val currentStepNo = loadedSession.current_step_no
          val displayStepNo = previewStepNo.coerceIn(1, maxStepNo)
          val displayStep = loadedDetail.steps.find { it.step_no == displayStepNo }
          val isFirstStep = displayStepNo <= 1
          val isLastStep = displayStepNo >= maxStepNo
          val isPreviewingOtherStep = displayStepNo != currentStepNo
          val canFinishNow = showFinishActions || (currentStepNo >= maxStepNo && remainingSeconds <= 0)

          Text(
            text = loadedDetail.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(
              R.string.cook_mode_session_status,
              sessionId,
              sessionStatusLabel(loadedSession.status)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(14.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(
                Brush.verticalGradient(
                  listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surface
                  )
                )
              )
              .padding(horizontal = 14.dp, vertical = 16.dp)
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
              Text(
                text = formatSeconds(remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.tertiary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (isPaused) {
                  stringResource(R.string.cook_mode_paused)
                } else {
                  stringResource(R.string.cook_mode_running)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(12.dp))

              AnimatedChefAssistant(
                isPaused = isPaused,
                enabled = !actionLoading,
                onClick = {
                  if (actionLoading) {
                    return@AnimatedChefAssistant
                  }
                  actionLoading = true
                  scope.launch {
                    runCatching {
                      if (isPaused) {
                        withContext(Dispatchers.IO) { repository.resumeTimer(sessionId) }
                      } else {
                        withContext(Dispatchers.IO) { repository.pauseTimer(sessionId) }
                      }
                      refreshState(resetPreviewStep = false)
                    }.onFailure {
                      error = if (isPaused) errorResumeTimer else errorPauseTimer
                    }
                    actionLoading = false
                  }
                }
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (isPaused) {
                  stringResource(R.string.cook_mode_helper_start)
                } else {
                  stringResource(R.string.cook_mode_helper_pause)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = stringResource(R.string.cook_mode_step_no, displayStepNo),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              if (isPreviewingOtherStep) {
                Text(
                  text = stringResource(R.string.cook_mode_previewing_step, displayStepNo, currentStepNo),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
              Text(
                text = displayStep?.title ?: stringResource(R.string.cook_mode_no_step_info),
                style = MaterialTheme.typography.titleMedium
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = displayStep?.instruction ?: "",
                style = MaterialTheme.typography.bodyLarge
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(
                  R.string.detail_step_timer_mode,
                  displayStep?.timer_seconds ?: 0,
                  displayStep?.remind_mode ?: "-"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = {
                if (isFirstStep) {
                  onBack()
                } else {
                  previewStepNo = (displayStepNo - 1).coerceAtLeast(1)
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text(
                if (isFirstStep) {
                  stringResource(R.string.common_back)
                } else {
                  stringResource(R.string.cook_mode_prev_step)
                }
              )
            }

            OutlinedButton(
              onClick = {
                if (isLastStep) {
                  showFinishActions = true
                  error = null
                } else {
                  previewStepNo = (displayStepNo + 1).coerceAtMost(maxStepNo)
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text(
                if (isLastStep) {
                  stringResource(R.string.cook_mode_finish_early)
                } else {
                  stringResource(R.string.cook_mode_next_step)
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              if (actionLoading) {
                return@Button
              }
              actionLoading = true
              scope.launch {
                if (isPreviewingOtherStep) {
                  error = errorNeedCurrentStep
                  actionLoading = false
                  return@launch
                }

                runCatching {
                  withContext(Dispatchers.IO) { repository.completeStep(sessionId, currentStepNo) }
                  refreshState(resetPreviewStep = true)
                  if (currentStepNo >= maxStepNo) {
                    showFinishActions = true
                  }
                  error = null
                }.onFailure {
                  error = errorCompleteStep
                }
                actionLoading = false
              }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionLoading && !canFinishNow
          ) {
            Text(stringResource(R.string.cook_mode_complete_step))
          }

          if (canFinishNow) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = finishReadyMessage,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = {
                  if (actionLoading) {
                    return@Button
                  }
                  actionLoading = true
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
                      error = errorFinishSession
                    }
                    actionLoading = false
                  }
                },
                modifier = Modifier.weight(1f),
                enabled = !actionLoading
              ) {
                Text(stringResource(R.string.cook_mode_mark_success))
              }

              OutlinedButton(
                onClick = {
                  if (actionLoading) {
                    return@OutlinedButton
                  }
                  actionLoading = true
                  scope.launch {
                    runCatching {
                      withContext(Dispatchers.IO) {
                        repository.completeSession(
                          sessionId = sessionId,
                          userId = BuildConfig.DEFAULT_USER_ID,
                          result = "FAILED",
                          note = failedNote
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
                      error = errorFinishSession
                    }
                    actionLoading = false
                  }
                },
                modifier = Modifier.weight(1f),
                enabled = !actionLoading
              ) {
                Text(stringResource(R.string.cook_mode_mark_failed))
              }
            }
          }

          if (error != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = error ?: stringResource(R.string.error_generic),
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun AnimatedChefAssistant(
  isPaused: Boolean,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "assistant")
  val bobOffset by infiniteTransition.animateFloat(
    initialValue = -8f,
    targetValue = 8f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bob"
  )
  val bodySway by infiniteTransition.animateFloat(
    initialValue = -5f,
    targetValue = 5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sway"
  )
  val armSwing by infiniteTransition.animateFloat(
    initialValue = -7f,
    targetValue = 7f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 550),
      repeatMode = RepeatMode.Reverse
    ),
    label = "arm"
  )
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 700),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  val liveBob = if (isPaused) 0f else bobOffset
  val liveSway = if (isPaused) 0f else bodySway
  val liveArm = if (isPaused) 0f else armSwing
  val liveScale = if (isPaused) 1f else pulseScale

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp)),
    colors = CardDefaults.cardColors(
      containerColor = if (isPaused) {
        MaterialTheme.colorScheme.surfaceVariant
      } else {
        MaterialTheme.colorScheme.secondaryContainer
      }
    ),
    onClick = {
      if (enabled) {
        onClick()
      }
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = stringResource(R.string.cook_mode_helper_name),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      Canvas(
        modifier = Modifier
          .size(168.dp)
          .graphicsLayer {
            translationY = liveBob
            rotationZ = liveSway
            scaleX = liveScale
            scaleY = liveScale
          }
      ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f

        drawOval(
          color = Color(0x33000000),
          topLeft = Offset(w * 0.28f, h * 0.83f),
          size = Size(w * 0.44f, h * 0.08f)
        )

        drawRoundRect(
          color = Color(0xFF57B8FF),
          topLeft = Offset(w * 0.38f, h * 0.42f),
          size = Size(w * 0.24f, h * 0.34f),
          cornerRadius = CornerRadius(x = w * 0.09f, y = w * 0.09f)
        )

        drawCircle(
          color = Color(0xFFFDFCF7),
          radius = w * 0.16f,
          center = Offset(centerX, h * 0.28f)
        )

        drawCircle(
          color = Color(0xFF2C2D30),
          radius = w * 0.016f,
          center = Offset(centerX - w * 0.05f, h * 0.27f)
        )
        drawCircle(
          color = Color(0xFF2C2D30),
          radius = w * 0.016f,
          center = Offset(centerX + w * 0.05f, h * 0.27f)
        )

        drawRoundRect(
          color = Color(0xFF2C2D30),
          topLeft = Offset(centerX - w * 0.03f, h * 0.33f),
          size = Size(w * 0.06f, h * 0.014f),
          cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )

        drawRoundRect(
          color = Color(0xFFFDFCF7),
          topLeft = Offset(centerX - w * 0.05f + liveArm, h * 0.47f),
          size = Size(w * 0.07f, h * 0.045f),
          cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )
        drawRoundRect(
          color = Color(0xFFFDFCF7),
          topLeft = Offset(centerX - w * 0.02f - liveArm, h * 0.47f),
          size = Size(w * 0.07f, h * 0.045f),
          cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        drawRoundRect(
          color = Color(0xFFFFA34F),
          topLeft = Offset(centerX + w * 0.11f, h * 0.12f),
          size = Size(w * 0.026f, h * 0.22f),
          cornerRadius = CornerRadius(w * 0.012f, w * 0.012f)
        )
        drawCircle(
          color = Color(0xFFFFA34F),
          radius = w * 0.04f,
          center = Offset(centerX + w * 0.12f, h * 0.11f)
        )
      }
    }
  }
}

private fun formatSeconds(seconds: Int): String {
  val safeValue = if (seconds < 0) 0 else seconds
  val minute = safeValue / 60
  val second = safeValue % 60
  return String.format("%02d:%02d", minute, second)
}

@Composable
private fun sessionStatusLabel(status: String?): String {
  return when (status) {
    "IN_PROGRESS" -> stringResource(R.string.status_in_progress)
    "COMPLETED" -> stringResource(R.string.status_completed)
    "ABANDONED" -> stringResource(R.string.status_abandoned)
    else -> status ?: "-"
  }
}
