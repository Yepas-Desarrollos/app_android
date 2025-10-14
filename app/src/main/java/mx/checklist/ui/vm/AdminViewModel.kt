package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import mx.checklist.data.Repo
import mx.checklist.data.api.dto.*

class AdminViewModel(private val repo: Repo) : ViewModel() {
    fun createSection(checklistId: Long, name: String = "Nueva sección", percentage: Double = 0.0, orderIndex: Int? = null, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Creando sección...") {
                // Validaciones antes de enviar al backend
                val currentSections = _currentTemplate.value?.sections ?: emptyList()
                val currentSum = currentSections.sumOf { it.percentage ?: 0.0 }
                val newSum = currentSum + percentage

                println("[AdminViewModel.CreateSection] Validaciones:")
                println("  - checklistId: $checklistId")
                println("  - name: '$name'")
                println("  - percentage: $percentage")
                println("  - currentSum: $currentSum")
                println("  - newSum: $newSum")
                println("  - sections count: ${currentSections.size}")

                // Validación básica: solo rechazar valores negativos
                if (percentage < 0.0) {
                    _error.value = "El porcentaje no puede ser negativo"
                    return@safe
                }

                // Validar que el nombre no esté vacío
                if (name.isBlank()) {
                    _error.value = "El nombre de la sección no puede estar vacío"
                    return@safe
                }

                // Advertencia si supera 100% (pero permitir la creación)
                if (newSum > 100.0) {
                    println("[AdminViewModel.CreateSection] ⚠️ ADVERTENCIA: La suma de porcentajes será ${String.format("%.1f", newSum)}% (supera 100%)")
                    // No bloquear la creación, solo advertir
                }

                val nuevaSeccion = SectionTemplateCreateDto(
                    name = name.trim(),
                    percentage = percentage,
                    orderIndex = orderIndex ?: currentSections.size // Usar valor por defecto si es null
                )

                println("[AdminViewModel.CreateSection] Enviando request: $nuevaSeccion")

                try {
                    repo.createSection(checklistId, nuevaSeccion)
                    println("[AdminViewModel.CreateSection] ✅ Sección creada exitosamente")
                    loadTemplate(checklistId)
                    // NO llamar onSuccess aquí - solo establecer el mensaje de éxito
                    _operationSuccess.value = "Sección creada exitosamente"
                } catch (e: Exception) {
                    println("[AdminViewModel.CreateSection] ❌ Error al crear sección: ${e.message}")
                    throw e
                }
            }
        }
    }
    
    // MÉTODOS PARA SECCIONES
    
    // Actualizar una sección existente
    fun updateSection(
        sectionId: Long,
        name: String? = null,
        percentage: Double? = null,
        orderIndex: Int? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            safe("Actualizando sección...") {
                val seccionActualizada = SectionTemplateUpdateDto(
                    name = name ?: "",
                    percentage = percentage ?: 0.0,
                    orderIndex = orderIndex ?: 0
                )
                repo.updateSection(sectionId, seccionActualizada)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    loadTemplate(templateId) // Recargar template para actualizar UI
                }
                // NO llamar onSuccess aquí - solo establecer el mensaje de éxito
                _operationSuccess.value = "Sección actualizada exitosamente"
            }
        }
    }
    
    // Eliminar una sección
    fun deleteSection(sectionId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Eliminando sección...") {
                repo.deleteSection(sectionId)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    // Reducir recargas: solo recargar una vez con delay
                    kotlinx.coroutines.delay(500) // Esperar a que el backend termine
                    loadTemplate(templateId)
                }
                onSuccess?.invoke()
                _operationSuccess.value = "Sección eliminada exitosamente"
            }
        }
    }
    
    // Reordenar secciones
    fun reorderSections(checklistId: Long, sectionIds: List<Long>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Reordenando secciones...") {
                repo.reorderSections(checklistId, sectionIds)
                // Reducir recargas: solo recargar una vez con delay
                kotlinx.coroutines.delay(300)
                loadTemplate(checklistId)
                onSuccess?.invoke()
                _operationSuccess.value = "Secciones reordenadas exitosamente"
            }
        }
    }
    
    // Distribuir porcentajes equitativamente entre secciones usando el endpoint correcto
    fun distributeSectionPercentages(checklistId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Distribuyendo porcentajes...") {
                // Usar el endpoint de distribución automática del backend
                repo.distributeSectionPercentages(checklistId)
                // Reducir recargas: solo recargar una vez con delay
                kotlinx.coroutines.delay(300)
                loadTemplate(checklistId)
                onSuccess?.invoke()
                _operationSuccess.value = "Porcentajes distribuidos exitosamente"
            }
        }
    }
    
    // Actualizar porcentajes de todas las secciones a la vez
    fun updateSectionPercentages(
        checklistId: Long, 
        sectionPercentages: List<SectionPercentageUpdateDto>, 
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            safe("Actualizando porcentajes...") {
                repo.updateSectionPercentages(checklistId, sectionPercentages)
                loadTemplate(checklistId) // Recargar template para actualizar UI
                onSuccess?.invoke()
                _operationSuccess.value = "Porcentajes actualizados exitosamente"
            }
        }
    }
    
    // MÉTODOS PARA ITEMS DE SECCIÓN
    
    // Obtener items de una sección
    fun getSectionItems(sectionId: Long, onSuccess: ((List<ItemTemplateDto>) -> Unit)? = null) {
        viewModelScope.launch {
            safe("Cargando items...") {
                val items = repo.getSectionItems(sectionId)
                onSuccess?.invoke(items)
            }
        }
    }
    
    // Crear item en una sección
    fun createSectionItem(
        sectionId: Long,
        title: String,
        expectedType: String = "TEXT", // Cambiado de "BOOLEAN" a "TEXT" por defecto
        category: String? = null,
        subcategory: String? = null,
        config: Map<String, Any?>? = null,
        evidence: Map<String, Any?>? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            safe("Creando ítem...") {
                // Validar que el título no esté vacío
                if (title.isBlank()) {
                    _error.value = "El título del ítem no puede estar vacío"
                    return@safe
                }

                // Obtener el siguiente orderIndex para esta sección
                val currentTemplate = _currentTemplate.value
                val sectionItems = currentTemplate?.sections?.find { it.id == sectionId }?.items ?: emptyList()
                val nextOrderIndex = if (sectionItems.isEmpty()) 1 else (sectionItems.maxOfOrNull { it.orderIndex } ?: 0) + 1

                val nuevoItem = ItemTemplateDto(
                    id = null,
                    sectionId = sectionId,
                    title = title.trim(),
                    percentage = null, // El backend no maneja percentage en items según tu información
                    category = category,
                    subcategory = subcategory,
                    expectedType = expectedType,
                    orderIndex = nextOrderIndex, // Obligatorio según backend
                    config = config
                )

                println("[AdminViewModel.CreateSectionItem] Enviando request:")
                println("  - sectionId: $sectionId")
                println("  - title: '$title'")
                println("  - expectedType: '$expectedType'")
                println("  - orderIndex: $nextOrderIndex")
                println("  - config: $config")

                try {
                    repo.createSectionItem(sectionId, nuevoItem)
                    println("[AdminViewModel.CreateSectionItem] ✅ Item creado exitosamente")

                    // Recargar template para actualizar UI
                    val templateId = currentTemplate?.id
                    if (templateId != null) {
                        loadTemplate(templateId)
                    }

                    // NO llamar onSuccess aquí - solo establecer el mensaje de éxito
                    _operationSuccess.value = "Ítem creado exitosamente"
                } catch (e: Exception) {
                    println("[AdminViewModel.CreateSectionItem] ❌ Error al crear ítem: ${e.message}")
                    throw e
                }
            }
        }
    }
    
    // Actualizar item de una sección
    fun updateSectionItem(
        itemId: Long,
        title: String? = null,
        expectedType: String? = null,
        category: String? = null,
        subcategory: String? = null,
        orderIndex: Int? = null,
        config: Map<String, Any?>? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            safe("Actualizando ítem...") {
                val itemActualizado = ItemTemplateDto(
                    id = itemId,
                    sectionId = null, // No necesario para actualización
                    title = title,
                    percentage = null, // El backend no maneja percentage según tu información
                    category = category,
                    subcategory = subcategory,
                    expectedType = expectedType,
                    orderIndex = orderIndex ?: 0,
                    config = config
                )

                println("[AdminViewModel.UpdateSectionItem] Enviando request:")
                println("  - itemId: $itemId")
                println("  - title: '$title'")
                println("  - expectedType: '$expectedType'")
                println("  - category: '$category'")
                println("  - subcategory: '$subcategory'")

                try {
                    repo.updateSectionItem(itemId, itemActualizado)
                    println("[AdminViewModel.UpdateSectionItem] ✅ Item actualizado exitosamente")

                    // Recargar template para actualizar UI
                    val templateId = _currentTemplate.value?.id
                    if (templateId != null) {
                        loadTemplate(templateId)
                    }

                    // NO llamar onSuccess aquí - solo establecer el mensaje de éxito
                    _operationSuccess.value = "Ítem actualizado exitosamente"
                } catch (e: Exception) {
                    println("[AdminViewModel.UpdateSectionItem] ❌ Error al actualizar ítem: ${e.message}")
                    throw e
                }
            }
        }
    }
    
    // Eliminar item de una sección
    fun deleteSectionItem(sectionId: Long, itemId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Eliminando ítem...") {
                repo.deleteSectionItem(sectionId, itemId)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    loadTemplate(templateId) // Recargar template para actualizar UI
                }
                onSuccess?.invoke()
                _operationSuccess.value = "Ítem eliminado exitosamente"
            }
        }
    }
    
    // Reordenar items de una sección
    fun reorderItems(sectionId: Long, itemIds: List<Long>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Reordenando ítems...") {
                repo.reorderItems(sectionId, itemIds)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    loadTemplate(templateId) // Recargar template para actualizar UI
                }
                onSuccess?.invoke()
                _operationSuccess.value = "Ítems reordenados exitosamente"
            }
        }
    }
    
    // Distribuir porcentajes equitativamente entre items
    fun distributeItemPercentages(sectionId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Distribuyendo porcentajes de ítems...") {
                repo.distributeItemPercentages(sectionId)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    loadTemplate(templateId) // Recargar template para actualizar UI
                }
                onSuccess?.invoke()
                _operationSuccess.value = "Porcentajes de ítems distribuidos exitosamente"
            }
        }
    }
    
    // Actualizar porcentajes de todos los items a la vez
    fun updateItemPercentages(
        sectionId: Long, 
        itemPercentages: List<Map<String, Any>>, 
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            safe("Actualizando porcentajes de ítems...") {
                repo.updateItemPercentages(sectionId, itemPercentages)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    loadTemplate(templateId) // Recargar template para actualizar UI
                }
                onSuccess?.invoke()
                _operationSuccess.value = "Porcentajes de ítems actualizados exitosamente"
            }
        }
    }
    
    // Mover un item a otra sección
    fun moveItemToSection(itemId: Long, targetSectionId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            safe("Moviendo ítem a otra sección...") {
                repo.moveItemToSection(itemId, targetSectionId)
                val templateId = _currentTemplate.value?.id
                if (templateId != null) {
                    loadTemplate(templateId) // Recargar template para actualizar UI
                }
                onSuccess?.invoke()
                _operationSuccess.value = "Ítem movido exitosamente"
            }
        }
    }

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _templates = MutableStateFlow<List<AdminTemplateDto>>(emptyList())
    val templates: StateFlow<List<AdminTemplateDto>> = _templates

    private val _currentTemplate = MutableStateFlow<AdminTemplateDto?>(null)
    val currentTemplate: StateFlow<AdminTemplateDto?> = _currentTemplate

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess

    fun clearError() { _error.value = null }
    fun clearSuccess() { _operationSuccess.value = null }

    // ✅ Limpiar template actual cuando se navega a crear uno nuevo
    fun clearCurrentTemplate() {
        _currentTemplate.value = null
    }

    fun loadTemplates() {
        viewModelScope.launch {
            safe("Cargando templates...") {
                Log.d("AdminViewModel", "Loading admin templates...")
                val result = repo.adminGetTemplates()
                Log.d("AdminViewModel", "Loaded ${result.size} admin templates")
                _templates.value = result
            }
        }
    }

    fun loadTemplate(templateId: Long) {
        viewModelScope.launch {
            safe("Cargando template...") {
                val template = repo.adminGetTemplate(templateId)
                android.util.Log.d("AdminViewModel", "Template cargado: $template")
                _currentTemplate.value = template
            }
        }
    }

    fun createTemplate(
        name: String,
        scope: String, // ✅ CAMBIADO: "Auditores" | "Supervisores" (en español)
        items: List<CreateItemTemplateDto> = emptyList(),
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            safe("Creando template...") {
                val request = CreateTemplateDto(
                    name = name,
                    scope = scope, // ✅ Enviar scope en español
                    items = items
                )
                val result = repo.adminCreateTemplate(request)

                // ✅ WORKAROUND: Crear automáticamente una sección "Items" para mantener compatibilidad con backend
                // El usuario no la verá, pero es necesaria para que el backend acepte los items
                try {
                    val dummySection = SectionTemplateCreateDto(
                        name = "Items",
                        percentage = 100.0,
                        orderIndex = 1
                    )
                    repo.createSection(result.id, dummySection)
                    Log.d("AdminViewModel", "✅ Sección dummy 'Items' creada automáticamente para template ${result.id}")
                } catch (e: Exception) {
                    Log.e("AdminViewModel", "⚠️ No se pudo crear sección dummy: ${e.message}")
                    // No fallar si no se puede crear la sección, el template ya existe
                }

                _operationSuccess.value = "Template '${result.name}' creado exitosamente"

                // Recargar el template completo desde el backend para obtener la sección creada
                val fullTemplate = repo.adminGetTemplate(result.id)
                _currentTemplate.value = fullTemplate

                onSuccess(result.id)
            }
        }
    }

    fun updateTemplate(
        templateId: Long,
        name: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Actualizando template...") {
                val request = UpdateTemplateDto(
                    name = name
                )
                // Actualizar en el backend
                repo.adminUpdateTemplate(templateId, request)
                
                // Recargar el template desde el backend para obtener datos actualizados
                _currentTemplate.value = repo.adminGetTemplate(templateId)
                _operationSuccess.value = "Template actualizado exitosamente"
                // NO llamar loadTemplates() para evitar navegación no deseada
                onSuccess()
            }
        }
    }

    // Método para actualizar solo el estado activo/inactivo (version simplificada)
    fun updateTemplateStatus(
        templateId: Long,
        isActive: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Actualizando estado del template...") {
                // ✅ CORREGIDO: Ahora guarda en el backend usando el endpoint correcto
                val result = repo.adminUpdateTemplateStatus(templateId, isActive)

                // Actualizar el template localmente con el resultado del backend
                _currentTemplate.value = repo.adminGetTemplate(templateId)

                _operationSuccess.value = result.message
                onSuccess()
            }
        }
    }

    fun deleteTemplate(templateId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            safe("Eliminando template...") {
                val result = repo.adminDeleteTemplate(templateId)
                if (result.success) {
                    _operationSuccess.value = "Template eliminado exitosamente"
                    // Solo cargar templates UNA vez después de eliminar
                    loadTemplates()
                    onSuccess()
                } else {
                    _error.value = result.message ?: "Error al eliminar template"
                }
            }
        }
    }

    fun createItem(
        templateId: Long,
        orderIndex: Int,
        title: String,
        category: String?,
        subcategory: String?,
        percentage: Double?,
        expectedType: String,
        config: Map<String, Any?>?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Creando item...") {
                Log.d("AdminViewModel", "templateId usado para crear ítem: $templateId")
                if (templateId == 0L) {
                    _error.value = "Error: El templateId es 0. No se puede crear el ítem."
                    return@safe
                }

                // ✅ WORKAROUND: Obtener el sectionId de la primera sección disponible
                // Esto mantiene compatibilidad con el backend que requiere sectionId
                val currentTemplate = _currentTemplate.value
                val firstSectionId = currentTemplate?.sections?.firstOrNull()?.id

                if (firstSectionId == null) {
                    _error.value = "Error: No hay secciones disponibles en el template. Recarga el template."
                    Log.e("AdminViewModel", "❌ No se encontró sectionId para crear item")
                    return@safe
                }

                val request = CreateItemTemplateDto(
                    sectionId = firstSectionId, // ✅ Usar sectionId de la sección automática
                    orderIndex = orderIndex,
                    title = title,
                    category = category,
                    subcategory = subcategory,
                    percentage = percentage,
                    expectedType = expectedType,
                    config = config
                )

                Log.d("AdminViewModel", "Creando item con sectionId=$firstSectionId: $request")
                repo.adminCreateItem(templateId, request)
                _operationSuccess.value = "Item '$title' creado exitosamente"
                loadTemplate(templateId) // Refrescar template actual
                onSuccess()
            }
        }
    }

    fun updateItem(
        templateId: Long,
        itemId: Long,
        orderIndex: Int?,
        title: String?,
        category: String?,
        subcategory: String?,
        percentage: Double?, // ✅ AGREGADO: Parámetro de porcentaje
        expectedType: String?,
        config: Map<String, Any?>?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Actualizando item...") {
                // ✅ Normalizar el config para convertir "type": "photo" a "type": "PHOTO"
                val normalizedConfig = normalizeConfig(config)

                val request = UpdateItemTemplateDto(
                    orderIndex = orderIndex,
                    title = title,
                    category = category,
                    subcategory = subcategory,
                    percentage = percentage, // ✅ AGREGADO: Incluir en el request
                    expectedType = expectedType,
                    config = normalizedConfig // ✅ Usar config normalizado
                )
                repo.adminUpdateItem(templateId, itemId, request)
                _operationSuccess.value = "Item actualizado exitosamente"
                loadTemplate(templateId) // Refrescar template actual
                onSuccess()
            }
        }
    }

    fun deleteItem(templateId: Long, itemId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            safe("Eliminando item...") {
                val result = repo.adminDeleteItem(templateId, itemId)
                if (result.success) {
                    _operationSuccess.value = "Item eliminado exitosamente"
                    loadTemplate(templateId) // Refrescar template actual
                    onSuccess()
                } else {
                    _error.value = result.message ?: "Error al eliminar item"
                }
            }
        }
    }

    fun forceDeleteRun(runId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            safe("Eliminando corrida enviada...") {
                val result = repo.adminForceDeleteRun(runId)
                if (result.success) {
                    _operationSuccess.value = result.message
                    onSuccess()
                } else {
                    _error.value = "Error al eliminar corrida"
                }
            }
        }
    }

    /**
     * Normaliza el config de un item para asegurar que el type de evidencia esté en mayúsculas
     */
    private fun normalizeConfig(config: Map<String, Any?>?): Map<String, Any?>? {
        if (config == null) return null

        val evidence = config["evidence"] as? Map<*, *> ?: return config
        val type = evidence["type"] as? String ?: return config

        // Convertir el type a mayúsculas
        val normalizedEvidence = evidence.toMutableMap().apply {
            this["type"] = type.uppercase()
        }

        return config.toMutableMap().apply {
            this["evidence"] = normalizedEvidence
        }
    }

    private suspend inline fun safe(loadingMessage: String, crossinline block: suspend () -> Unit) {
        try {
            _error.value = null
            _loading.value = true
            block()
        } catch (t: Throwable) {
            val errorMsg = when (t) {
                is retrofit2.HttpException -> {
                    // Extraer mensaje del servidor para errores HTTP
                    try {
                        val errorBody = t.response()?.errorBody()?.string()
                        if (errorBody?.contains("\"message\"") == true) {
                            // Parsear JSON simple para extraer el mensaje
                            val messageStart = errorBody.indexOf("\"message\":\"") + 11
                            val messageEnd = errorBody.indexOf("\"", messageStart)
                            if (messageStart > 10 && messageEnd > messageStart) {
                                errorBody.substring(messageStart, messageEnd)
                            } else {
                                "Error del servidor: ${t.message}"
                            }
                        } else {
                            "Error del servidor: ${t.message}"
                        }
                    } catch (e: Exception) {
                        "Error del servidor: ${t.message}"
                    }
                }
                else -> "Error: ${t.message}"
            }
            Log.e("AdminViewModel", errorMsg, t)
            _error.value = errorMsg
            t.printStackTrace()
        } finally {
            _loading.value = false
        }
    }
}