package mx.checklist.data.api.dto

/**
 * Request para responder un ítem.
 * - responseStatus: requerido (solo "OK" o "FAIL")
 * - responseText: opcional
 * - responseNumber: opcional (Double?)
 * - scannedBarcode: opcional
 */
data class RespondReq(
    val responseStatus: String, // "OK", "FAIL"
    val responseText: String? = null,
    val responseNumber: Double? = null,
    val scannedBarcode: String? = null
)
