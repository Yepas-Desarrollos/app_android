package mx.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
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
import mx.checklist.data.auth.AuthState
import mx.checklist.data.api.ApiClient
import mx.checklist.ui.AppNavHost
import mx.checklist.ui.navigation.NavRoutes
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.vm.AdminViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(this)
        val repo = Repo(tokenStore = tokenStore)

        setContent {
            MaterialTheme {
                val authVM = viewModel<AuthViewModel>(factory = SimpleFactory { AuthViewModel(repo) })
                val runsVM = viewModel<RunsViewModel>(factory = SimpleFactory { RunsViewModel(repo) })
                val adminVM = viewModel<AdminViewModel>(factory = SimpleFactory { AdminViewModel(repo) })

                // Inicializar AuthState si hay token guardado
                LaunchedEffect(Unit) {
                    // Cargar token guardado desde TokenStore
                    tokenStore.tokenFlow.collect { savedToken ->
                        if (savedToken != null) {
                            AuthState.token = savedToken
                            ApiClient.setToken(savedToken)
                        }
                    }
                }
                
                LaunchedEffect(Unit) {
                    // Cargar roleCode guardado desde TokenStore  
                    tokenStore.roleCodeFlow.collect { savedRole ->
                        AuthState.roleCode = savedRole
                    }
                }

                AppNavHost(
                    authVM = authVM,
                    runsVM = runsVM,
                    adminVM = adminVM
                )
            }
        }
    }
}

class SimpleFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
