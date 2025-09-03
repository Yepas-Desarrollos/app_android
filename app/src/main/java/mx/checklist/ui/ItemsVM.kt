package mx.checklist.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.api.ItemDto
import mx.checklist.data.api.RespondReq

class ItemsVM(private val repo: Repo): ViewModel() {
    private val _items = MutableStateFlow<List<ItemDto>>(emptyList())
    val items: StateFlow<List<ItemDto>> = _items
    fun load(runId:Long){ viewModelScope.launch { _items.value = repo.runItems(runId) } }
    fun respond(itemId:Long, body:RespondReq, onDone:()->Unit = {}){ viewModelScope.launch { repo.respond(itemId, body); onDone() } }
    fun submit(runId:Long, onDone:()->Unit){ viewModelScope.launch { repo.submitRun(runId); onDone() } }
}
