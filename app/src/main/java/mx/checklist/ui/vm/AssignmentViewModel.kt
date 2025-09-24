package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject
import mx.checklist.data.Repo
import mx.checklist.data.api.dto.*

class AssignmentViewModel(
    private val repo: Repo
) : ViewModel() {
    
    private val _users = MutableStateFlow<List<AssignableUserDto>>(emptyList())
    val users: StateFlow<List<AssignableUserDto>> = _users.asStateFlow()
    
    private val _summary = MutableStateFlow<List<AssignmentSummaryDto>>(emptyList())
    val summary: StateFlow<List<AssignmentSummaryDto>> = _summary.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sectors = MutableStateFlow<List<Int>>(emptyList())
    val sectors: StateFlow<List<Int>> = _sectors.asStateFlow()

    // Flag para prevenir doble submit
    private val _isAssigning = MutableStateFlow(false)
    val isAssigning: StateFlow<Boolean> = _isAssigning.asStateFlow()

    // Mensaje de feedback (snackbar)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun consumeSnackbarMessage() { _snackbarMessage.value = null }
    
    /**
     * Cargar usuarios que pueden ser asignados (AUDITOR/SUPERVISOR)
     */
    fun loadAssignableUsers() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                val users = repo.getAssignableUsers()
                _users.value = users
                
            } catch (e: Exception) {
                _error.value = "Error cargando usuarios: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Cargar sectores disponibles
     */
    fun loadSectors() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                val sectors = repo.getAssignmentSectors()
                _sectors.value = sectors
            } catch (e: Exception) {
                _error.value = "Error cargando sectores: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Cargar resumen de asignaciones por área
     */
    fun loadAssignmentSummary() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                val summaryList = repo.getAssignmentSummary()
                _summary.value = summaryList
                
            } catch (e: Exception) {
                _error.value = "Error cargando resumen: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Asignar usuario a sectores específicos
     */
    fun assignUserToSectors(userId: Long, sectors: List<Int>) {
        viewModelScope.launch {
            try {
                if (_isAssigning.value) return@launch // evitar doble tap
                _isAssigning.value = true
                _error.value = null

                val response = repo.assignUserToSectors(userId, sectors)
                // Extraer count si viene dentro de data { count: N }
                var count: Int? = null
                response.data?.let { d ->
                    when (d) {
                        is Map<*, *> -> {
                            (d["count"])?.let { c -> if (c is Number) count = c.toInt() }
                        }
                        // Si en el futuro data cambia de tipo se pueden agregar más ramas
                    }
                }
                val msg = buildString {
                    append("Asignado a sectores ")
                    append(sectors.joinToString(","))
                    if (count != null) append(" (" + count + " tiendas)")
                }
                _snackbarMessage.value = msg

                // Recargar datos en paralelo
                launch { loadAssignableUsers() }
                launch { loadAssignmentSummary() }
                
            } catch (e: Exception) {
                val detail = (e as? HttpException)?.let { http ->
                    runCatching {
                        val raw = http.response()?.errorBody()?.string()
                        if (!raw.isNullOrBlank()) {
                            val json = JSONObject(raw)
                            // Nest puede devolver { message: '...', ... } o { message: ['..'] }
                            when (val m = json.opt("message")) {
                                is String -> m
                                is org.json.JSONArray -> m.join(" | ")
                                else -> http.message()
                            }
                        } else http.message()
                    }.getOrElse { http.message() }
                } ?: e.message
                _error.value = "Error asignando usuario: ${detail}"
            } finally {
                _isAssigning.value = false
            }
        }
    }
    
    /**
     * Obtener tiendas asignadas a un usuario
     */
    fun getUserAssignedStores(userId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                val stores = repo.getUserAssignedStores(userId)
                // TODO: Manejar resultado
                
            } catch (e: Exception) {
                _error.value = "Error obteniendo asignaciones: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}