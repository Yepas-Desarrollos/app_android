package mx.checklist.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.checklist.ui.screens.ItemsScreen
import mx.checklist.ui.screens.LoginScreen
import mx.checklist.ui.screens.RunScreen
import mx.checklist.ui.screens.StoresScreen
import mx.checklist.ui.screens.TemplatesScreen
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun AppNavHost(
    authVM: AuthViewModel,
    runsVM: RunsViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.Login
) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // LOGIN
        composable(Routes.Login) {
            LoginScreen(
                vm = authVM,
                onLoggedIn = {
                    nav.navigate(Routes.Stores) { popUpTo(0) }
                }
            )
        }

        // STORES
        composable(Routes.Stores) {
            StoresScreen(
                vm = runsVM,
                onStoreSelected = { storeCode: String ->
                    nav.navigate(Routes.template(storeCode))
                }
            )
        }

        // TEMPLATES (storeCode como path param)
        composable(
            route = Routes.Templates,
            arguments = listOf(
                navArgument("storeCode") { type = NavType.StringType }
            )
        ) { backStack ->
            val storeCode = requireNotNull(backStack.arguments?.getString("storeCode")) {
                "storeCode es requerido"
            }

            TemplatesScreen(
                storeCode = storeCode,
                vm = runsVM,
                onRunCreated = { runId: Long, sc: String ->
                    // Usa query param, consistente con Routes.items(...)
                    nav.navigate(Routes.items(runId, sc))
                }
            )
        }

        // ITEMS (runId como path + storeCode como query param)
        composable(
            route = Routes.Items,
            arguments = listOf(
                navArgument("runId") { type = NavType.LongType },
                navArgument("storeCode") {
                    type = NavType.StringType
                    nullable = true // opcional en la ruta, pero lo exigimos abajo
                }
            )
        ) { backStack ->
            val runId = requireNotNull(backStack.arguments?.getLong("runId")) {
                "runId es requerido"
            }
            val storeCode = backStack.arguments?.getString("storeCode")
                ?: error("storeCode es requerido")

            ItemsScreen(
                runId = runId,
                storeCode = storeCode,
                vm = runsVM,
                onSubmit = {
                    nav.navigate(Routes.Stores) { popUpTo(0) }
                }
            )
        }

        // (Opcional) Resumen/envío directo por ID
        composable(
            route = "run/{runId}",
            arguments = listOf(
                navArgument("runId") { type = NavType.LongType }
            )
        ) { backStack ->
            val runId = requireNotNull(backStack.arguments?.getLong("runId")) {
                "runId es requerido"
            }

            RunScreen(
                runId = runId,
                vm = runsVM,
                onSubmitted = {
                    nav.navigate(Routes.Stores) { popUpTo(0) }
                }
            )
        }
    }
}
