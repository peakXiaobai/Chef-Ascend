package com.chefascend.mobile.ui.screens

sealed class ScreenRoutes(val route: String) {
  data object Catalog : ScreenRoutes("catalog")
  data object Records : ScreenRoutes("records")
  data object Settings : ScreenRoutes("settings")
  data object Detail : ScreenRoutes("detail/{dishId}") {
    fun create(dishId: String): String = "detail/$dishId"
  }

  data object CookMode : ScreenRoutes("cook/{dishId}/{sessionId}") {
    fun create(dishId: String, sessionId: String): String = "cook/$dishId/$sessionId"
  }

  data object Completion : ScreenRoutes("completion/{sessionId}/{recordId}/{result}/{todayCount}") {
    fun create(sessionId: String, recordId: String, result: String, todayCount: Int): String {
      return "completion/$sessionId/$recordId/$result/$todayCount"
    }
  }
}
