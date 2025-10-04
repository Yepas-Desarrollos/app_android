# Corrección de Tipo de Datos - AssignmentSummary

## 🐛 Error Encontrado

```
Assignment type mismatch: actual type is 'mx.checklist.data.api.dto.AssignmentSummaryDto', 
but 'kotlin.collections.List<mx.checklist.data.api.dto.AssignmentSummaryDto>' was expected.
```

## 🔍 Análisis del Problema

### **Estado Anterior:**
- **ViewModel** esperaba: `List<AssignmentSummaryDto>`
- **Repo** devolvía: `AssignmentSummaryDto` (objeto único)
- **API** devolvía: `AssignmentSummaryDto` (objeto único)

### **Inconsistencia:**
El ViewModel está diseñado para mostrar múltiples elementos en una lista (para el tab "Resumen"), pero la API estaba devolviendo un solo objeto.

## ✅ Solución Implementada

### **Cambios Realizados:**

#### **1. Api.kt**
```kotlin
// ANTES
@GET("admin/assignments/summary")
suspend fun getAssignmentSummary(): AssignmentSummaryDto

// DESPUÉS  
@GET("admin/assignments/summary")
suspend fun getAssignmentSummary(): List<AssignmentSummaryDto>
```

#### **2. Repo.kt**
```kotlin
// ANTES
suspend fun getAssignmentSummary(): AssignmentSummaryDto {
    return api.getAssignmentSummary()
}

// DESPUÉS
suspend fun getAssignmentSummary(): List<AssignmentSummaryDto> {
    return api.getAssignmentSummary()
}
```

#### **3. ViewModel (Sin Cambios)**
```kotlin
// ✅ YA ESTABA CORRECTO
private val _summary = MutableStateFlow<List<AssignmentSummaryDto>>(emptyList())
val summary: StateFlow<List<AssignmentSummaryDto>> = _summary.asStateFlow()
```

## 🎯 Justificación

### **¿Por qué Lista vs Objeto Único?**

La UI del AssignmentScreen muestra el resumen como:
- **Múltiples cards** por tienda/sector
- **LazyColumn** con `items(summary, key = { ... })`
- **Cada elemento** representa una combinación tienda+sector

Por lo tanto, es lógico que la API devuelva una **lista de resúmenes** donde cada elemento es:
```kotlin
AssignmentSummaryDto(
    storeCode = "T001",
    storeName = "Tienda Centro", 
    sector = "ABARROTES",
    assignedUsers = 3,
    users = [...]
)
```

## 🔄 Backend Compatibilidad

**Nota**: Esta corrección asume que el backend puede devolver una lista. Si el backend actual devuelve un objeto único, será necesario:

1. **Opción A**: Modificar el backend para devolver `List<AssignmentSummaryDto>`
2. **Opción B**: Adaptar el frontend para convertir el objeto único en lista

## 📱 Estado de Compilación

Con este cambio, el error de tipo debería estar **resuelto**. El flujo de datos ahora es consistente:

```
Backend API → List<AssignmentSummaryDto> 
     ↓
Repo → List<AssignmentSummaryDto>
     ↓  
ViewModel → StateFlow<List<AssignmentSummaryDto>>
     ↓
UI → LazyColumn con items(summary)
```