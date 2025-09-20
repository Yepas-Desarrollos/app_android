package mx.checklist.data.api.dto

// Enum para los tipos de campo soportados
enum class FieldType(val displayName: String, val value: String) {
    BOOLEAN("Sí/No", "BOOLEAN"),
    SINGLE_CHOICE("Opción única", "SINGLE_CHOICE"), 
    MULTISELECT("Selección múltiple", "MULTISELECT"),
    SCALE("Escala numérica", "SCALE"),
    NUMBER("Número", "NUMBER"),
    TEXT("Texto", "TEXT"),
    PHOTO("Foto", "PHOTO"),
    BARCODE("Código de barras", "BARCODE");

    companion object {
        fun fromValue(value: String): FieldType? {
            return values().find { it.value == value }
        }
    }
}

// Configuraciones por defecto para cada tipo de campo
object FieldTypeDefaults {
    fun getDefaultConfig(fieldType: FieldType): Map<String, Any?> {
        return when (fieldType) {
            FieldType.BOOLEAN -> emptyMap()
            
            FieldType.SINGLE_CHOICE -> mapOf(
                "options" to listOf("Opción 1", "Opción 2", "Opción 3")
            )
            
            FieldType.MULTISELECT -> mapOf(
                "options" to listOf("Opción A", "Opción B", "Opción C"),
                "maxSelections" to 3
            )
            
            FieldType.SCALE -> mapOf(
                "min" to 1,
                "max" to 10,
                "step" to 1
            )
            
            FieldType.NUMBER -> mapOf(
                "min" to null,
                "max" to null,
                "decimals" to 2
            )
            
            FieldType.TEXT -> mapOf(
                "maxLength" to 500,
                "multiline" to true
            )
            
            FieldType.PHOTO -> mapOf(
                "evidence" to mapOf(
                    "type" to "PHOTO",
                    "required" to true,
                    "minCount" to 1,
                    "maxCount" to 5
                )
            )
            
            FieldType.BARCODE -> mapOf(
                "format" to "CODE_128"
            )
        }
    }
}