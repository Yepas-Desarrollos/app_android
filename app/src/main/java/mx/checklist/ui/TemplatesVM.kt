package mx.checklist.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.api.TemplateDto

class TemplatesVM(private val repo: Repo): ViewModel() {
    private val _items = MutableStateFlow<List<TemplateDto>>(emptyList())
    val items: StateFlow<List<TemplateDto>> = _items
    fun load(){ viewModelScope.launch { _items.value = repo.templates() } }
    suspend fun createRun(storeCode:String, templateId:Long) = repo.createRun(storeCode, templateId)
}
