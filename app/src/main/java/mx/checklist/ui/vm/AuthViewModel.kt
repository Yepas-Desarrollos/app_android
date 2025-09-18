package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.auth.Authenticated

data class LoginState(
    val loading: Boolean = false,
    val error: String? = null,
    val authenticated: Authenticated? = null
)

/**
 * ViewModel de Autenticación (sustituye al previo LoginVM).
 * Mantén este nombre de clase para que coincida con MainActivity.
 */
class AuthViewModel(private val repo: Repo) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    fun login(email: String, password: String, onOk: () -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = LoginState(loading = true)
                val auth = repo.login(email, password)
                _state.value = LoginState(authenticated = auth)
                onOk()
            } catch (t: Throwable) {
                _state.value = LoginState(error = t.message ?: "Error de login")
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.logout()
                _state.value = LoginState()
                onComplete()
            } catch (t: Throwable) {
                // Log error but still clear state
                _state.value = LoginState()
                onComplete()
            }
        }
    }
}
