package mx.checklist.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.checklist.data.repository.AuditReviewRepository
import mx.checklist.data.api.dto.*
import java.io.File
import javax.inject.Inject

/**
 * ViewModel para el sistema de Revisiones y Correcciones de Auditorías
 */
@HiltViewModel
class AuditReviewViewModel @Inject constructor(
    private val repo: AuditReviewRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuditReviewVM"
        private const val PAGE_SIZE = 20
        private const val MAX_PHOTOS = 10
    }

    // ============================================
    // STATE FLOWS - Listas
    // ============================================

    // Revisiones pendientes (SUPERVISOR)
    private val _pendingReviews = MutableStateFlow<List<PendingReviewDto>>(emptyList())
    val pendingReviews: StateFlow<List<PendingReviewDto>> = _pendingReviews

    // Revisiones corregidas para validar (AUDITOR) - Solo CORRECTED
    private val _correctedReviews = MutableStateFlow<List<CorrectedReviewDto>>(emptyList())
    val correctedReviews: StateFlow<List<CorrectedReviewDto>> = _correctedReviews

    // Historial de revisiones validadas (AUDITOR) - VALIDATED y REJECTED
    private val _validatedReviews = MutableStateFlow<List<CorrectedReviewDto>>(emptyList())
    val validatedReviews: StateFlow<List<CorrectedReviewDto>> = _validatedReviews

    // Correcciones del equipo (MGR_OPS)
    private val _teamCorrections = MutableStateFlow<List<TeamCorrectionDto>>(emptyList())
    val teamCorrections: StateFlow<List<TeamCorrectionDto>> = _teamCorrections

    // Detalle de revisión
    private val _reviewDetail = MutableStateFlow<ReviewDetailDto?>(null)
    val reviewDetail: StateFlow<ReviewDetailDto?> = _reviewDetail

    // ============================================
    // STATE FLOWS - UI States
    // ============================================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // ============================================
    // STATE FLOWS - Paginación
    // ============================================

    // Paginación para revisiones pendientes
    private var pendingCurrentPage = 1
    private val _pendingHasMore = MutableStateFlow(false)
    val pendingHasMore: StateFlow<Boolean> = _pendingHasMore
    private val _pendingIsLoadingMore = MutableStateFlow(false)
    val pendingIsLoadingMore: StateFlow<Boolean> = _pendingIsLoadingMore

    // Paginación para revisiones corregidas
    private var correctedCurrentPage = 1
    private val _correctedHasMore = MutableStateFlow(false)
    val correctedHasMore: StateFlow<Boolean> = _correctedHasMore
    private val _correctedIsLoadingMore = MutableStateFlow(false)
    val correctedIsLoadingMore: StateFlow<Boolean> = _correctedIsLoadingMore

    // Paginación para correcciones del equipo
    private var teamCurrentPage = 1
    private val _teamHasMore = MutableStateFlow(false)
    val teamHasMore: StateFlow<Boolean> = _teamHasMore
    private val _teamIsLoadingMore = MutableStateFlow(false)
    val teamIsLoadingMore: StateFlow<Boolean> = _teamIsLoadingMore

    // Paginación para historial validado
    private var validatedCurrentPage = 1
    private val _validatedHasMore = MutableStateFlow(false)
    val validatedHasMore: StateFlow<Boolean> = _validatedHasMore
    private val _validatedIsLoadingMore = MutableStateFlow(false)
    val validatedIsLoadingMore: StateFlow<Boolean> = _validatedIsLoadingMore

    // Mis correcciones (SUPERVISOR) - historial de lo que he corregido
    private val _myCorrections = MutableStateFlow<List<MyCorrectionDto>>(emptyList())
    val myCorrections: StateFlow<List<MyCorrectionDto>> = _myCorrections

    // Paginación para mis correcciones
    private var myCorrectionsCurrentPage = 1
    private val _myCorrectionsHasMore = MutableStateFlow(false)
    val myCorrectionsHasMore: StateFlow<Boolean> = _myCorrectionsHasMore
    private val _myCorrectionsIsLoadingMore = MutableStateFlow(false)
    val myCorrectionsIsLoadingMore: StateFlow<Boolean> = _myCorrectionsIsLoadingMore

    // ============================================
    // STATE FLOWS - Fotos seleccionadas
    // ============================================

    private val _selectedPhotos = MutableStateFlow<List<File>>(emptyList())
    val selectedPhotos: StateFlow<List<File>> = _selectedPhotos

    // ============================================
    // SUPERVISOR: Cargar revisiones pendientes
    // ============================================

    /**
     * Cargar primera página de revisiones pendientes
     */
    fun loadPendingReviews(storeId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            pendingCurrentPage = 1

            try {
                val response = repo.getPendingReviewsPaginated(
                    page = 1,
                    limit = PAGE_SIZE,
                    storeId = storeId
                )
                _pendingReviews.value = response.data
                _pendingHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} revisiones pendientes")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando revisiones pendientes", e)
                _error.value = "Error al cargar revisiones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar más revisiones pendientes (paginación)
     */
    fun loadMorePendingReviews(storeId: Long? = null) {
        if (_pendingIsLoadingMore.value || !_pendingHasMore.value) return

        viewModelScope.launch {
            _pendingIsLoadingMore.value = true

            try {
                pendingCurrentPage++
                val response = repo.getPendingReviewsPaginated(
                    page = pendingCurrentPage,
                    limit = PAGE_SIZE,
                    storeId = storeId
                )
                
                _pendingReviews.value = _pendingReviews.value + response.data
                _pendingHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} más revisiones pendientes (página $pendingCurrentPage)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando más revisiones pendientes", e)
                pendingCurrentPage-- // Revertir incremento
                _error.value = "Error al cargar más revisiones: ${e.message}"
            } finally {
                _pendingIsLoadingMore.value = false
            }
        }
    }

    // ============================================
    // SUPERVISOR: Cargar MIS correcciones (historial)
    // ============================================

    /**
     * Cargar primera página de mis correcciones
     */
    fun loadMyCorrections(status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            myCorrectionsCurrentPage = 1

            try {
                val response = repo.getMyCorrectionsPagenated(
                    page = 1,
                    limit = PAGE_SIZE,
                    status = status
                )
                _myCorrections.value = response.data
                _myCorrectionsHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} de mis correcciones")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando mis correcciones", e)
                _error.value = "Error al cargar correcciones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar más de mis correcciones (paginación)
     */
    fun loadMoreMyCorrections(status: String? = null) {
        if (_myCorrectionsIsLoadingMore.value || !_myCorrectionsHasMore.value) return

        viewModelScope.launch {
            _myCorrectionsIsLoadingMore.value = true

            try {
                myCorrectionsCurrentPage++
                val response = repo.getMyCorrectionsPagenated(
                    page = myCorrectionsCurrentPage,
                    limit = PAGE_SIZE,
                    status = status
                )
                
                _myCorrections.value = _myCorrections.value + response.data
                _myCorrectionsHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} más de mis correcciones (página $myCorrectionsCurrentPage)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando más de mis correcciones", e)
                myCorrectionsCurrentPage--
                _error.value = "Error al cargar más correcciones: ${e.message}"
            } finally {
                _myCorrectionsIsLoadingMore.value = false
            }
        }
    }

    // ============================================
    // AUDITOR: Cargar revisiones corregidas
    // ============================================

    /**
     * Cargar primera página de revisiones corregidas (solo CORRECTED)
     */
    fun loadCorrectedReviews() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            correctedCurrentPage = 1

            try {
                val response = repo.getCorrectedReviewsPaginated(
                    page = 1,
                    limit = PAGE_SIZE,
                    status = "CORRECTED"
                )
                _correctedReviews.value = response.data
                _correctedHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} revisiones por validar")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando revisiones corregidas", e)
                _error.value = "Error al cargar correcciones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar más revisiones corregidas (paginación)
     */
    fun loadMoreCorrectedReviews() {
        if (_correctedIsLoadingMore.value || !_correctedHasMore.value) return

        viewModelScope.launch {
            _correctedIsLoadingMore.value = true

            try {
                correctedCurrentPage++
                val response = repo.getCorrectedReviewsPaginated(
                    page = correctedCurrentPage,
                    limit = PAGE_SIZE,
                    status = "CORRECTED"
                )
                
                _correctedReviews.value = _correctedReviews.value + response.data
                _correctedHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} más revisiones (página $correctedCurrentPage)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando más revisiones corregidas", e)
                correctedCurrentPage--
                _error.value = "Error al cargar más correcciones: ${e.message}"
            } finally {
                _correctedIsLoadingMore.value = false
            }
        }
    }

    // ============================================
    // AUDITOR: Historial de Validadas
    // ============================================

    /**
     * Cargar historial de revisiones validadas (VALIDATED + REJECTED)
     */
    fun loadValidatedHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            validatedCurrentPage = 1

            try {
                val response = repo.getCorrectedReviewsPaginated(
                    page = 1,
                    limit = PAGE_SIZE,
                    status = "all" // Trae todos para el historial
                )
                // Filtrar solo VALIDATED y REJECTED
                val filtered = response.data.filter { it.status in listOf("VALIDATED", "REJECTED", "APPROVED") }
                _validatedReviews.value = filtered
                _validatedHasMore.value = response.pagination.hasMore && filtered.isNotEmpty()
                Log.d(TAG, "✅ Cargadas ${filtered.size} revisiones en historial")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando historial", e)
                _error.value = "Error al cargar historial: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar más historial
     */
    fun loadMoreValidatedHistory() {
        if (_validatedIsLoadingMore.value || !_validatedHasMore.value) return

        viewModelScope.launch {
            _validatedIsLoadingMore.value = true

            try {
                validatedCurrentPage++
                val response = repo.getCorrectedReviewsPaginated(
                    page = validatedCurrentPage,
                    limit = PAGE_SIZE,
                    status = "all"
                )
                val filtered = response.data.filter { it.status in listOf("VALIDATED", "REJECTED", "APPROVED") }
                _validatedReviews.value = _validatedReviews.value + filtered
                _validatedHasMore.value = response.pagination.hasMore && filtered.isNotEmpty()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando más historial", e)
                validatedCurrentPage--
            } finally {
                _validatedIsLoadingMore.value = false
            }
        }
    }

    /**
     * Remover item de la lista de corregidos (update optimista)
     */
    fun removeFromCorrectedList(reviewId: Long) {
        _correctedReviews.value = _correctedReviews.value.filter { it.id != reviewId }
    }

    // ============================================
    // MGR_OPS: Cargar correcciones del equipo
    // ============================================

    /**
     * Cargar primera página de correcciones del equipo
     */
    fun loadTeamCorrections(storeId: Long? = null, userId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            teamCurrentPage = 1

            try {
                val response = repo.getTeamCorrectionsPaginated(
                    page = 1,
                    limit = PAGE_SIZE,
                    storeId = storeId,
                    userId = userId
                )
                _teamCorrections.value = response.data
                _teamHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} correcciones del equipo")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando correcciones del equipo", e)
                _error.value = "Error al cargar correcciones del equipo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar más correcciones del equipo (paginación)
     */
    fun loadMoreTeamCorrections(storeId: Long? = null, userId: Long? = null) {
        if (_teamIsLoadingMore.value || !_teamHasMore.value) return

        viewModelScope.launch {
            _teamIsLoadingMore.value = true

            try {
                teamCurrentPage++
                val response = repo.getTeamCorrectionsPaginated(
                    page = teamCurrentPage,
                    limit = PAGE_SIZE,
                    storeId = storeId,
                    userId = userId
                )
                
                _teamCorrections.value = _teamCorrections.value + response.data
                _teamHasMore.value = response.pagination.hasMore
                Log.d(TAG, "✅ Cargadas ${response.data.size} más correcciones del equipo (página $teamCurrentPage)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando más correcciones del equipo", e)
                teamCurrentPage--
                _error.value = "Error al cargar más correcciones: ${e.message}"
            } finally {
                _teamIsLoadingMore.value = false
            }
        }
    }

    // ============================================
    // DETALLE DE REVISIÓN
    // ============================================

    /**
     * Cargar detalle completo de una revisión
     */
    fun loadReviewDetail(reviewId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val detail = repo.getReviewDetail(reviewId)
                _reviewDetail.value = detail
                Log.d(TAG, "✅ Cargado detalle de revisión $reviewId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando detalle de revisión", e)
                _error.value = "Error al cargar detalle: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearReviewDetail() {
        _reviewDetail.value = null
    }

    // ============================================
    // MANEJO DE FOTOS
    // ============================================

    /**
     * Agregar una foto a la selección
     */
    fun addPhoto(file: File) {
        val current = _selectedPhotos.value.toMutableList()
        if (current.size < MAX_PHOTOS) {
            current.add(file)
            _selectedPhotos.value = current
            Log.d(TAG, "📷 Foto agregada: ${file.name} (${current.size}/$MAX_PHOTOS)")
        } else {
            _error.value = "Máximo $MAX_PHOTOS fotos permitidas"
        }
    }

    /**
     * Remover una foto de la selección
     */
    fun removePhoto(file: File) {
        val current = _selectedPhotos.value.toMutableList()
        current.remove(file)
        _selectedPhotos.value = current
        Log.d(TAG, "🗑️ Foto removida: ${file.name} (${current.size}/$MAX_PHOTOS)")
    }

    /**
     * Limpiar todas las fotos seleccionadas
     */
    fun clearPhotos() {
        _selectedPhotos.value = emptyList()
        Log.d(TAG, "🧹 Fotos limpiadas")
    }

    // ============================================
    // CREAR CORRECCIÓN
    // ============================================

    /**
     * Enviar corrección con fotos
     */
    fun submitCorrection(
        reviewId: Long,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val photos = _selectedPhotos.value

                // Validar mínimo 1 foto
                if (photos.isEmpty()) {
                    _error.value = "Debes adjuntar al menos 1 foto de evidencia"
                    _isLoading.value = false
                    return@launch
                }

                Log.d(TAG, "📤 Enviando corrección para revisión $reviewId con ${photos.size} fotos")

                // Crear corrección y subir fotos
                val (correction, attachments) = repo.createCorrectionWithPhotos(
                    reviewId = reviewId,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    photos = photos
                )

                Log.d(TAG, "✅ Corrección creada: ${correction.id} con ${attachments.size} fotos")

                // Limpiar fotos seleccionadas
                _selectedPhotos.value = emptyList()

                // Mostrar éxito
                _successMessage.value = "Corrección enviada exitosamente"

                // Recargar lista de pendientes
                loadPendingReviews()

                // Callback de éxito
                onSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error enviando corrección", e)
                _error.value = "Error al enviar corrección: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // VALIDAR CORRECCIÓN
    // ============================================

    /**
     * Aprobar o rechazar una corrección
     */
    fun validateCorrection(
        reviewId: Long,
        approved: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val action = if (approved) "aprobando" else "rechazando"
                Log.d(TAG, "🔄 $action corrección de revisión $reviewId")

                repo.validateCorrection(reviewId, approved)

                val actionDone = if (approved) "aprobada" else "rechazada"
                _successMessage.value = "Corrección $actionDone exitosamente"
                Log.d(TAG, "✅ Corrección $actionDone")

                // Recargar lista de corregidas
                loadCorrectedReviews()

                // Callback de éxito
                onSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error validando corrección", e)
                _error.value = "Error al validar corrección: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // HELPERS
    // ============================================

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Limpiar todos los estados al salir
     */
    fun clearAll() {
        _pendingReviews.value = emptyList()
        _correctedReviews.value = emptyList()
        _teamCorrections.value = emptyList()
        _reviewDetail.value = null
        _selectedPhotos.value = emptyList()
        _error.value = null
        _successMessage.value = null
        pendingCurrentPage = 1
        correctedCurrentPage = 1
        teamCurrentPage = 1
    }
}
