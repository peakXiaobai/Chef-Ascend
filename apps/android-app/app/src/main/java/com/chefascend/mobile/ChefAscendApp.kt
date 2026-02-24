package com.chefascend.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chefascend.mobile.data.api.ApiClient
import com.chefascend.mobile.data.repository.ChefRepository
import com.chefascend.mobile.ui.screens.CatalogScreen
import com.chefascend.mobile.ui.screens.CompletionScreen
import com.chefascend.mobile.ui.screens.CookModeScreen
import com.chefascend.mobile.ui.screens.DishDetailScreen
import com.chefascend.mobile.ui.screens.RecordsScreen
import com.chefascend.mobile.ui.screens.ScreenRoutes

@Composable
fun ChefAscendApp() {
  val navController = rememberNavController()
  val repository = remember { ChefRepository(ApiClient.chefApiService) }

  NavHost(navController = navController, startDestination = ScreenRoutes.Catalog.route) {
    composable(ScreenRoutes.Catalog.route) {
      CatalogScreen(
        repository = repository,
        onDishClick = { dishId -> navController.navigate(ScreenRoutes.Detail.create(dishId)) },
        onOpenRecords = { navController.navigate(ScreenRoutes.Records.route) }
      )
    }

    composable(
      route = ScreenRoutes.Detail.route,
      arguments = listOf(navArgument("dishId") { type = NavType.StringType })
    ) { backStackEntry ->
      val dishId = backStackEntry.arguments?.getString("dishId") ?: return@composable
      DishDetailScreen(
        dishId = dishId,
        repository = repository,
        onBack = { navController.popBackStack() },
        onStartCooking = { returnedDishId, sessionId ->
          navController.navigate(ScreenRoutes.CookMode.create(returnedDishId, sessionId))
        }
      )
    }

    composable(
      route = ScreenRoutes.CookMode.route,
      arguments = listOf(
        navArgument("dishId") { type = NavType.StringType },
        navArgument("sessionId") { type = NavType.StringType }
      )
    ) { backStackEntry ->
      val dishId = backStackEntry.arguments?.getString("dishId") ?: return@composable
      val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
      CookModeScreen(
        dishId = dishId,
        sessionId = sessionId,
        repository = repository,
        onBack = { navController.popBackStack() },
        onComplete = { summary ->
          navController.navigate(
            ScreenRoutes.Completion.create(
              sessionId = summary.sessionId,
              recordId = summary.recordId,
              result = summary.result,
              todayCount = summary.todayCount
            )
          )
        }
      )
    }

    composable(
      route = ScreenRoutes.Completion.route,
      arguments = listOf(
        navArgument("sessionId") { type = NavType.StringType },
        navArgument("recordId") { type = NavType.StringType },
        navArgument("result") { type = NavType.StringType },
        navArgument("todayCount") { type = NavType.IntType }
      )
    ) { backStackEntry ->
      val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
      val recordId = backStackEntry.arguments?.getString("recordId") ?: return@composable
      val result = backStackEntry.arguments?.getString("result") ?: return@composable
      val todayCount = backStackEntry.arguments?.getInt("todayCount") ?: 0

      CompletionScreen(
        sessionId = sessionId,
        recordId = recordId,
        result = result,
        todayCount = todayCount,
        onBackToCatalog = {
          navController.navigate(ScreenRoutes.Catalog.route) {
            popUpTo(ScreenRoutes.Catalog.route) { inclusive = true }
          }
        },
        onOpenRecords = { navController.navigate(ScreenRoutes.Records.route) }
      )
    }

    composable(ScreenRoutes.Records.route) {
      RecordsScreen(
        repository = repository,
        onBack = { navController.popBackStack() }
      )
    }
  }
}
