# Estructura Actual del Frontend - Manejo de Templates y Categorías

## Resumen
El frontend **YA ESTÁ ADAPTADO** para trabajar con la estructura plana donde:
- Templates contienen Items directamente
- Las "secciones" se identifican mediante el campo `category` de cada item
- Se agrupa dinámicamente en la UI por categoría

## Estructura de Datos Actual

### Backend → Frontend
```
Template (checklist)
└── Items (directos, sin jerarquía de secciones en DB)
    ├── id
    ├── title
    ├── category ← ESTE CAMPO SE USA COMO "SECCIÓN"
    ├── subcategory (opcional)
    ├── expectedType
    ├── orderIndex
    └── config
```

### DTOs Actuales

#### ItemTemplateDto
```kotlin
data class ItemTemplateDto(
    val id: Long? = null,
    val sectionId: Long? = null,        // ⚠️ Legacy, puede estar null
    val title: String? = null,
    val percentage: Double? = null,
    val category: String? = null,       // ✅ USADO COMO SECCIÓN
    val subcategory: String? = null,
    val expectedType: String? = null,
    val orderIndex: Int = 0,
    val config: Map<String, Any?>? = null
)
```

#### RunItemDto
```kotlin
data class RunItemDto(
    val id: Long,
    val runId: Long,
    val itemTemplateId: Long,
    val orderIndex: Int,
    val responseStatus: String? = null,
    val responseText: String? = null,
    val responseNumber: Double? = null,
    val scannedBarcode: String? = null,
    val respondedAt: String? = null,
    val itemTemplate: ItemTemplateDto? = null,  // ✅ Contiene category
    val attachments: List<AttachmentDto>? = emptyList()
)
```

## Implementación en ItemsScreen.kt

### Agrupación por Categoría
```kotlin
// Línea 95-98 en ItemsScreen.kt
val itemsBySection = remember(items) {
    items
        .groupBy { it.itemTemplate?.category ?: "Sin categoría" }
        .toSortedMap() // Ordenar alfabéticamente por nombre de categoría
}
```

### Renderizado de Secciones
```kotlin
LazyColumn {
    itemsBySection.forEach { (sectionName, sectionItems) ->
        // Estadísticas por sección
        val sectionAnswered = sectionItems.count { !it.responseStatus.isNullOrEmpty() }
        val sectionTotal = sectionItems.size
        
        // Encabezado de sección
        item(key = "header_$sectionName") {
            Column {
                Text(text = sectionName)  // ← category se muestra como nombre de sección
                Text("$sectionAnswered de $sectionTotal completados")
            }
        }
        
        // Items de la sección
        items(sectionItems, key = { it.id }) { item ->
            // Renderizado del item
        }
    }
}
```

## DTOs de Compatibilidad

### SectionTemplateDto
```kotlin
// ⚠️ Este DTO existe para COMPATIBILIDAD con endpoints admin antiguos
// NO se usa en la ejecución normal de checklists
data class SectionTemplateDto(
    val id: Long? = null,
    val name: String,
    val percentage: Double? = null,
    val orderIndex: Int,
    val items: List<ItemTemplateDto> = emptyList()
)
```

### AdminTemplateDto
```kotlin
data class AdminTemplateDto(
    val id: Long,
    val name: String,
    // ...otros campos...
    val sections: List<SectionTemplateDto> = emptyList(),  // ⚠️ Para admin UI
    @Deprecated("Usar sections")
    val items: List<ItemTemplateDto> = emptyList()         // ⚠️ Legacy
)
```

## Flujo de Trabajo Actual

### 1. Usuario selecciona un template
```
Home → Selecciona Template → Selecciona Tienda → Crea Run
```

### 2. Backend crea el run y sus items
```
POST /runs
  → Backend crea run
  → Backend crea run_items basados en item_templates
  → Cada run_item tiene referencia a item_template.category
```

### 3. Frontend obtiene items del run
```
GET /runs/{id}/items
  → Retorna List<RunItemDto>
  → Cada RunItemDto contiene itemTemplate con category
```

### 4. Frontend agrupa por category
```kotlin
items.groupBy { it.itemTemplate?.category ?: "Sin categoría" }
```

### 5. Frontend renderiza secciones dinámicamente
```
Section: "Limpieza"
  - Item 1: ¿El piso está limpio?
  - Item 2: ¿Las mesas están ordenadas?

Section: "Seguridad"
  - Item 3: ¿Hay extintor?
  - Item 4: ¿Salidas despejadas?
```

## Ventajas de la Estructura Actual

✅ **Simplicidad**: No hay tabla de secciones en DB
✅ **Flexibilidad**: Las "secciones" (categorías) se pueden cambiar fácilmente
✅ **Escalabilidad**: Agregar subcategorías es natural
✅ **Compatibilidad**: Los items tienen orderIndex para ordenar dentro de categoría
✅ **Agrupación dinámica**: El frontend agrupa automáticamente por category

## Estado de Migración

### ✅ Ya Implementado
- [x] ItemTemplateDto tiene campo `category`
- [x] RunItemDto incluye itemTemplate con category
- [x] ItemsScreen agrupa por category
- [x] Renderizado de secciones dinámicas
- [x] Estadísticas por sección
- [x] Ordenamiento alfabético de secciones

### ⚠️ Legacy/Compatibilidad
- SectionTemplateDto: Solo para admin UI (si se usa)
- AdminTemplateDto.sections: Solo para admin UI
- ItemTemplateDto.sectionId: Puede ser null, no se usa

### 🔍 Recomendaciones

#### 1. Limpiar DTOs Legacy (Opcional)
Si ya no usas la estructura jerárquica con secciones en DB, podrías:
```kotlin
data class ItemTemplateDto(
    val id: Long? = null,
    // val sectionId: Long? = null,  ← REMOVER si no se usa
    val title: String? = null,
    val category: String? = null,      // ← La "sección" real
    val subcategory: String? = null,
    val expectedType: String? = null,
    val orderIndex: Int = 0,
    val config: Map<String, Any?>? = null
)
```

#### 2. Validar categorías en Admin UI
Si tienes pantallas de administración, asegúrate de que permitan:
- Asignar `category` a cada item
- Previsualizar cómo se agruparán los items por categoría
- Ordenar items dentro de cada categoría usando `orderIndex`

#### 3. Manejo de "Sin categoría"
Actualmente los items sin category van a "Sin categoría". Considera:
- Hacer `category` obligatorio en el backend
- O usar un valor por defecto como "General"

## Conclusión

**Tu frontend YA ESTÁ CORRECTAMENTE IMPLEMENTADO** para trabajar con la estructura plana:
- Templates → Items (directo)
- Category = Sección (agrupación lógica)
- La UI agrupa dinámicamente por category
- No necesitas cambiar nada en ItemsScreen.kt

La estructura es flexible y escalable para producción. ✅

