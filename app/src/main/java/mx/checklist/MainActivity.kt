package mx.checklist
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import mx.checklist.data.Repo
import mx.checklist.data.TokenStore
import mx.checklist.data.api.ApiClient
import mx.checklist.ui.*
import mx.checklist.ui.screens.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(this)
        val repo = Repo(ApiClient.api, tokenStore)
        lifecycleScope.launch { tokenStore.tokenFlow.collect { ApiClient.setToken(it) } }

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val loginVM = androidx.lifecycle.viewmodel.compose.viewModel<LoginVM>(factory = SimpleFactory { LoginVM(repo) })
                val storesVM = androidx.lifecycle.viewmodel.compose.viewModel<StoresVM>(factory = SimpleFactory { StoresVM(repo) })
                val tmplVM = androidx.lifecycle.viewmodel.compose.viewModel<TemplatesVM>(factory = SimpleFactory { TemplatesVM(repo) })
                val itemsVM = androidx.lifecycle.viewmodel.compose.viewModel<ItemsVM>(factory = SimpleFactory { ItemsVM(repo) })

                NavHost(navController = nav, startDestination = Route.Login.r) {
                    composable(Route.Login.r) { 
                        LoginScreen(
                            onLogged = { 
                                nav.navigate(Route.Stores.r) { 
                                    popUpTo(Route.Login.r){ inclusive=true } 
                                }
                            },
                            vm = loginVM
                        )
                    }
                    composable(Route.Stores.r) {
                        StoresScreen(
                            vm = storesVM,
                            onStoreSelected = { storeCode ->
                                nav.navigate(Route.Templates.path(storeCode))
                            }
                        )
                    }
                    composable(
                        route = Route.Templates.r,
                        arguments = listOf(navArgument("storeCode") { defaultValue = "" })
                    ) { backStackEntry ->
                        val storeCode = backStackEntry.arguments?.getString("storeCode") ?: ""
                        TemplatesScreen(
                            storeCode = storeCode,
                            vm = tmplVM,
                            onTemplateSelected = { runId, sc ->
                                nav.navigate(Route.Items.path(runId, sc))
                            }
                        )
                    }
                    composable(
                        route = Route.Items.r,
                        arguments = listOf(
                            navArgument("runId") { type = NavType.LongType },
                            navArgument("storeCode") { defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val runId = backStackEntry.arguments?.getLong("runId") ?: 0L
                        val storeCode = backStackEntry.arguments?.getString("storeCode") ?: ""
                        ItemsScreen(
                            runId = runId,
                            storeCode = storeCode,
                            vm = itemsVM,
                            onFinished = {
                                nav.popBackStack(Route.Stores.r, false)
                            }
                        )
                    }
                }
            }
        }
    }
}
class SimpleFactory<T: ViewModel>(val creator: ()->T): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T1 : ViewModel> create(modelClass: Class<T1>): T1 {
        if (modelClass.isAssignableFrom(creator()::class.java)) {
            return creator() as T1
        }
        throw IllegalArgumentException("Unknown model class " + modelClass.name)
    }
}

