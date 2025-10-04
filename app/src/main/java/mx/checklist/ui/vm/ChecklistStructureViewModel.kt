package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.Repo
import mx.checklist.data.api.dto.*
import mx.checklist.data.auth.AuthState
import kotlin.math.abs

sealed class ChecklistStructureUiState {
    object Loading : ChecklistStructureUiState()
    data class Success(
        val checklistId: Long,
        val checklistName: String,
        val sections: List<SectionTemplateDto>
    ) : ChecklistStructureUiState()
    data class Error(val message: String) : ChecklistStructureUiState()
}

data class ValidationState(
    val isValid: Boolean = true,
    val message: String = "",
    val sectionsPercentageSum: Double = 0.0,
    val invalidSectionIds: List<Long> = emptyList(),
    val invalidItemIds: List<Long> = emptyList()
)

class ChecklistStructureViewModel(private val repo: Repo) : ViewModel() {
    private val _uiState = MutableStateFlow<ChecklistStructureUiState>(ChecklistStructureUiState.Loading)
    val uiState: StateFlow<ChecklistStructureUiState> = _uiState

    private val _validationState = MutableStateFlow(ValidationState())
    val validationState: StateFlow<ValidationState> = _validationState

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var localSections: MutableList<SectionTemplateDto> = mutableListOf()

    /**
     * Carga la estructura del checklist.
     * - Si el usuario es ADMIN: usa endpoint /admin/templates/{id} (completo)
     * - Si el usuario NO es admin: usa endpoint público /templates/{id}/structure
     */
    fun loadChecklistStructure(checklistId: Long) {
        viewModelScope.launch { safe {
            _uiState.value = ChecklistStructureUiState.Loading

            // Verificar si el usuario es administrador
            val isAdmin = AuthState.roleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")

            if (isAdmin) {
                // Usuario admin: usa endpoint completo de admin
                val template = repo.adminGetTemplate(checklistId)
                // Según backend: template.items[] siempre está vacío, todos los items están en sections[].items[]
                val sections = template.sections
                localSections = sections.toMutableList()
                _uiState.value = ChecklistStructureUiState.Success(checklistId, template.name, localSections)
            } else {
                // Usuario normal: usa endpoint público (solo lectura)
                // Backend filtra automáticamente por scope del usuario
                val template = repo.getTemplateStructure(checklistId)
                val sections = template.sections
                localSections = sections.toMutableList()

                // Ahora obtenemos el nombre real del template desde el endpoint público
                _uiState.value = ChecklistStructureUiState.Success(
                    checklistId,
                    template.name, // ✅ Nombre real del template
                    localSections
                )
            }

            recomputeSectionValidation()
        }}
    }

    fun loadSectionItems(sectionId: Long) {
        viewModelScope.launch { safe {
            val items = repo.getSectionItems(sectionId)
            updateLocalSection(sectionId) { it.copy(items = items) }
            recomputeItemValidation(sectionId)
        }}
    }

