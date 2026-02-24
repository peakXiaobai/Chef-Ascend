package com.chefascend.mobile.data.model

data class CatalogResponse(
  val page: Int,
  val page_size: Int,
  val total: Int,
  val items: List<DishSummary>
)

data class DishSummary(
  val id: String,
  val name: String,
  val difficulty: Int,
  val estimated_total_seconds: Int,
  val cover_image_url: String?,
  val today_cook_count: Int
)

data class DishDetail(
  val id: String,
  val name: String,
  val description: String?,
  val difficulty: Int,
  val estimated_total_seconds: Int,
  val cover_image_url: String?,
  val today_cook_count: Int,
  val ingredients: List<DishIngredient>,
  val steps: List<DishStep>
)

data class DishIngredient(
  val name: String,
  val amount: String
)

data class DishStep(
  val step_no: Int,
  val title: String,
  val instruction: String,
  val timer_seconds: Int,
  val remind_mode: String
)

data class CreateSessionRequest(
  val dish_id: Long,
  val user_id: Long
)

data class SessionState(
  val session_id: String,
  val status: String,
  val current_step_no: Int,
  val timer: SessionTimer
)

data class SessionTimer(
  val remaining_seconds: Int,
  val is_paused: Boolean
)

data class CreateSessionResponse(
  val session_id: String,
  val dish_id: String,
  val status: String,
  val current_step_no: Int,
  val started_at: String
)

data class StepActionResponse(
  val session_id: String,
  val current_step_no: Int,
  val status: String
)

data class CompleteSessionRequest(
  val user_id: Long,
  val result: String,
  val rating: Int? = null,
  val note: String? = null
)

data class CompleteSessionResponse(
  val session_id: String,
  val record_id: String,
  val result: String,
  val today_cook_count: Int
)

data class UserRecordsResponse(
  val page: Int,
  val page_size: Int,
  val total: Int,
  val items: List<UserRecord>
)

data class UserRecord(
  val record_id: String,
  val dish_id: String,
  val dish_name: String,
  val result: String,
  val rating: Int?,
  val cooked_at: String
)
