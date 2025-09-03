package mx.checklist.data.api.dto
data class StoreDto(
    val id: Long,
    val code: String,
    val name: String,
    val city: String? = null,
    val isActive: Boolean
)
