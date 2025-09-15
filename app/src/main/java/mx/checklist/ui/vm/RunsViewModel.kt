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
    fun updateError(message: String?) { _error.value = message }

    private val _stores = MutableStateFlow<List<StoreDto>>(emptyList())
    private val _templates = MutableStateFlow<List<TemplateDto>>(emptyList())

    private val _runItems = MutableStateFlow<List<RunItemDto>>(emptyList())
    private val _runItemsLoadedFor = MutableStateFlow<Long?>(null)
    fun runItemsFlow(): StateFlow<List<RunItemDto>> = _runItems

    private val _runInfo = MutableStateFlow<RunInfoDto?>(null)
    fun runInfoFlow(): StateFlow<RunInfoDto?> = _runInfo

    private val _pendingRuns = MutableStateFlow<List<RunSummaryDto>>(emptyList())
    fun pendingRunsFlow(): StateFlow<List<RunSummaryDto>> = _pendingRuns

    private val _historyRuns = MutableStateFlow<List<RunSummaryDto>>(emptyList())
    fun historyRunsFlow(): StateFlow<List<RunSummaryDto>> = _historyRuns

    // Se eliminó _itemAttachments y itemAttachments (la lista global de adjuntos)

    private val _evidenceError = MutableStateFlow<String?>(null)
    val evidenceError: StateFlow<String?> = _evidenceError

    fun clearError() { _error.value = null }
    fun clearEvidenceError() { _evidenceError.value = null }

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

    fun loadRunItems(runId: Long) {
        if (_runItemsLoadedFor.value == runId && _runItems.value.isNotEmpty()) return
        viewModelScope.launch {
            safe {
                // Asumimos que repo.runItems(runId) ya incluye los attachments en cada RunItemDto
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
                _pendingRuns.value = repo.pendingRuns() // Refresh pending runs
            }
        }
    }

    fun respond(itemId: Long, status: String?, text: String?, number: Double?, onUpdated: (RunItemDto) -> Unit = {}) {
        viewModelScope.launch {
            val item = _runItems.value.find { it.id == itemId }
            if (item == null) {
                _error.value = "Item no encontrado"
                return@launch
            }

            val evidenceConfig = item.itemTemplate?.config?.get("evidence") as? Map<String, Any?>
            var evidenceRequired = false
            var minCount = 0
            var requiredOnFail = false

            if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
                evidenceRequired = evidenceConfig["required"] as? Boolean ?: false
                minCount = (evidenceConfig["minCount"] as? Number)?.toInt() ?: 0
                requiredOnFail = evidenceConfig["requiredOnFail"] as? Boolean ?: false
            }

            var photosNeeded = 0
            // Usar los adjuntos del item específico desde _runItems
            val currentPhotoCount = item.attachments?.size ?: 0

            if (requiredOnFail && status.equals("FAIL", ignoreCase = true)) {
                photosNeeded = if (minCount > 0) minCount else 1
            } else if (evidenceRequired) {
                photosNeeded = minCount
            }
            
            if (item.itemTemplate?.expectedType.equals("PHOTO", ignoreCase = true) || item.itemTemplate?.expectedType.equals("MULTIPHOTO", ignoreCase = true)) {
                if (evidenceConfig == null && photosNeeded == 0) {
                     photosNeeded = 1
                }
            }

            if (photosNeeded > 0 && currentPhotoCount < photosNeeded) {
                _evidenceError.value = "Se requieren $photosNeeded foto(s) para este ítem (actualmente $currentPhotoCount)."
                return@launch
            }

            _evidenceError.value = null
            safe {
                val updatedItemDto = repo.respond(itemId, status, text, number)
                _runItems.value = _runItems.value.map { currentItemInList ->
                    if (currentItemInList.id == updatedItemDto.id) {
                        // Conservamos los adjuntos que YA TENÍA el currentItemInList (que es el 'item' original)
                        // y actualizamos el resto de los campos con lo que vino de la respuesta del backend.
                        currentItemInList.copy(
                            responseStatus = updatedItemDto.responseStatus,
                            responseText = updatedItemDto.responseText,
                            responseNumber = updatedItemDto.responseNumber,
                            respondedAt = updatedItemDto.respondedAt
                            // Asegúrate de que attachments NO se actualice desde updatedItemDto aquí,
                            // ya que updatedItemDto probablemente no los incluya o los incluya vacíos.
                            // Los attachments se manejan por separado en uploadAttachments/deleteAttachment.
                        )
                    } else {
                        currentItemInList
                    }
                }
                val finalItemToShow = _runItems.value.find { it.id == updatedItemDto.id } ?: updatedItemDto // Debería encontrar el actualizado
                onUpdated(finalItemToShow)
            }
        }
    }

    fun uploadAttachments(itemId: Long, files: List<java.io.File>, onOk: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe {
                repo.uploadAttachments(itemId, files)
                val newAttachments = repo.listAttachments(itemId)
                _runItems.value = _runItems.value.map { runItem ->
                    if (runItem.id == itemId) {
                        runItem.copy(attachments = newAttachments)
                    } else {
                        runItem
                    }
                }
                onOk?.invoke()
            }
        }
    }
    
    // Se eliminó la función loadAttachments(itemId: Long) ya que no se necesitará

    fun deleteAttachment(itemId: Long, attachmentId: Int) {
        viewModelScope.launch {
            safe {
                repo.deleteAttachment(itemId, attachmentId)
                val newAttachments = repo.listAttachments(itemId)
                _runItems.value = _runItems.value.map { runItem ->
                    if (runItem.id == itemId) {
                        runItem.copy(attachments = newAttachments)
                    } else {
                        runItem
                    }
                }
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

    fun deleteRun(runId: Long, onOk: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe {
                repo.deleteRun(runId)
                _pendingRuns.value = repo.pendingRuns()
                _historyRuns.value = repo.historyRuns()
                onOk?.invoke()
            }
        }
    }

    fun canSubmitAll(): Pair<Boolean, String?> {
        val items = _runItems.value
        for (it in items) { // 'it' aquí es RunItemDto
            val tpl = it.itemTemplate
            val cfg = tpl?.config ?: emptyMap<String, Any>()
            // Usar los adjuntos directamente del RunItemDto 'it'
            val attachments = it.attachments ?: emptyList()

            fun cfgInt(key: String, map: Map<String, Any?>? = cfg): Int? = (map?.get(key) as? Number)?.toInt()
            fun cfgBool(key: String, map: Map<String, Any?>? = cfg): Boolean = (map?.get(key) as? Boolean) == true
            
            val evidenceConfig = cfg["evidence"] as? Map<String, Any?>

            if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
                val required = cfgBool("required", evidenceConfig)
                val minCount = cfgInt("minCount", evidenceConfig) ?: 0
                val requiredOnFail = cfgBool("requiredOnFail", evidenceConfig)

                var photosActuallyNeeded = 0
                if (requiredOnFail && it.responseStatus.equals("FAIL", ignoreCase = true)) {
                    photosActuallyNeeded = if (minCount > 0) minCount else 1
                } else if (required) {
                    photosActuallyNeeded = minCount
                }
                
                if (attachments.size < photosActuallyNeeded) {
                    return false to "Ítem #${it.orderIndex} '${tpl?.title}': requiere $photosActuallyNeeded foto(s)."
                }
            } else if (tpl?.expectedType.equals("PHOTO", ignoreCase = true) || tpl?.expectedType.equals("MULTIPHOTO", ignoreCase = true)) {
                 if (attachments.isEmpty()) {
                    return false to "Ítem #${it.orderIndex} '${tpl?.title}': requiere al menos 1 foto."
                }
            }

            when (tpl?.expectedType?.uppercase()) {
                "CHOICE" -> {
                    val s = it.responseStatus.orEmpty()
                    if (s.isBlank()) return false to "Falta estatus en ítem #${it.orderIndex} '${tpl.title}'"
                }
                // PHOTO/MULTIPHOTO handled above
                "NUMERIC" -> {
                    // Validation logic can be added here
                }
            }
        }
        return true to null
    }

    private suspend inline fun safe(crossinline block: suspend () -> Unit) {
        try {
            _error.value = null 
            _loading.value = true
            block()
        } catch (t: Throwable) {
            _error.value = t.message ?: "Error inesperado"
            t.printStackTrace() // Helpful for debugging
        } finally {
            _loading.value = false
        }
    }
}
