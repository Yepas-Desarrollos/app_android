package mx.checklist.data.api.dto

data class CreateTemplateDto(
    val name: String,
    val scope: String, // ✅ Cambiado: "Auditores" | "Supervisores" (en español, como en el backend)
    val items: List<CreateItemTemplateDto> = emptyList()
)