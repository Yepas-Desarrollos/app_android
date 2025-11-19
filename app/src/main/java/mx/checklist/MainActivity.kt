package mx.checklist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.AndroidEntryPoint
import mx.checklist.data.TokenStore
import mx.checklist.data.auth.AuthState
import mx.checklist.data.api.ApiClient
import mx.checklist.ui.AppNavHost
import mx.checklist.ui.theme.ChecklistTheme
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.vm.AssignmentViewModel
import mx.checklist.ui.vm.ChecklistStructureViewModel
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar caché de imágenes con límite
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.5)  // 50% del heap
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .maxSizePercent(0.02)  // 2% del storage
                    .directory(File(cacheDir, "image_cache"))
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            ChecklistTheme {
                // ✅ Usar hiltViewModel() - inyección automática de Hilt
                // Esto DEBE estar dentro de setContent (función Composable)
                val authVM: AuthViewModel = hiltViewModel<AuthViewModel>()
                val runsVM: RunsViewModel = hiltViewModel<RunsViewModel>()
                val adminVM: AdminViewModel = hiltViewModel<AdminViewModel>()
                val assignmentVM: AssignmentViewModel = hiltViewModel<AssignmentViewModel>()
                val checklistVM: ChecklistStructureViewModel = hiltViewModel<ChecklistStructureViewModel>()

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
                        adminVM = adminVM,
                        assignmentVM = assignmentVM,
                        checklistVM = checklistVM
                    )
                }
            }
        }
    }
}
