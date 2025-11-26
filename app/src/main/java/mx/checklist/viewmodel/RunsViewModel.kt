package mx.checklist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.repository.RunRepository
import mx.checklist.data.repository.ChecklistRepository
import mx.checklist.data.api.dto.*
import javax.inject.Inject

@HiltViewModel
class RunsViewModel @Inject constructor(
    private val runRepo: RunRepository,
    private val checklistRepo: ChecklistRepository
) : ViewModel() {

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

    // NUEVO: Paginación para historial
    private val _historyPagination = MutableStateFlow(PaginationInfo())
    val historyPagination: StateFlow<PaginationInfo> = _historyPagination

    private val _totalEnviados = MutableStateFlow(0)
    val totalEnviados: StateFlow<Int> = _totalEnviados

    private val _loadingMoreHistory = MutableStateFlow(false)
    val loadingMoreHistory: StateFlow<Boolean> = _loadingMoreHistory

    private val _evidenceError = MutableStateFlow<String?>(null)
    val evidenceError: StateFlow<String?> = _evidenceError

    // Estado específico para uploads de imágenes
    private val _uploadingImages = MutableStateFlow<Set<Long>>(emptySet())
    val uploadingImages: StateFlow<Set<Long>> = _uploadingImages

    // NUEVO: borradores por itemId para no perder respuestas locales cuando falta evidencia
    data class DraftResponse(
        val status: String? = null,
        val text: String? = null,
        val number: Double? = null
    )
    private val _drafts = MutableStateFlow<Map<Long, DraftResponse>>(emptyMap())
    val drafts: StateFlow<Map<Long, DraftResponse>> = _drafts

    fun setDraft(itemId: Long, status: String?, text: String?, number: Double?) {
        _drafts.value = _drafts.value.toMutableMap().apply {
            this[itemId] = DraftResponse(status, text, number)
        }
    }
    fun clearDraft(itemId: Long) {
        if (_drafts.value.containsKey(itemId)) {
            _drafts.value = _drafts.value.toMutableMap().apply { remove(itemId) }
        }
    }

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
        _historyPagination.value = PaginationInfo()
        _totalEnviados.value = 0
        _loadingMoreHistory.value = false
        _error.value = null
        _evidenceError.value = null
        _uploadingImages.value = emptySet()
        // Limpiar borradores
        _drafts.value = emptyMap()
    }

    fun getStores(): StateFlow<List<StoreDto>> {
        if (_stores.value.isEmpty()) viewModelScope.launch { safe { _stores.value = checklistRepo.getStores() } }
        return _stores
    }

    fun getTemplates(): StateFlow<List<TemplateDto>> {
        if (_templates.value.isEmpty()) viewModelScope.launch { safe { _templates.value = checklistRepo.getTemplates() } }
        return _templates
    }

    fun loadPendingRuns(limit: Int? = 20, all: Boolean? = false, storeCode: String? = null) {
        viewModelScope.launch { safe { _pendingRuns.value = runRepo.getPendingRuns(limit ?: 20) } }
    }

    fun loadHistoryRuns(limit: Int? = 20, storeCode: String? = null) {
        viewModelScope.launch { safe { _historyRuns.value = runRepo.getHistoryRuns(limit ?: 20) } }
    }

    // NUEVO: Cargar historial con paginación (primera página)
    fun loadHistoryRunsPaginated(limit: Int = 50) {
        viewModelScope.launch {
            safe {
                val response = runRepo.getHistoryRunsPaginated(page = 1, limit = limit)
                _historyRuns.value = response.data
                _historyPagination.value = PaginationInfo(
                    page = response.pagination.page,
                    limit = response.pagination.limit,
                    total = response.pagination.total,
                    totalPages = response.pagination.totalPages,
                    hasMore = response.pagination.hasMore
                )
                // Asignar el total de enviados
                _totalEnviados.value = response.pagination.total
            }
        }
    }

    // NUEVO: Cargar más páginas del historial
    fun loadMoreHistory() {
        val currentPagination = _historyPagination.value

        // Si ya estamos cargando o no hay más, no hacer nada
        if (_loadingMoreHistory.value || !currentPagination.hasMore) return

        viewModelScope.launch {
            _loadingMoreHistory.value = true
            try {
                val response = runRepo.getHistoryRunsPaginated(
                    page = currentPagination.page + 1,
                    limit = currentPagination.limit
                )

                // Agregar los nuevos datos a la lista existente
                _historyRuns.value = _historyRuns.value + response.data

                // Actualizar la paginación
                _historyPagination.value = PaginationInfo(
                    page = response.pagination.page,
                    limit = response.pagination.limit,
                    total = response.pagination.total,
                    totalPages = response.pagination.totalPages,
                    hasMore = response.pagination.hasMore
                )
                // Mantener el total actualizado
                _totalEnviados.value = response.pagination.total
            } catch (e: Exception) {
                _error.value = "Error al cargar más resultados: ${e.message}"
            } finally {
                _loadingMoreHistory.value = false
            }
        }
    }

    fun loadRunItems(runId: Long) {
        if (_runItemsLoadedFor.value == runId && _runItems.value.isNotEmpty()) return
        viewModelScope.launch {
            safe {
                _runItems.value = runRepo.getRunItems(runId)
                _runItemsLoadedFor.value = runId
                // Opcional: limpiar borradores de items que ya no existen en este run
                val currentIds = _runItems.value.map { it.id }.toSet()
                _drafts.value = _drafts.value.filterKeys { it in currentIds }
            }
        }
    }

    fun loadRunInfo(runId: Long) {
        viewModelScope.launch { safe { _runInfo.value = runRepo.getRunInfo(runId) } }
    }

    fun createRun(storeCode: String, templateId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            safe {
                val res = runRepo.createRun(storeCode, templateId)
                onCreated(res.id)
                _pendingRuns.value = runRepo.getPendingRuns()
            }
        }
    }

    // Control de requests en vuelo para evitar duplicados
    private val _respondingItems = MutableStateFlow<Set<Long>>(emptySet())
    val respondingItems: StateFlow<Set<Long>> = _respondingItems

    fun respond(itemId: Long, status: String?, text: String?, number: Double?, barcode: String? = null, onUpdated: (RunItemDto) -> Unit = {}) {
        viewModelScope.launch {
            val item = _runItems.value.find { it.id == itemId }
            if (item == null) {
                _error.value = "Item no encontrado"
                return@launch
            }

            // Si ya hay un request en vuelo para este item, guardar borrador y salir (se reintenta al terminar)
            if (_respondingItems.value.contains(itemId)) {
                setDraft(itemId, status, text, number)
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
                setDraft(itemId, status, text, number)
                return@launch
            }

            // Validación específica para BOOLEAN, pero sin mostrar error si aún no hay status
            if (item.itemTemplate?.expectedType.equals("BOOLEAN", ignoreCase = true)) {
                val s = status?.uppercase()?.trim()
                if (s.isNullOrEmpty()) {
                    // Aún no se eligió OK/FAIL: solo guarda borrador y sal sin error
                    setDraft(itemId, status, text, number)
                    return@launch
                }
                if (s != "OK" && s != "FAIL") {
                    _error.value = "Solo se permite OK o FAIL para este campo."
                    setDraft(itemId, status, text, number)
                    return@launch
                }
            }

            // Marcar en vuelo
            _respondingItems.value = _respondingItems.value + itemId

            try {
                val updatedItemDto = runRepo.respond(itemId, status, text, number, barcode)

                _runItems.value = _runItems.value.map { currentItemInList ->
                    if (currentItemInList.id == updatedItemDto.id) {
                        currentItemInList.copy(
                            responseStatus = updatedItemDto.responseStatus,
                            responseText = updatedItemDto.responseText,
                            responseNumber = updatedItemDto.responseNumber,
                            scannedBarcode = updatedItemDto.scannedBarcode,
                            respondedAt = updatedItemDto.respondedAt,
                            attachments = updatedItemDto.attachments?.takeIf { it.isNotEmpty() }
                                ?: currentItemInList.attachments
                        )
                    } else {
                        currentItemInList
                    }
                }

                // Al terminar, revisar si hay un borrador pendiente que difiera del estado del servidor
                val latest = _runItems.value.find { it.id == itemId }
                val pendingDraft = _drafts.value[itemId]
                val attachmentsCount = latest?.attachments?.size ?: 0
                val needsPhotos = photosNeeded > 0

                if (pendingDraft != null) {
                    val draftStatus = pendingDraft.status
                    val serverStatus = latest?.responseStatus
                    val canSendDraftNow = !needsPhotos || (needsPhotos && attachmentsCount >= photosNeeded)
                    if (canSendDraftNow && draftStatus != null && draftStatus != serverStatus) {
                        // Consumir el draft reintentando guardado
                        clearDraft(itemId)
                        respond(itemId, pendingDraft.status, pendingDraft.text, pendingDraft.number, barcode, onUpdated)
                    } else if (draftStatus == serverStatus) {
                        clearDraft(itemId)
                    }
                } else {
                    // Sin borrador, limpiar cualquier residuo
                    clearDraft(itemId)
                }

                val finalItemToShow = latest ?: updatedItemDto
                onUpdated(finalItemToShow)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Error al guardar la respuesta"
                t.printStackTrace()
            } finally {
                // Desmarcar en vuelo
                _respondingItems.value = _respondingItems.value - itemId
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
                    runRepo.uploadAttachments(itemId, listOf(file))
                    uploadSuccess = true

                    // 3. Al tener éxito, refrescar la lista desde el servidor
                    val newAttachments = runRepo.listAttachments(itemId)

                    // ✅ MEJORADO: Preservar el localUri en el attachment más reciente para transición suave
                    val updatedAttachments = newAttachments.map { serverAtt ->
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

                    // Solo autoguardar si el borrador tiene status válido (OK/FAIL)
                    _drafts.value[itemId]?.let { draft ->
                        val st = draft.status?.uppercase()
                        if (st == "OK" || st == "FAIL") {
                            respond(itemId, draft.status, draft.text, draft.number)
                        }
                    }

                } catch (e: Exception) {
                    retryCount++
                    
                    // Detectar específicamente 401 (sesión expirada)
                    val is401 = e is retrofit2.HttpException && e.code() == 401
                    
                    if (is401) {
                        // NO reintentar si es 401, la sesión ya expiró
                        _runItems.value = _runItems.value.map { runItem ->
                            if (runItem.id == itemId) {
                                // Mantener la imagen temporal para que el usuario la vea
                                runItem.copy(attachments = runItem.attachments.orEmpty())
                            } else {
                                runItem
                            }
                        }
                        
                        _evidenceError.value = "Sesión expirada. La imagen se guardó localmente. Por favor inicia sesión nuevamente para subirla."
                        break // Salir del loop de reintentos
                    }
                    
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
                runRepo.deleteAttachment(itemId, attachmentId)
                val newAttachments = runRepo.listAttachments(itemId)
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
                runRepo.submitRun(runId)
                onSubmitted()
                _pendingRuns.value = runRepo.getPendingRuns()
                _historyRuns.value = runRepo.getHistoryRuns()
            }
        }
    }

    fun deleteRun(runId: Long, onOk: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe {
                runRepo.deleteRun(runId)
                _pendingRuns.value = runRepo.getPendingRuns()
                _historyRuns.value = runRepo.getHistoryRuns()
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
            // No mostrar errores si fue cancelada la corrutina
            if (t is kotlinx.coroutines.CancellationException) {
                throw t  // Relanzar para que la corrutina se cancele correctamente
            }

            // Mensajes de error específicos por tipo de excepción
            _error.value = when {
                t.message?.contains("timeout", ignoreCase = true) == true ->
                    "Error de conexión: La solicitud tardó demasiado. Verifica tu conexión."
                t.message?.contains("network", ignoreCase = true) == true ->
                    "Error de red: No hay conexión a internet."
                t.message?.contains("ConnectException", ignoreCase = true) == true ->
                    "Error de conexión: No se pudo conectar. Verifica tu internet."
                t.message?.contains("401", ignoreCase = true) == true ->
                    "Sesión expirada: Por favor, inicia sesión nuevamente."
                t.message?.contains("403", ignoreCase = true) == true ->
                    "No tienes permisos para realizar esta acción."
                t.message?.contains("404", ignoreCase = true) == true ->
                    "El recurso solicitado no existe."
                t.message?.contains("409", ignoreCase = true) == true ->
                    "Conflicto: Los datos han sido modificados. Recarga e intenta de nuevo."
                t.message?.contains("413", ignoreCase = true) == true ->
                    "Error: El archivo es demasiado grande."
                t.message?.contains("500", ignoreCase = true) == true ->
                    "Error del servidor: Intenta más tarde."
                t.message?.contains("502", ignoreCase = true) == true ||
                t.message?.contains("503", ignoreCase = true) == true ->
                    "Servidor no disponible: Intenta más tarde."
                else -> t.message ?: "Error inesperado"
            }
            t.printStackTrace()
        } finally {
            _loading.value = false
        }
    }
}
