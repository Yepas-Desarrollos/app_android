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
import mx.checklist.ui.screens.HomeScreen
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

        val tokenStore = TokenStore(this)
        val repo = Repo(tokenStore = tokenStore)

        setContent {
            MaterialTheme {
                val nav = rememberNavController()

                val authVM = androidx.lifecycle.viewmodel.compose.viewModel<AuthViewModel>(
                    factory = SimpleFactory { AuthViewModel(repo) }
                )
                val runsVM = androidx.lifecycle.viewmodel.compose.viewModel<RunsViewModel>(
                    factory = SimpleFactory { RunsViewModel(repo) }
                )

                // Iniciamos en Login; tras login vamos a Home
                NavHost(navController = nav, startDestination = Routes.Login) {

                    // LOGIN → HOME
                    composable(Routes.Login) {
                        LoginScreen(
                            vm = authVM,
                            onLoggedIn = { nav.navigate(Routes.Home) { popUpTo(0) } }
                        )
                    }

                    // HOME
                    composable(Routes.Home) {
                        HomeScreen(
                            onNuevaCorrida = { nav.navigate(Routes.Stores) },
                            onVerTiendas = { nav.navigate(Routes.Stores) }
                        )
                    }

                    // STORES
                    composable(Routes.Stores) {
                        StoresScreen(
                            vm = runsVM,
                            onStoreSelected = { storeCode ->
                                nav.navigate(Routes.template(storeCode))
                            }
                        )
                    }

                    // TEMPLATES (storeCode como path)
                    composable(
                        route = Routes.Templates,
                        arguments = listOf(
                            navArgument("storeCode") { type = NavType.StringType }
                        )
                    ) { backStack ->
                        val storeCode = requireNotNull(
                            backStack.arguments?.getString("storeCode")
                        ) { "storeCode es requerido" }

                        TemplatesScreen(
                            storeCode = storeCode,
                            vm = runsVM,
                            onRunCreated = { runId, sc ->
                                nav.navigate(Routes.items(runId, sc))
                            }
                        )
                    }

                    // ITEMS (runId path + storeCode query)
                    composable(
                        route = Routes.Items,
                        arguments = listOf(
                            navArgument("runId") { type = NavType.LongType },
                            navArgument("storeCode") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) { backStack ->
                        val runId = requireNotNull(
                            backStack.arguments?.getLong("runId")
                        ) { "runId es requerido" }
                        val storeCode = backStack.arguments?.getString("storeCode")
                            ?: error("storeCode es requerido")

                        ItemsScreen(
                            runId = runId,
                            storeCode = storeCode,
                            vm = runsVM,
                            onSubmit = {
                                // Tras enviar checklist, ir a HOME
                                nav.navigate(Routes.Home) { popUpTo(0) }
                            },
                            readOnly = false // pon true si entras a un run SUBMITTED
                        )
                    }

                    // (Opcional) detalle de corrida por ID directo
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
                                nav.navigate(Routes.Home) { popUpTo(0) }
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Factory simple para VMs con dependencias */
class SimpleFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
