package mx.checklist.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo

data class LoginState(val loading:Boolean=false, val error:String?=null)
class LoginVM(private val repo: Repo): ViewModel() {
    private val _st = MutableStateFlow(LoginState()); val st: StateFlow<LoginState> = _st
    fun login(email:String, pass:String, onOk:()->Unit){
        viewModelScope.launch {
            try { _st.value = LoginState(true); repo.login(email, pass); _st.value = LoginState(); onOk() }
            catch (e:Exception){ _st.value = LoginState(error = e.message ?: "Error de login") }
        }
    }
}


