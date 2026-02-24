package com.chefascend.mobile.data.api

import com.chefascend.mobile.data.model.CatalogResponse
import com.chefascend.mobile.data.model.CompleteSessionRequest
import com.chefascend.mobile.data.model.CompleteSessionResponse
import com.chefascend.mobile.data.model.CreateSessionRequest
import com.chefascend.mobile.data.model.CreateSessionResponse
import com.chefascend.mobile.data.model.DishDetail
import com.chefascend.mobile.data.model.SessionState
import com.chefascend.mobile.data.model.StepActionResponse
import com.chefascend.mobile.data.model.UserRecordsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChefApiService {
  @GET("api/v1/dishes")
  suspend fun getDishes(
    @Query("page") page: Int = 1,
    @Query("page_size") pageSize: Int = 20,
    @Query("sort") sort: String = "popular_today"
  ): CatalogResponse

  @GET("api/v1/dishes/{dishId}")
  suspend fun getDishDetail(@Path("dishId") dishId: String): DishDetail

  @POST("api/v1/cook-sessions")
  suspend fun startSession(@Body body: CreateSessionRequest): CreateSessionResponse

  @GET("api/v1/cook-sessions/{sessionId}")
  suspend fun getSessionState(@Path("sessionId") sessionId: String): SessionState

  @POST("api/v1/cook-sessions/{sessionId}/steps/{stepNo}/start")
  suspend fun startStep(
    @Path("sessionId") sessionId: String,
    @Path("stepNo") stepNo: Int
  ): StepActionResponse

  @POST("api/v1/cook-sessions/{sessionId}/steps/{stepNo}/complete")
  suspend fun completeStep(
    @Path("sessionId") sessionId: String,
    @Path("stepNo") stepNo: Int
  ): StepActionResponse

  @POST("api/v1/cook-sessions/{sessionId}/timer/pause")
  suspend fun pauseTimer(@Path("sessionId") sessionId: String): SessionState

  @POST("api/v1/cook-sessions/{sessionId}/timer/resume")
  suspend fun resumeTimer(@Path("sessionId") sessionId: String): SessionState

  @POST("api/v1/cook-sessions/{sessionId}/timer/reset")
  suspend fun resetTimer(@Path("sessionId") sessionId: String): SessionState

  @POST("api/v1/cook-sessions/{sessionId}/complete")
  suspend fun completeSession(
    @Path("sessionId") sessionId: String,
    @Body body: CompleteSessionRequest
  ): CompleteSessionResponse

  @GET("api/v1/users/{userId}/cook-records")
  suspend fun getUserRecords(
    @Path("userId") userId: Long,
    @Query("page") page: Int = 1,
    @Query("page_size") pageSize: Int = 20
  ): UserRecordsResponse
}
