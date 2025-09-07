package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.api.dto.*

class RunsViewModel(private val repo: Repo) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _stores = MutableStateFlow<List<StoreDto>>(emptyList())
    private val _templates = MutableStateFlow<List<TemplateDto>>(emptyList())

    private val _runItems = MutableStateFlow<List<RunItemDto>>(emptyList())
    private val _runItemsLoadedFor = MutableStateFlow<Long?>(null)

    // Info run actual
    private val _runInfo = MutableStateFlow<RunInfoDto?>(null)
    fun runInfoFlow(): StateFlow<RunInfoDto?> = _runInfo

    // Listas
    private val _pendingRuns = MutableStateFlow<List<RunSummaryDto>>(emptyList())
    fun pendingRunsFlow(): StateFlow<List<RunSummaryDto>> = _pendingRuns

    private val _historyRuns = MutableStateFlow<List<RunSummaryDto>>(emptyList())
    fun historyRunsFlow(): StateFlow<List<RunSummaryDto>> = _historyRuns

    fun getStores(): StateFlow<List<StoreDto>> {
        if (_stores.value.isEmpty()) viewModelScope.launch { safe { _stores.value = repo.stores() } }
        return _stores
    }

    fun getTemplates(): StateFlow<List<TemplateDto>> {
        if (_templates.value.isEmpty()) viewModelScope.launch { safe { _templates.value = repo.templates() } }
        return _templates
    }

    fun loadPendingRuns(limit: Int? = 20, all: Boolean? = false, storeCode: String? = null) {
        viewModelScope.launch { safe { _pendingRuns.value = repo.pendingRuns(limit, all, storeCode) } }
    }

    fun loadHistoryRuns(limit: Int? = 20, storeCode: String? = null) {
        viewModelScope.launch { safe { _historyRuns.value = repo.historyRuns(limit, storeCode) } }
    }

    fun runItemsFlow(): StateFlow<List<RunItemDto>> = _runItems

    fun loadRunItems(runId: Long) {
        if (_runItemsLoadedFor.value == runId && _runItems.value.isNotEmpty()) return
        viewModelScope.launch {
            safe {
                _runItems.value = repo.runItems(runId)
                _runItemsLoadedFor.value = runId
            }
        }
    }

    fun loadRunInfo(runId: Long) {
        viewModelScope.launch { safe { _runInfo.value = repo.runInfo(runId) } }
    }

    fun createRun(storeCode: String, templateId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            safe {
                val res = repo.createRun(storeCode, templateId)
                onCreated(res.id)
                _pendingRuns.value = repo.pendingRuns()
            }
        }
    }

    fun respond(itemId: Long, status: String?, text: String?, number: Double?, onUpdated: (RunItemDto) -> Unit = {}) {
        viewModelScope.launch {
            safe {
                val updated = repo.respond(itemId, status, text, number)
                _runItems.value = _runItems.value.map { if (it.id == updated.id) updated else it }
                onUpdated(updated)
            }
        }
    }

    fun submit(runId: Long, onSubmitted: () -> Unit) {
        viewModelScope.launch {
            safe {
                repo.submit(runId)
                onSubmitted()
                _pendingRuns.value = repo.pendingRuns()
                _historyRuns.value = repo.historyRuns()
            }
        }
    }

    fun deleteRun(runId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            safe {
                repo.deleteRun(runId)
                _pendingRuns.value = repo.pendingRuns(all = true)
                onDeleted()
            }
        }
    }

    private suspend inline fun safe(crossinline block: suspend () -> Unit) {
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
