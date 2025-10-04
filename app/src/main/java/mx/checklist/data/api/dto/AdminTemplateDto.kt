package mx.checklist.data.api.dto

// AdminTemplateDto actualizado para soportar secciones (estructura jerárquica de 3 niveles)
// Se mantiene la lista de items para compatibilidad temporal con pantallas existentes.

data class AdminTemplateDto(
    val id: Long,
    val name: String,
    val scope: String? = null,
    val version: Int? = null,
    val frequency: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // NUEVO: secciones con sus items internos
    val sections: List<SectionTemplateDto> = emptyList(),
    @Deprecated("Usar sections")
    val items: List<ItemTemplateDto> = emptyList()
)