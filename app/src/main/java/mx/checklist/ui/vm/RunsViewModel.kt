package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.api.dto.*
import mx.checklist.data.repo.RunsRepository

class RunsViewModel : ViewModel() {
    private val repo = RunsRepository()

    private val _stores = MutableStateFlow<List<StoreDto>>(emptyList())
    val stores = _stores.asStateFlow()

    private val _templates = MutableStateFlow<List<TemplateDto>>(emptyList())
    val templates = _templates.asStateFlow()

    private val _items = MutableStateFlow<List<RunItemDto>>(emptyList())
    val items = _items.asStateFlow()

    fun loadStores() = viewModelScope.launch {
        _stores.value = repo.getStores()
    }

    fun loadTemplates() = viewModelScope.launch {
        _templates.value = repo.getTemplates()
    }

    fun createRun(storeCode: String, templateId: Long, onCreated: (Long) -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repo.createRun(storeCode, templateId)
                onCreated(res.id)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun loadRunItems(runId: Long) = viewModelScope.launch {
        _items.value = repo.getRunItems(runId)
    }

    fun respond(itemId: Long, status: String) = viewModelScope.launch {
        repo.respondItem(itemId, status)
        // refrescar localmente
        _items.value = _items.value.map { if (it.id == itemId) it.copy(responseStatus = status) else it }
    }

    fun submit(runId: Long, onOk: () -> Unit, onError: (Throwable) -> Unit) = viewModelScope.launch {
        try {
            repo.submitRun(runId)
            onOk()
        } catch (t: Throwable) {
            onError(t)
        }
    }
}
