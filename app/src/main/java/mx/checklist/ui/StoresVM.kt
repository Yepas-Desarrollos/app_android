package mx.checklist.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.api.StoreDto

class StoresVM(private val repo: Repo): ViewModel() {
    private val _items = MutableStateFlow<List<StoreDto>>(emptyList())
    val items: StateFlow<List<StoreDto>> = _items
    fun load(){ viewModelScope.launch { _items.value = repo.stores() } }
}


