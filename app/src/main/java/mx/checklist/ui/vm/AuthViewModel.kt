package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.checklist.data.repo.AuthRepository

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.login(email, password)
                onSuccess()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }
}
