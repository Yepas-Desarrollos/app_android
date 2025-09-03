package mx.checklist.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.checklist.ui.navigation.NavRoutes
import mx.checklist.ui.screens.*

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = NavRoutes.LOGIN) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(onLogged = { nav.navigate(NavRoutes.STORES) { popUpTo(0) } })
        }
        composable(NavRoutes.STORES) {
            StoresScreen(onStoreSelected = { code ->
                nav.navigate(NavRoutes.templates(code))
            })
        }
        composable(
            route = NavRoutes.TEMPLATES,
            arguments = listOf(navArgument("storeCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeCode = backStackEntry.arguments?.getString("storeCode")!!
            TemplatesScreen(storeCode = storeCode, onRunCreated = { runId ->
                nav.navigate(NavRoutes.run(runId))
            })
        }
        composable(
            route = NavRoutes.RUN,
            arguments = listOf(navArgument("runId") { type = NavType.LongType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getLong("runId")!!
            RunScreen(runId = runId, onSubmitted = {
                nav.navigate(NavRoutes.STORES) { popUpTo(0) }
            })
        }
    }
}
