package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.api.dto.RunItemDto
import mx.checklist.data.api.dto.StoreDto
import mx.checklist.data.api.dto.TemplateDto

class RunsViewModel(private val repo: Repo) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _stores = MutableStateFlow<List<StoreDto>>(emptyList())
    private val _templates = MutableStateFlow<List<TemplateDto>>(emptyList())

    private val _runItems = MutableStateFlow<List<RunItemDto>>(emptyList())
    private val _runItemsLoadedFor = MutableStateFlow<Long?>(null)

    fun getStores(): StateFlow<List<StoreDto>> {
        if (_stores.value.isEmpty()) {
            viewModelScope.launch { safeLoad { _stores.value = repo.stores() } }
        }
        return _stores
    }

    fun getTemplates(): StateFlow<List<TemplateDto>> {
        if (_templates.value.isEmpty()) {
            viewModelScope.launch { safeLoad { _templates.value = repo.templates() } }
        }
        return _templates
    }

    /** Flow estable para observar en la UI */
    fun runItemsFlow(): StateFlow<List<RunItemDto>> = _runItems

    /** Carga ítems SOLO si cambia el runId o no hay cache */
    fun loadRunItems(runId: Long) {
        if (_runItemsLoadedFor.value == runId && _runItems.value.isNotEmpty()) return
        viewModelScope.launch {
            safeLoad {
                _runItems.value = repo.runItems(runId)
                _runItemsLoadedFor.value = runId
            }
        }
    }

    fun createRun(storeCode: String, templateId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            safeLoad {
                val res = repo.createRun(storeCode, templateId)
                onCreated(res.id)
            }
        }
    }

    fun respond(
        itemId: Long,
        status: String?,
        text: String?,
        number: Double?,
        onUpdated: (RunItemDto) -> Unit = {}
    ) {
        viewModelScope.launch {
            safeLoad {
                val updated = repo.respond(itemId, status, text, number)
                _runItems.value = _runItems.value.map { if (it.id == updated.id) updated else it }
                onUpdated(updated)
            }
        }
    }

    fun submit(runId: Long, onSubmitted: () -> Unit) {
        viewModelScope.launch {
            safeLoad {
                repo.submit(runId)
                onSubmitted()
            }
        }
    }

    private suspend inline fun safeLoad(crossinline block: suspend () -> Unit) {
        try {
            _error.value = null
            _loading.value = true
            block()
        } catch (t: Throwable) {
            _error.value = t.message ?: "Error inesperado"
        } finally {
            _loading.value = false
        }
    }
}
