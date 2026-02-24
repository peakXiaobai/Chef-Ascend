package com.chefascend.mobile.data.repository

import com.chefascend.mobile.data.api.ChefApiService
import com.chefascend.mobile.data.model.CatalogResponse
import com.chefascend.mobile.data.model.CompleteSessionRequest
import com.chefascend.mobile.data.model.CompleteSessionResponse
import com.chefascend.mobile.data.model.CreateSessionRequest
import com.chefascend.mobile.data.model.CreateSessionResponse
import com.chefascend.mobile.data.model.DishDetail
import com.chefascend.mobile.data.model.SessionState
import com.chefascend.mobile.data.model.StepActionResponse
import com.chefascend.mobile.data.model.UserRecordsResponse

class ChefRepository(private val api: ChefApiService) {
  suspend fun getCatalog(): CatalogResponse = api.getDishes()

  suspend fun getDishDetail(dishId: String): DishDetail = api.getDishDetail(dishId)

  suspend fun startSession(dishId: Long, userId: Long): CreateSessionResponse {
    return api.startSession(CreateSessionRequest(dish_id = dishId, user_id = userId))
  }

  suspend fun getSessionState(sessionId: String): SessionState = api.getSessionState(sessionId)

  suspend fun startStep(sessionId: String, stepNo: Int): StepActionResponse = api.startStep(sessionId, stepNo)

  suspend fun completeStep(sessionId: String, stepNo: Int): StepActionResponse =
    api.completeStep(sessionId, stepNo)

  suspend fun pauseTimer(sessionId: String): SessionState = api.pauseTimer(sessionId)

  suspend fun resumeTimer(sessionId: String): SessionState = api.resumeTimer(sessionId)

  suspend fun resetTimer(sessionId: String): SessionState = api.resetTimer(sessionId)

  suspend fun completeSession(
    sessionId: String,
    userId: Long,
    result: String,
    rating: Int? = null,
    note: String? = null
  ): CompleteSessionResponse {
    return api.completeSession(
      sessionId,
      CompleteSessionRequest(user_id = userId, result = result, rating = rating, note = note)
    )
  }

  suspend fun getUserRecords(userId: Long): UserRecordsResponse = api.getUserRecords(userId)
}
