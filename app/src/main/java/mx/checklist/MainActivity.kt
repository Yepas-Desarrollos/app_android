package mx.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.checklist.data.Repo
import mx.checklist.data.TokenStore
import mx.checklist.ui.Routes
import mx.checklist.ui.screens.ItemsScreen
import mx.checklist.ui.screens.LoginScreen
import mx.checklist.ui.screens.RunScreen
import mx.checklist.ui.screens.StoresScreen
import mx.checklist.ui.screens.TemplatesScreen
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dependencias básicas
        val tokenStore = TokenStore(this)
        val repo = Repo(tokenStore = tokenStore)

        setContent {
            MaterialTheme {
                val nav = rememberNavController()

                // ViewModels: SOLO 2 (AuthViewModel y RunsViewModel)
                val authVM = androidx.lifecycle.viewmodel.compose.viewModel<AuthViewModel>(
                    factory = SimpleFactory { AuthViewModel(repo) }
                )
                val runsVM = androidx.lifecycle.viewmodel.compose.viewModel<RunsViewModel>(
                    factory = SimpleFactory { RunsViewModel(repo) }
                )

                NavHost(
                    navController = nav,
                    startDestination = Routes.Login
                ) {
                    // LOGIN
                    composable(Routes.Login) {
                        LoginScreen(vm = authVM) {
                            nav.navigate(Routes.Stores) { popUpTo(0) }
                        }
                    }

                    // STORES
                    composable(Routes.Stores) {
                        StoresScreen(vm = runsVM) { storeCode ->
                            nav.navigate(Routes.template(storeCode))
                        }
                    }

                    // TEMPLATES
                    composable(
                        route = Routes.Templates,
                        arguments = listOf(
                            navArgument("storeCode") { type = NavType.StringType }
                        )
                    ) { backStack ->
                        val storeCode = backStack.arguments?.getString("storeCode")!!
                        TemplatesScreen(
                            storeCode = storeCode,
                            vm = runsVM
                        ) { runId, sc ->
                            nav.navigate(Routes.items(runId, sc))
                        }
                    }

                    // ITEMS (o detalle de la corrida)
                    composable(
                        route = Routes.Items,
                        arguments = listOf(
                            navArgument("runId") { type = NavType.LongType },
                            navArgument("storeCode") { type = NavType.StringType }
                        )
                    ) { backStack ->
                        val runId = backStack.arguments?.getLong("runId")!!
                        val storeCode = backStack.arguments?.getString("storeCode")!!
                        ItemsScreen(
                            runId = runId,
                            storeCode = storeCode,
                            vm = runsVM
                        ) {
                            nav.navigate(Routes.Stores) { popUpTo(0) }
                        }
                    }

                    // (Opcional) Si deseas navegar a la pantalla de envío final por ID:
                    composable(
                        route = "run/{runId}",
                        arguments = listOf(navArgument("runId") { type = NavType.LongType })
                    ) { backStack ->
                        val runId = backStack.arguments?.getLong("runId")!!
                        RunScreen(
                            runId = runId,
                            vm = runsVM
                        ) {
                            nav.navigate(Routes.Stores) { popUpTo(0) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Factory simple para VMs con constructor con dependencias.
 * Nota: sin genérico a nivel de clase para evitar el type mismatch
 * con el parámetro 'factory' de viewModel() en Compose.
 */
class SimpleFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
