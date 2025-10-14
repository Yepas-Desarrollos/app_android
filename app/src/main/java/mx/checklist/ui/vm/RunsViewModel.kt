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

    private val _evidenceError = MutableStateFlow<String?>(null)
    val evidenceError: StateFlow<String?> = _evidenceError

    // Estado específico para uploads de imágenes
    private val _uploadingImages = MutableStateFlow<Set<Long>>(emptySet())
    val uploadingImages: StateFlow<Set<Long>> = _uploadingImages

    fun clearError() { _error.value = null }
    fun clearEvidenceError() { _evidenceError.value = null }

    // Método para limpiar cache cuando cambia el usuario
    fun clearCache() {
        _stores.value = emptyList()
        _templates.value = emptyList()
        _runItems.value = emptyList()
        _runItemsLoadedFor.value = null
        _runInfo.value = null
        _pendingRuns.value = emptyList()
        _historyRuns.value = emptyList()
        _error.value = null
        _evidenceError.value = null
        _uploadingImages.value = emptySet()
    }

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

    fun respond(itemId: Long, status: String?, text: String?, number: Double?, barcode: String? = null, onUpdated: (RunItemDto) -> Unit = {}) {
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
            val currentPhotoCount = item.attachments?.size ?: 0

            if (requiredOnFail && status.equals("FAIL", ignoreCase = true)) {
                photosNeeded = if (minCount > 0) minCount else 1
            } else if (evidenceRequired) {
                photosNeeded = minCount
            }

            if (item.itemTemplate?.expectedType.equals("PHOTO", ignoreCase = true) ||
                item.itemTemplate?.expectedType.equals("MULTIPHOTO", ignoreCase = true)) {
                if (evidenceConfig == null && photosNeeded == 0) {
                     photosNeeded = 1
                }
            }

            if (photosNeeded > 0 && currentPhotoCount < photosNeeded) {
                _evidenceError.value = "Se requieren $photosNeeded foto(s) para este ítem (actualmente $currentPhotoCount)."
                return@launch
            }

            // Validación de status solo OK/FAIL para BOOLEAN
            if (item.itemTemplate?.expectedType.equals("BOOLEAN", ignoreCase = true)) {
                if (status != "OK" && status != "FAIL") {
                    _error.value = "Solo se permite OK o FAIL para este campo."
                    return@launch
                }
            }

            // Validación por tipo
            // (puedes agregar más validaciones según expectedType y config)

            //  CORREGIDO: Ahora se pasa el parámetro barcode correctamente al repositorio
            safe {
                val updatedItemDto = repo.respond(itemId, status, text, number, barcode)
                _runItems.value = _runItems.value.map { currentItemInList ->
                    if (currentItemInList.id == updatedItemDto.id) {
                        currentItemInList.copy(
                            responseStatus = updatedItemDto.responseStatus,
                            responseText = updatedItemDto.responseText,
                            responseNumber = updatedItemDto.responseNumber,
                            scannedBarcode = updatedItemDto.scannedBarcode,
                            respondedAt = updatedItemDto.respondedAt
                        )
                    } else {
                        currentItemInList
                    }
                }
                val finalItemToShow = _runItems.value.find { it.id == updatedItemDto.id } ?: updatedItemDto
                onUpdated(finalItemToShow)
            }
        }
    }

    fun uploadAttachment(itemId: Long, file: java.io.File, localUri: String) {
        viewModelScope.launch {
            // Marcar que este item está subiendo una imagen
            _uploadingImages.value = _uploadingImages.value + itemId

            val tempId = -System.currentTimeMillis().toInt()
            val tempAttachment = AttachmentDto(
                id = tempId,
                type = "PHOTO",
                url = "",
                createdAt = "",
                localUri = localUri
            )

            // 1. Actualización optimista: añadir el adjunto temporal a la UI
            _runItems.value = _runItems.value.map { runItem ->
                if (runItem.id == itemId) {
                    runItem.copy(attachments = runItem.attachments.orEmpty() + tempAttachment)
                } else {
                    runItem
                }
            }

            // 2. Subir el archivo en segundo plano con reintentos
            var retryCount = 0
            val maxRetries = 3
            var uploadSuccess = false

            while (retryCount < maxRetries && !uploadSuccess) {
                try {
                    repo.uploadAttachments(itemId, listOf(file))
                    uploadSuccess = true

                    // 3. Al tener éxito, refrescar la lista desde el servidor
                    val newAttachments = repo.listAttachments(itemId)

                    // ✅ MEJORADO: Preservar el localUri en el attachment más reciente para transición suave
                    val updatedAttachments = newAttachments.map { serverAtt ->
                        // Si este es el attachment más reciente (último en la lista), preservar el localUri
                        if (serverAtt == newAttachments.lastOrNull()) {
                            serverAtt.copy(localUri = localUri)
                        } else {
                            serverAtt
                        }
                    }

                    _runItems.value = _runItems.value.map { runItem ->
                        if (runItem.id == itemId) {
                            runItem.copy(attachments = updatedAttachments)
                        } else {
                            runItem
                        }
                    }

                    // Limpiar cualquier error previo
                    _evidenceError.value = null

                } catch (e: Exception) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        // Esperar antes del siguiente intento (backoff exponencial)
                        kotlinx.coroutines.delay(2000L * retryCount)
                    } else {
                        // 4. Si falla después de todos los reintentos, eliminar el adjunto temporal
                        _runItems.value = _runItems.value.map { runItem ->
                            if (runItem.id == itemId) {
                                runItem.copy(attachments = runItem.attachments.orEmpty().filter { it.id != tempId })
                            } else {
                                runItem
                            }
                        }

                        // Determinar el tipo de error y mostrar mensaje específico
                        val errorMessage = when {
                            e.message?.contains("timeout", ignoreCase = true) == true ->
                                "Error de conexión: La subida tomó demasiado tiempo. Verifica tu conexión e intenta de nuevo."
                            e.message?.contains("network", ignoreCase = true) == true ->
                                "Error de red: Verifica tu conexión a internet."
                            e.message?.contains("413", ignoreCase = true) == true ->
                                "La imagen es demasiado grande. Intenta con una imagen más pequeña."
                            else -> "Error al subir la imagen: ${e.message}. Intenta de nuevo."
                        }
                        _evidenceError.value = errorMessage
                    }
                }
            }

            // Remover el item del estado de carga
            _uploadingImages.value = _uploadingImages.value - itemId
        }
    }

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
        for (item in items) {
            val tpl = item.itemTemplate
            val cfg = tpl?.config ?: emptyMap<String, Any>()
            val attachments = item.attachments ?: emptyList()

            fun cfgInt(key: String, map: Map<String, Any?>? = cfg): Int? = (map?.get(key) as? Number)?.toInt()
            fun cfgBool(key: String, map: Map<String, Any?>? = cfg): Boolean = (map?.get(key) as? Boolean) == true

            val evidenceConfig = cfg["evidence"] as? Map<String, Any?>

            if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
                val required = cfgBool("required", evidenceConfig)
                val minCount = cfgInt("minCount", evidenceConfig) ?: 0
                val requiredOnFail = cfgBool("requiredOnFail", evidenceConfig)

                var photosActuallyNeeded = 0
                if (requiredOnFail && item.responseStatus.equals("FAIL", ignoreCase = true)) {
                    photosActuallyNeeded = if (minCount > 0) minCount else 1
                } else if (required) {
                    photosActuallyNeeded = minCount
                }

                if (attachments.size < photosActuallyNeeded) {
                    return false to "Ítem #${item.orderIndex} '${tpl?.title}': requiere $photosActuallyNeeded foto(s)."
                }
            } else if (tpl?.expectedType.equals("PHOTO", ignoreCase = true) ||
                       tpl?.expectedType.equals("MULTIPHOTO", ignoreCase = true)) {
                 if (attachments.isEmpty()) {
                    return false to "Ítem #${item.orderIndex} '${tpl?.title}': requiere al menos 1 foto."
                }
            }

            when (tpl?.expectedType?.uppercase()) {
                "CHOICE" -> {
                    val s = item.responseStatus.orEmpty()
                    if (s.isBlank()) return false to "Falta estatus en ítem #${item.orderIndex} '${tpl.title}'"
                }
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
            t.printStackTrace()
        } finally {
            _loading.value = false
        }
    }
}
