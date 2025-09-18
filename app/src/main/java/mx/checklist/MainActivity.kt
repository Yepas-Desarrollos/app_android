package mx.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.checklist.data.Repo
import mx.checklist.data.TokenStore
import mx.checklist.ui.Routes
import mx.checklist.ui.screens.*
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
                val authVM = viewModel<AuthViewModel>(factory = SimpleFactory { AuthViewModel(repo) })
                val runsVM = viewModel<RunsViewModel>(factory = SimpleFactory { RunsViewModel(repo) })

                NavHost(navController = nav, startDestination = Routes.Login) {

                    composable(Routes.Login) {
                        LoginScreen(vm = authVM, onLoggedIn = { nav.navigate(Routes.Home) { popUpTo(0) } })
                    }

                    composable(Routes.Home) {
                        HomeScreen(
                            vm = runsVM,
                            authVM = authVM,
                            onNuevaCorrida = { nav.navigate(Routes.Stores) },
                            onOpenHistory = { nav.navigate(Routes.History) },
                            onLogout = { nav.navigate(Routes.Login) { popUpTo(0) } }
                        )
                    }

                    composable(Routes.History) {
                        HistoryScreen(
                            vm = runsVM,
                            onOpenRun = { runId, storeCode, templateName ->
                                nav.navigate(Routes.items(runId, storeCode, templateName))
                            }
                        )
                    }

                    composable(Routes.Stores) {
                        StoresScreen(vm = runsVM, onStoreSelected = { storeCode ->
                            nav.navigate(Routes.template(storeCode))
                        })
                    }

                    composable(
                        route = Routes.Templates,
                        arguments = listOf(navArgument("storeCode") { type = NavType.StringType })
                    ) { backStack ->
                        val storeCode = requireNotNull(backStack.arguments?.getString("storeCode")) { "storeCode es requerido" }
                        TemplatesScreen(
                            storeCode = storeCode,
                            vm = runsVM,
                            onRunCreated = { runId, sc -> nav.navigate(Routes.items(runId, sc)) }
                        )
                    }

                    composable(
                        route = Routes.Items,
                        arguments = listOf(
                            navArgument("runId") { type = NavType.LongType },
                            navArgument("storeCode") { type = NavType.StringType; nullable = true },
                            navArgument("templateName") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStack ->
                        val runId = requireNotNull(backStack.arguments?.getLong("runId")) { "runId es requerido" }
                        val storeCode = backStack.arguments?.getString("storeCode") ?: error("storeCode es requerido")
                        val templateName = backStack.arguments?.getString("templateName")

                        ItemsScreen(
                            runId = runId,
                            storeCode = storeCode,
                            vm = runsVM,
                            onSubmit = { nav.navigate(Routes.Home) { popUpTo(0) } },
                            readOnly = false,
                            templateName = templateName
                        )
                    }
                }
            }
        }
    }
}

class SimpleFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
