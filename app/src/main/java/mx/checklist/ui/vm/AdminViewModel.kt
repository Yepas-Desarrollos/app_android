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
                _currentTemplate.value = repo.adminGetTemplate(templateId)
            }
        }
    }

    fun createTemplate(
        name: String,
        description: String?,
        items: List<CreateItemTemplateDto> = emptyList(),
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            safe("Creando template...") {
                val request = CreateTemplateDto(
                    name = name,
                    description = description,
                    items = items
                )
                val result = repo.adminCreateTemplate(request)
                _operationSuccess.value = "Template '${result.name}' creado exitosamente"
                loadTemplates() // Refrescar lista
                onSuccess(result.id)
            }
        }
    }

    fun updateTemplate(
        templateId: Long,
        name: String?,
        description: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Actualizando template...") {
                val request = UpdateTemplateDto(
                    name = name,
                    description = description
                )
                val result = repo.adminUpdateTemplate(templateId, request)
                _currentTemplate.value = result
                _operationSuccess.value = "Template actualizado exitosamente"
                loadTemplates() // Refrescar lista
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
                    loadTemplates() // Refrescar lista
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
        expectedType: String,
        config: Map<String, Any?>?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Creando item...") {
                val request = CreateItemTemplateDto(
                    orderIndex = orderIndex,
                    title = title,
                    category = category,
                    subcategory = subcategory,
                    expectedType = expectedType,
                    config = config
                )
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
        expectedType: String?,
        config: Map<String, Any?>?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            safe("Actualizando item...") {
                val request = UpdateItemTemplateDto(
                    orderIndex = orderIndex,
                    title = title,
                    category = category,
                    subcategory = subcategory,
                    expectedType = expectedType,
                    config = config
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

    private suspend inline fun safe(loadingMessage: String, crossinline block: suspend () -> Unit) {
        try {
            _error.value = null
            _loading.value = true
            block()
        } catch (t: Throwable) {
            val errorMsg = "Error: ${t.message}"
            Log.e("AdminViewModel", errorMsg, t)
            _error.value = errorMsg
            t.printStackTrace()
        } finally {
            _loading.value = false
        }
    }
}