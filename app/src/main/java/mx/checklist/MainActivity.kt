package mx.checklist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.checklist.data.Repo
import mx.checklist.data.TokenStore
import mx.checklist.data.auth.AuthState
import mx.checklist.data.api.ApiClient
import mx.checklist.ui.AppNavHost
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
                        Log.d("MainActivity", "🔑 TokenStore.tokenFlow: $savedToken")
                        if (savedToken != null) {
                            AuthState.token = savedToken
                            ApiClient.setToken(savedToken)
                        }
                    }
                }
                
                LaunchedEffect(Unit) {
                    // Cargar roleCode guardado desde TokenStore  
                    tokenStore.roleCodeFlow.collect { savedRole ->
                        Log.d("MainActivity", "👤 TokenStore.roleCodeFlow: $savedRole")
                        AuthState.roleCode = savedRole
                        Log.d("MainActivity", "👤 AuthState.roleCode actualizado a: ${AuthState.roleCode}")
                    }
                }

                // Observar el estado de auth para recomposición
                val authState by authVM.state.collectAsStateWithLifecycle()

                // Usar key() para forzar recomposición cuando cambie roleCode
                key(authState.authenticated?.roleCode) {
                    AppNavHost(
                        authVM = authVM,
                        runsVM = runsVM,
                        adminVM = adminVM
                    )
                }
            }
        }
    }
}

class SimpleFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
