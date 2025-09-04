package mx.checklist.data.api.dto

/**
 * Request para responder un ítem.
 * - responseStatus: requerido
 * - responseText: opcional
 * - responseNumber: opcional (Double?)
 */
data class RespondReq(
    val responseStatus: String,
    val responseText: String? = null,
    val responseNumber: Double? = null
)
