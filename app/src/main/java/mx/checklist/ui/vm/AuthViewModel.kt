package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo

data class LoginState(
    val loading: Boolean = false,
    val error: String? = null
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
                repo.login(email, password)
                _state.value = LoginState()
                onOk()
            } catch (t: Throwable) {
                _state.value = LoginState(error = t.message ?: "Error de login")
            }
        }
    }
}
