package mx.checklist.data.api.dto

// Enum para los tipos de campo soportados - SINCRONIZADO CON BACKEND
enum class FieldType(val displayName: String, val value: String) {
    TEXT("Texto", "TEXT"),
    NUMBER("Número", "NUMBER"),
    SINGLE_CHOICE("Opción única", "SINGLE_CHOICE"),
    MULTIPLE_CHOICE("Opción múltiple", "MULTIPLE_CHOICE"),
    PHOTO("Foto", "PHOTO"),
    BOOLEAN("Sí / No", "BOOLEAN");

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
            FieldType.TEXT -> emptyMap()

            FieldType.NUMBER -> emptyMap()

            FieldType.SINGLE_CHOICE -> mapOf(
                "options" to listOf("Opción 1", "Opción 2", "Opción 3")
            )
            
            FieldType.MULTIPLE_CHOICE -> mapOf(
                "options" to listOf("Opción A", "Opción B", "Opción C"),
                "maxSelections" to 3
            )
            
            FieldType.PHOTO -> mapOf(
                "minCount" to 1,
                "maxCount" to 5,
                "required" to true
            )

            FieldType.BOOLEAN -> mapOf(
                "default" to false
            )
        }
    }
}