    // Secciones
    fun createSection(checklistId: Long, name: String, percentage: Double) {
        viewModelScope.launch { safe {
            // Validaciones antes de enviar al backend
            val currentSum = localSections.sumOf { it.percentage ?: 0.0 }
            val newSum = currentSum + percentage

            println("[CreateSection] Validaciones:")
            println("  - checklistId: $checklistId")
            println("  - name: '$name'")
            println("  - percentage: $percentage")
            println("  - currentSum: $currentSum")
            println("  - newSum: $newSum")
            println("  - sections count: ${localSections.size}")

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
                println("[CreateSection] ⚠️ ADVERTENCIA: La suma de porcentajes será ${String.format("%.1f", newSum)}% (supera 100%)")
                // No bloquear la creación, solo advertir
            }

            val orderIndex = localSections.size
            val request = SectionTemplateCreateDto(
                name = name.trim(),
                percentage = percentage,
                orderIndex = orderIndex
            )

            println("[CreateSection] Enviando request: $request")

            try {
                repo.createSection(checklistId, request)
                println("[CreateSection] ✅ Sección creada exitosamente")
                // Forzar reload desde backend para evitar desincronización
                loadChecklistStructure(checklistId)
            } catch (e: Exception) {
                println("[CreateSection] ❌ Error al crear sección: ${e.message}")
                throw e
            }
        }}
    }

    fun updateSection(id: Long, name: String, percentage: Double) {
        viewModelScope.launch { safe {
            val existing = localSections.firstOrNull { it.id == id } ?: return@safe
            val updateDto = SectionTemplateUpdateDto(
                name = name,
                percentage = percentage,
                orderIndex = existing.orderIndex
            )
            val updated = repo.updateSection(id, updateDto)
            localSections[localSections.indexOfFirst { it.id == id }] = updated
            publishSections(existingChecklistId())
            recomputeSectionValidation()
        }}
    }

    fun deleteSection(id: Long) {
        viewModelScope.launch { safe {
            repo.deleteSection(id)
            loadChecklistStructure(existingChecklistId() ?: return@safe)
        }}
    }

    fun updateSectionPercentages(checklistId: Long, sections: List<SectionPercentage>) {
        viewModelScope.launch { safe {
            val payload = sections.map { SectionPercentageUpdateDto(id = it.id, percentage = it.percentage) }
            val updated = repo.updateSectionPercentages(checklistId, payload)
            localSections = updated.toMutableList()
            publishSections(checklistId)
        }}
    }

    fun distributeSectionPercentages(checklistId: Long) {
        viewModelScope.launch { safe {
            val updated = repo.distributeSectionPercentages(checklistId)
            localSections = updated.toMutableList()
            publishSections(checklistId)
        }}
    }

    fun reorderSections(checklistId: Long, from: Int, to: Int) {
        if (from == to || from !in localSections.indices || to !in localSections.indices) return
        val mutable = localSections.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)
        localSections = mutable.mapIndexed { idx, s -> s.copy(orderIndex = idx) }.toMutableList()
        publishSections(checklistId)

        viewModelScope.launch { safe {
            val ids = localSections.mapNotNull { it.id }
            if (ids.size == localSections.size) {
                repo.reorderSections(checklistId, ids)
            }
        }}
    }

    // Items
    fun createItem(templateId: Long, sectionId: Long, title: String, percentage: Double, expectedType: String) {
        viewModelScope.launch { safe {
            val section = localSections.firstOrNull { it.id == sectionId } ?: return@safe
            val request = CreateItemTemplateDto(
                orderIndex = section.items.size,
                title = title,
                category = null,
                subcategory = null,
                expectedType = expectedType,
                config = null
            )
            repo.adminCreateItem(templateId, request)
            // Recargar los ítems desde el backend para obtener el id real
            loadSectionItems(sectionId)
        }}
    }

    fun updateItem(id: Long, title: String, percentage: Double) {
        viewModelScope.launch { safe {
            val section = localSections.firstOrNull { it.items.any { it.id == id } } ?: return@safe
            val item = section.items.first { it.id == id }
            val updated = repo.updateSectionItem(id, item.copy(title = title, percentage = percentage))
            updateLocalSection(section.id!!) { s ->
                s.copy(items = s.items.map { if (it.id == id) updated else it })
            }
        }}
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { safe {
            val section = localSections.firstOrNull { it.items.any { it.id == id } } ?: return@safe
            repo.deleteSectionItem(section.id!!, id)
            // Recargar los ítems desde el backend para mantener sincronización
            loadSectionItems(section.id!!)
        }}
    }

    fun updateItemPercentages(sectionId: Long, items: List<ItemPercentage>) {
        viewModelScope.launch { safe {
            val payload = items.map { mapOf("id" to it.id, "percentage" to it.percentage) }
            val updated = repo.updateItemPercentages(sectionId, payload)
            updateLocalSection(sectionId) { it.copy(items = updated) }
        }}
    }

    fun distributeItemPercentages(sectionId: Long) {
        viewModelScope.launch { safe {
            val updated = repo.distributeItemPercentages(sectionId)
            updateLocalSection(sectionId) { it.copy(items = updated) }
        }}
    }

    fun reorderItems(sectionId: Long, from: Int, to: Int) {
        viewModelScope.launch { safe {
            val section = localSections.firstOrNull { it.id == sectionId } ?: return@safe
            if (from == to || from !in section.items.indices || to !in section.items.indices) return@safe
            val mutable = section.items.toMutableList()
            val moved = mutable.removeAt(from)
            mutable.add(to, moved)
            val reordered = mutable.mapIndexed { idx, it -> it.copy(orderIndex = idx) }
            updateLocalSection(sectionId) { it.copy(items = reordered) }
            val ids = reordered.mapNotNull { it.id }
            if (ids.size == reordered.size) {
                // Nuevo endpoint: requiere sectionId y lista de IDs
                repo.reorderItems(sectionId, ids)
            }
        }}
    }

    fun moveItemToSection(itemId: Long, targetSectionId: Long) {
        viewModelScope.launch { safe {
            repo.moveItemToSection(itemId, targetSectionId)
            existingChecklistId()?.let { loadChecklistStructure(it) }
        }}
    }

    fun editSectionPercentageLocally(sectionId: Long, pct: Double) {
        localSections = localSections.map {
            if (it.id == sectionId) it.copy(percentage = pct) else it
        }.toMutableList()
        publishSections(existingChecklistId())
        recomputeSectionValidation()
    }

    fun editItemPercentageLocally(itemId: Long, pct: Double) {
        val section = localSections.firstOrNull { it.items.any { it.id == itemId } } ?: return
        updateLocalSection(section.id!!) { s ->
            s.copy(items = s.items.map {
                if (it.id == itemId) it.copy(percentage = pct) else it
            })
        }
        recomputeItemValidation(section.id!!)
    }

    fun validateSectionPercentages(sections: List<SectionTemplateDto>): Boolean {
        val sum = sections.sumOf { it.percentage ?: 0.0 }
        return abs(sum - 100.0) <= 0.01 && sections.all { (it.percentage ?: 0.0) > 0 }
    }

    fun validateItemPercentages(items: List<ItemTemplateDto>): Boolean {
        val sum = items.sumOf { it.percentage ?: 0.0 }
        return abs(sum - 100.0) <= 0.01 && items.all { (it.percentage ?: 0.0) > 0 }
    }

    private fun recomputeSectionValidation() {
        val sum = localSections.sumOf { it.percentage ?: 0.0 }
        val invalid = localSections.filter { (it.percentage ?: 0.0) <= 0 }.mapNotNull { it.id }
        val sectionValid = abs(sum - 100.0) <= 0.01 && invalid.isEmpty()

        _validationState.value = _validationState.value.copy(
            sectionsPercentageSum = sum,
            invalidSectionIds = invalid,
            isValid = sectionValid && _validationState.value.invalidItemIds.isEmpty()
        )
    }

    private fun recomputeItemValidation(sectionId: Long) {
        val section = localSections.firstOrNull { it.id == sectionId } ?: return
        val items = section.items
        val sum = items.sumOf { it.percentage ?: 0.0 }
        val invalid = items.filter { (it.percentage ?: 0.0) <= 0 }.mapNotNull { it.id }
        val itemsValid = abs(sum - 100.0) <= 0.01 && invalid.isEmpty()

        _validationState.value = _validationState.value.copy(
            invalidItemIds = invalid,
            isValid = itemsValid && _validationState.value.invalidSectionIds.isEmpty()
        )
    }

    private fun publishSections(checklistId: Long?) {
        checklistId ?: return
        val snapshot = localSections.map { section ->
            section.copy(items = section.items.toList())
        }
        _uiState.value = ChecklistStructureUiState.Success(
            checklistId,
            (uiState.value as? ChecklistStructureUiState.Success)?.checklistName ?: "",
            snapshot
        )
        recomputeSectionValidation()
    }

    private fun updateLocalSection(sectionId: Long, mapper: (SectionTemplateDto) -> SectionTemplateDto) {
        val idx = localSections.indexOfFirst { it.id == sectionId }
        if (idx >= 0) {
            localSections[idx] = mapper(localSections[idx])
            publishSections(existingChecklistId())
            recomputeItemValidation(sectionId)
        }
    }

    private fun existingChecklistId(): Long? = (uiState.value as? ChecklistStructureUiState.Success)?.checklistId

    private suspend inline fun safe(crossinline block: suspend () -> Unit) {
        try {
            _loading.value = true
            _error.value = null
            block()
        } catch (t: Throwable) {
            _error.value = t.message ?: "Error"
            _uiState.value = ChecklistStructureUiState.Error(_error.value!!)
        } finally {
            _loading.value = false
        }
    }
}