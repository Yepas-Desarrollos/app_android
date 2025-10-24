# 🔧 Solución: Limitación a 20 Checklists Enviados

## ❌ Problema Identificado

En la pantalla de **"Historial de Checklists"** (específicamente en la pestaña "Enviados"), solo se mostraban **20 checklists máximo**, aunque en la BD había más (p.ej., 40 de auditorías completadas por diferentes auditores).

### Causa Raíz

El problema estaba en cómo se cargaban los datos:

1. **HistoryScreen.kt** y **SimpleOptimizedHistoryScreen.kt** llamaban a:
   ```kotlin
   vm.loadHistoryRuns()  // SIN PARÁMETROS
   ```

2. **RunsViewModel.kt** tenía el parámetro por defecto:
   ```kotlin
   fun loadHistoryRuns(limit: Int? = 20, storeCode: String? = null) {
       viewModelScope.launch { safe { _historyRuns.value = repo.historyRuns(limit, storeCode) } }
   }
   ```

3. **Repo.kt** siempre usaba `page = 1`:
   ```kotlin
   suspend fun historyRuns(limit: Int? = 20, storeCode: String? = null): List<RunSummaryDto> {
       val response = api.historyRunsPaginated(page = 1, limit = limit ?: 20)
       return response.data  // ← SIEMPRE page 1, limit 20
   }
   ```

**Resultado**: Solo se traían los primeros 20 checklists enviados, siempre de la página 1.

---

## ✅ Solución Aplicada

Se aumentó el `limit` de **20 a 1000** en todas las llamadas a `loadHistoryRuns()`:

### Archivo 1: `HistoryScreen.kt`
```kotlin
// ANTES
LaunchedEffect(Unit) {
    vm.loadPendingRuns(all = true)
    vm.loadHistoryRuns()  // ← limit por defecto = 20
}

// DESPUÉS
LaunchedEffect(Unit) {
    vm.loadPendingRuns(all = true)
    vm.loadHistoryRuns(limit = 1000)  // ← Cargar hasta 1000 registros
}
```

También se actualizó el reload después de eliminar:
```kotlin
// ANTES
vm.loadHistoryRuns()

// DESPUÉS
vm.loadHistoryRuns(limit = 1000)
```

### Archivo 2: `SimpleOptimizedHistoryScreen.kt`
```kotlin
// ANTES
LaunchedEffect(Unit) {
    runsVM.loadPendingRuns(all = true)
    runsVM.loadHistoryRuns()  // ← limit por defecto = 20
}

// DESPUÉS
LaunchedEffect(Unit) {
    runsVM.loadPendingRuns(all = true)
    runsVM.loadHistoryRuns(limit = 1000)  // ← Cargar hasta 1000 registros
}
```

---

## 📊 Comportamiento Antes vs Después

| Aspecto | ANTES | DESPUÉS |
|---------|-------|---------|
| **Checklists mostrados** | Siempre 20 (últimos) | Hasta 1000 (primera carga) |
| **Para roles MGR/ADMIN** | Solo veían 20 de todos los auditores | Ven hasta 1000 checklists |
| **Paginación** | No implementada | Single load de hasta 1000 |
| **Performance** | Rápido (pocos datos) | Rápido (típicamente < 40 checklists) |

---

## 🎯 Nota Importante

- El **límite de 1000** es una solución práctica y temporal
- Para casos con **MÁS de 1000 checklists**, se recomienda implementar **lazy loading** con scroll infinito
- El endpoint paginado (`historyRunsPaginated`) ya existe en el backend y soporta paginación
- La solución actual carga TODO de una vez en la primera carga

---

## 🔄 Próximas Mejoras (Opcional)

Si en el futuro hay más de 1000 checklists y la performance se ve afectada, se puede implementar:

1. **Lazy Loading**: Cargar 50 registros al iniciar, luego 50 más cuando scroll llega al fondo
2. **Filtros**: Agregar filtros por fecha, auditor, tienda, estado
3. **Búsqueda**: Permitir buscar checklists específicos sin cargar todo

---

## 📝 Archivos Modificados

- ✏️ `app/src/main/java/mx/checklist/ui/screens/HistoryScreen.kt`
- ✏️ `app/src/main/java/mx/checklist/ui/screens/SimpleOptimizedHistoryScreen.kt`

**Fecha**: 2025-M10-24  
**Estado**: ✅ Compilado sin errores

