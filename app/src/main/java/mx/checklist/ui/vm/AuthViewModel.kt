package mx.checklist.ui.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.auth.Authenticated
import mx.checklist.data.auth.AuthState
import mx.checklist.data.api.ApiClient
import javax.inject.Inject

data class LoginState(
    val loading: Boolean = false,
    val error: String? = null,
    val authenticated: Authenticated? = null,
    val welcomeMessage: String? = null
)

/**
 * ViewModel de Autenticación (sustituye al previo LoginVM).
 * Mantén este nombre de clase para que coincida con MainActivity.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    init {
        // Validar token guardado al inicializar
        validateSavedToken()
    }

    private fun validateSavedToken() {
        viewModelScope.launch {
            try {
                // Si hay un token guardado, intentar validarlo
                if (AuthState.token != null && AuthState.roleCode != null) {
                    Log.d("AuthViewModel", "🔍 Validando token guardado...")
                    
                    // Hacer una llamada simple para validar el token
                    val stores = repo.stores() // Esta llamada requiere autenticación
                    
                    // Si llegamos aquí, el token es válido
                    val authenticated = Authenticated(
                        token = AuthState.token!!,
                        roleCode = AuthState.roleCode!!
                    )
                    _state.value = LoginState(authenticated = authenticated)
                    Log.d("AuthViewModel", "✅ Token válido - usuario autenticado automáticamente")
                    
                } else {
                    Log.d("AuthViewModel", "🚪 No hay token guardado - requiere login")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Token inválido o expirado: ${e.message}")
                // Token inválido, limpiar datos guardados
                logout(onComplete = {})
            }
        }
    }

    fun login(email: String, password: String, onOk: () -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = LoginState(loading = true)
                val auth = repo.login(email, password)
                
                Log.d("AuthViewModel", "🔐 Login exitoso - token: ${auth.token?.take(20)}...")
                Log.d("AuthViewModel", "🔐 Login exitoso - roleCode: ${auth.roleCode}")
                Log.d("AuthViewModel", "🔐 Login exitoso - fullName: ${auth.fullName}")

                // Actualizar AuthState global
                AuthState.token = auth.token
                AuthState.roleCode = auth.roleCode
                
                Log.d("AuthViewModel", "🔐 AuthState actualizado - roleCode: ${AuthState.roleCode}")
                
                // IMPORTANTE: Sincronizar token con ApiClient
                ApiClient.setToken(auth.token)
                
                // Crear mensaje de bienvenida usando el nombre formateado
                val displayName = auth.getDisplayName()
                val welcomeMessage = "¡Bienvenido, $displayName!"

                Log.d("AuthViewModel", "👋 Mensaje de bienvenida: $welcomeMessage")

                _state.value = LoginState(authenticated = auth, welcomeMessage = welcomeMessage)

                // Navegar inmediatamente a Home donde se mostrará el mensaje
                onOk()
            } catch (t: Throwable) {
                Log.d("AuthViewModel", "❌ Error en login: ${t.message}")
                _state.value = LoginState(error = t.message ?: "Error de login")
            }
        }
    }

    /**
     * Limpiar el mensaje de bienvenida
     */
    fun clearWelcomeMessage() {
        _state.value = _state.value.copy(welcomeMessage = null)
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.logout()
                
                // Limpiar AuthState global
                AuthState.token = null
                AuthState.roleCode = null
                
                // IMPORTANTE: Limpiar token en ApiClient
                ApiClient.setToken(null)
                
                _state.value = LoginState()
                onComplete()
            } catch (t: Throwable) {
                // Log error but still clear state
                AuthState.token = null
                AuthState.roleCode = null
                ApiClient.setToken(null)
                _state.value = LoginState()
                onComplete()
            }
        }
    }
}
