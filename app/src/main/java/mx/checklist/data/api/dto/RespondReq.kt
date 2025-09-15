package mx.checklist.data.api.dto

/**
 * Request para responder un ítem.
 * - responseStatus: requerido
 * - responseText: opcional
 * - responseNumber: opcional (Double?)
 */
data class RespondReq(
    val responseStatus: String, // "OK", "FAIL", "NA"
    val responseText: String? = null,
    val responseNumber: Int? = null,
    val scannedBarcode: String? = null
)
