# 🔍 ANÁLISIS DE INCONGRUENCIAS Y MEJORAS - APP ANDROID CHECKLIST

## Fecha: 2025-01-10

---

## ⚠️ PROBLEMAS CRÍTICOS ENCONTRADOS

### 1. **DUPLICACIÓN DE PARÁMETROS EN `respond()` - RunsViewModel**
**Severidad:** 🔴 CRÍTICO

**Ubicación:** `RunsViewModel.kt` líneas 150-165

**Problema:**
```kotlin
// Se construye un RespondReq pero NO SE USA
val req = mx.checklist.data.api.dto.RespondReq(
    responseStatus = status ?: "",
    responseText = text,
    responseNumber = number,
    scannedBarcode = barcode
)
safe {
    //  Se pasan los parámetros individuales en lugar de usar 'req'
    val updatedItemDto = repo.respond(itemId, status, text, number)
    // ...
}
```

**Consecuencia:** 
- El campo `scannedBarcode` **NUNCA se envía al backend**
- Los campos de tipo BARCODE no funcionarán correctamente
- Se crea un objeto innecesario que se desecha

**Solución:**
```kotlin
// Opción 1: Usar el objeto RespondReq
val updatedItemDto = repo.respond(itemId, req)

// Opción 2: Cambiar firma de repo.respond() para aceptar barcode
val updatedItemDto = repo.respond(itemId, status, text, number, barcode)
```

---

### 2. **FUNCIÓN `safe()` DUPLICADA EN CADA VIEWMODEL**
**Severidad:** 🟡 MEDIO

**Problema:**
Cada ViewModel (`RunsViewModel`, `AdminViewModel`, etc.) tiene su propia implementación de la función `safe()` para manejo de errores:

```kotlin
// RunsViewModel
private suspend inline fun safe(crossinline block: suspend () -> Unit) {
    try {
        _error.value = null
        _loading.value = true
        block()
    } catch (t: Throwable) {
        _error.value = t.message ?: "Error inesperado"
        t.printStackTrace()
    } finally {
        _loading.value = false
    }
}

// AdminViewModel - CÓDIGO DUPLICADO
private suspend inline fun safe(loadingMsg: String? = null, crossinline block: suspend () -> Unit) {
    // ... mismo código con pequeñas variaciones
}
```

**Consecuencias:**
- Violación del principio DRY (Don't Repeat Yourself)
- Mantenimiento difícil: cambios deben replicarse en múltiples lugares
- Inconsistencia: `AdminViewModel` acepta `loadingMsg` pero no se usa

**Solución:** Crear una clase base `BaseViewModel` con la función `safe()`:

```kotlin
// Archivo nuevo: ui/vm/BaseViewModel.kt
abstract class BaseViewModel : ViewModel() {
    protected val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    protected val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    protected suspend inline fun safe(
        loadingMsg: String? = null,
        crossinline block: suspend () -> Unit
    ) {
        try {
            _error.value = null
            _loading.value = true
            block()
        } catch (t: Throwable) {
            _error.value = t.message ?: "Error inesperado"
            t.printStackTrace()
        } finally {
            _loading.value = false
        }
    }
}
```

---

### 3. **GESTIÓN DE AUTENTICACIÓN INCONSISTENTE**
**Severidad:** 🟠 ALTO

**Problema:** Hay **DOS fuentes de verdad** para el estado de autenticación:

1. `AuthState` (object global mutable con @Volatile)
```kotlin
object AuthState {
    @Volatile var token: String? = null
    @Volatile var roleCode: String? = null
}
```

2. `AuthViewModel.state` (StateFlow reactivo)
```kotlin
data class LoginState(
    val authenticated: Authenticated? = null,
    // ...
)
```

**Consecuencias:**
- Posibles condiciones de carrera
- Sincronización manual necesaria en múltiples lugares
- Estado puede desincronizarse entre `AuthState` y `AuthViewModel`
- Difícil de testear

**Ubicaciones del problema:**
- `MainActivity.kt`: Dos `LaunchedEffect` separados para sincronizar token y roleCode
- `AppNavHost.kt`: Depende de `AuthViewModel` pero actualiza `AuthState`
- `Repo.kt`: Actualiza `AuthState` directamente después de login

**Solución:** Eliminar `AuthState` y usar **solo** `AuthViewModel.state` como fuente única de verdad.

---

### 4. **CACHÉ DE TEMPLATES MAL IMPLEMENTADO**
**Severidad:** 🟡 MEDIO

**Ubicación:** `Repo.kt`

**Problema:**
```kotlin
private var cachedTemplates: List<TemplateDto>? = null

suspend fun templates(): List<TemplateDto> {
    //  Caché simple en memoria, se pierde al reiniciar la app
    cachedTemplates?.let { return it }
    
    val response = api.templatesPaginated(page = 1, limit = 100)
    val list = response.data
    cachedTemplates = list
    return list
}
```

**Problemas:**
- Caché solo en memoria RAM (volátil)
- No tiene expiración temporal
- No hay invalidación inteligente
- Se limita a 100 items arbitrariamente
- No usa Room Database para persistencia offline

**Solución:** Implementar caché con Room Database y política de expiración:
```kotlin
@Entity(tableName = "templates")
data class TemplateCacheEntity(
    @PrimaryKey val id: Long,
    val data: String, // JSON serializado
    val cachedAt: Long // timestamp
)
```

---

### 5. **INCONSISTENCIA EN MANEJO DE `percentage` EN ITEMS**
**Severidad:** 🟠 ALTO

**Ubicación:** Múltiples archivos

**Problema encontrado:**
```kotlin
// ItemTemplateDto.kt
data class ItemTemplateDto(
    val percentage: Double? = null, // ✅ Acepta null
    // ...
)

// AdminViewModel.kt - createSectionItem()
val nuevoItem = ItemTemplateDto(
    percentage = null, // ✅ Envía null
    // comentario dice: "El backend no maneja percentage en items"
)

// AdminViewModel.kt - updateItem()
fun updateItem(
    percentage: Double?, // ✅ AGREGADO: Parámetro de porcentaje
    // ...
) {
    val request = UpdateItemTemplateDto(
        percentage = percentage, // ✅ AGREGADO: Incluir en el request
    )
}

// Pero en Repo.kt - distributeItemPercentages()
suspend fun distributeItemPercentages(sectionId: Long): DistributeItemsResponse
// Este método SÍ distribuye porcentajes a items
```

**Inconsistencia:** Los comentarios dicen que items NO manejan porcentajes, pero:
- El API tiene endpoint `distributeItemPercentages`
- El DTO tiene campo `percentage`
- Algunos métodos lo incluyen, otros no

**Impacto:** Confusión sobre si los items tienen o no porcentajes.

---

### 6. **WORKAROUND INNECESARIO - SECCIÓN "DUMMY"**
**Severidad:** 🟡 MEDIO

**Ubicación:** `AdminViewModel.kt` - `createTemplate()`

```kotlin
// ✅ WORKAROUND: Crear automáticamente una sección "Items" 
try {
    val dummySection = SectionTemplateCreateDto(
        name = "Items",
        percentage = 100.0,
        orderIndex = 1
    )
    repo.createSection(result.id, dummySection)
    Log.d("AdminViewModel", "✅ Sección dummy 'Items' creada automáticamente")
} catch (e: Exception) {
    Log.e("AdminViewModel", "⚠️ No se pudo crear sección dummy: ${e.message}")
}
```

**Problema:**
- Crea secciones ocultas que el usuario no solicitó
- Puede generar confusión en el modelo de datos
- El backend debería crear esta sección automáticamente si es requerida
- El error se ignora silenciosamente

**Solución:** 
- Coordinar con backend para que cree la sección automáticamente
- O permitir templates sin secciones si no se necesitan

---

## 🐛 BUGS POTENCIALES

### 7. **RACE CONDITION EN SINCRONIZACIÓN DE TOKEN**
**Ubicación:** `MainActivity.kt`

```kotlin
LaunchedEffect(Unit) {
    tokenStore.tokenFlow.collect { savedToken ->
        if (savedToken != null) {
            AuthState.token = savedToken
            ApiClient.setToken(savedToken)
        }
    }
}

LaunchedEffect(Unit) {
    tokenStore.roleCodeFlow.collect { savedRole ->
        AuthState.roleCode = savedRole
    }
}
```

**Problema:** Dos flows independientes pueden sincronizarse en diferente orden:
- Puede llegar `roleCode` antes que `token`
- Puede resultar en estado inconsistente temporalmente

---

### 8. **ACTUALIZACIÓN OPTIMISTA PUEDE FALLAR SILENCIOSAMENTE**
**Ubicación:** `RunsViewModel.uploadAttachment()`

```kotlin
// 1. Actualización optimista: añadir el adjunto temporal
_runItems.value = _runItems.value.map { runItem ->
    if (runItem.id == itemId) {
        runItem.copy(attachments = runItem.attachments.orEmpty() + tempAttachment)
    } else {
        runItem
    }
}

// 2. Upload con reintentos (puede fallar después de 3 intentos)
while (retryCount < maxRetries && !uploadSuccess) {
    try {
        repo.uploadAttachments(itemId, listOf(file))
        uploadSuccess = true
        // ...
    } catch (e: Exception) {
        // Si falla, elimina el temporal
        // ⚠️ PERO: Si el usuario navega a otra pantalla antes,
        // el estado temporal se pierde sin notificación clara
    }
}
```

---

### 9. **VALIDACIÓN DE EVIDENCIAS DUPLICADA**
**Severidad:** 🟡 MEDIO

**Problema:** La lógica de validación de fotos está duplicada:

1. **En `RunsViewModel.respond()`** - líneas 100-140
2. **En `RunsViewModel.canSubmitAll()`** - líneas 300-340

Ambos métodos validan:
- `evidence.required`
- `evidence.minCount`
- `evidence.requiredOnFail`
- Tipos `PHOTO`, `MULTIPHOTO`

**Consecuencia:** Cambios en reglas de validación requieren actualizar 2 lugares.

---

## 🏗️ PROBLEMAS DE ARQUITECTURA

### 10. **REPO TIENE DEPENDENCIAS CRUZADAS**
**Problema:** El `Repo` conoce y usa:
- `ApiClient` (OK)
- `TokenStore` (OK)
- `HttpException` de Retrofit (⚠️ Detalle de implementación)

Debería retornar tipos propios y manejar conversiones internamente.

---

### 11. **NO HAY MANEJO DE ESTADOS DE RED**
**Problema:** La app no diferencia entre:
- Error de red (sin internet)
- Error de servidor (500)
- Error de autenticación (401/403)
- Error de validación (400)

Todos se muestran como: `"Error: [mensaje]"`

**Solución:** Crear sealed class para tipos de error:
```kotlin
sealed class AppError {
    data class Network(val message: String) : AppError()
    data class Authentication(val message: String) : AppError()
    data class Server(val code: Int, val message: String) : AppError()
    data class Validation(val field: String, val message: String) : AppError()
}
```

---

### 12. **NAVEGACIÓN COMPLEJA Y MANUAL**
**Problema:** `AppNavHost.kt` tiene mucha lógica de negocio:
- Validación de roles
- Redirecciones condicionales
- Feature flags para paginación

Esta lógica debería estar en un `NavigationManager` o `Router`.

---

## 📊 PROBLEMAS DE RENDIMIENTO

### 13. **RECARGAS INNECESARIAS DE TEMPLATES**
**Ubicación:** `AdminViewModel.kt`

Cada operación CRUD recarga TODO el template:
```kotlin
fun createSection(...) {
    // ...
    loadTemplate(checklistId) // ⚠️ Recarga todo
}

fun updateSection(...) {
    // ...
    loadTemplate(templateId) // ⚠️ Recarga todo
}

fun deleteSection(...) {
    // ...
    loadTemplate(templateId) // ⚠️ Recarga todo
}
```

**Impacto:** 
- Latencia innecesaria
- Consumo de datos móviles
- UX más lenta

**Solución:** Actualizar solo el elemento modificado en el estado local.

---

### 14. **UPLOADS DE IMÁGENES SIN COMPRESIÓN**
**Ubicación:** `Repo.uploadAttachments()`

```kotlin
suspend fun uploadAttachments(itemId: Long, files: List<File>): List<AttachmentDto> {
    val parts = files.map { file ->
        val media = "image/*".toMediaTypeOrNull()
        val body: RequestBody = file.asRequestBody(media)
        // ❌ No hay compresión de imágenes
        MultipartBody.Part.createFormData("files", file.name, body)
    }
    return ApiClient.uploadApi.uploadAttachments(itemId, parts)
}
```

**Problema:** Fotos de 4-8 MB se suben sin comprimir.

**Solución:** Comprimir imágenes antes de subir:
```kotlin
fun compressImage(file: File, maxSize: Int = 1024): File {
    // Usar BitmapFactory + Bitmap.compress()
}
```

---

## 🔐 PROBLEMAS DE SEGURIDAD

### 15. **HTTP SIN SSL (CLEARTEXT TRAFFIC)**
**Severidad:** 🔴 CRÍTICO

**Ubicación:** `AndroidManifest.xml`
```xml
<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config">
```

**Problema:**
- Comunicación sin encriptar
- Vulnerable a ataques Man-in-the-Middle
- Token JWT expuesto en tránsito

**Solución:** Migrar a HTTPS en producción.

---

### 16. **TOKEN EN LOGS**
**Ubicación:** Múltiples archivos con `HttpLoggingInterceptor.Level.BODY`

```kotlin
private val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY // ❌ Loguea tokens y datos sensibles
}
```

**Solución:** Solo en Debug mode:
```kotlin
level = if (BuildConfig.DEBUG) 
    HttpLoggingInterceptor.Level.BODY 
else 
    HttpLoggingInterceptor.Level.NONE
```

---

### 17. **AUTHSTATE GLOBAL MUTABLE**
Ya mencionado en punto #3, pero tiene implicación de seguridad:
- Cualquier clase puede leer/modificar el token
- No hay control de acceso
- Puede ser modificado desde múltiples threads

---

## 🎨 PROBLEMAS DE UX

### 18. **ERRORES NO SE LIMPIAN AUTOMÁTICAMENTE**
**Problema:** Los errores permanecen hasta que se limpian manualmente:

```kotlin
vm.updateError("Error al subir imagen")
// El error se queda en pantalla indefinidamente
```

**Solución:** Auto-limpiar después de 5 segundos o al navegar.

---

### 19. **LOADING STATES INCONSISTENTES**
Algunas pantallas muestran:
- `LinearProgressIndicator`
- Otras muestran texto "Cargando..."
- Otras deshabilitan botones
- Algunas no muestran nada

**Solución:** Componente `LoadingOverlay` unificado.

---

### 20. **SIN CONFIRMACIÓN EN OPERACIONES DESTRUCTIVAS**
**Ubicación:** `deleteSection()`, `deleteItem()`, etc.

No hay diálogo de confirmación antes de eliminar.

---

## 💾 PROBLEMAS DE DATOS

### 21. **SIN SOPORTE OFFLINE**
La app requiere internet para todo:
- No guarda borradores localmente con Room
- Pierde datos si se cierra la app mientras responde checklist
- No hay sincronización pendiente

---

### 22. **PÉRDIDA DE DATOS AL ROTAR PANTALLA**
Los `remember` sin `rememberSaveable` pierden datos:
```kotlin
var tempImageUri by remember { mutableStateOf<Uri?>(null) }
// ❌ Se pierde al rotar pantalla
```

---

## 🧪 PROBLEMAS DE TESTING

### 23. **SIN TESTS UNITARIOS**
No hay archivos de test funcionales (solo los ejemplos por defecto).

---

### 24. **VIEWMODELS NO INYECTABLES**
Difícil de testear porque usan:
```kotlin
class SimpleFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory
```

Debería usar Hilt o Koin para DI.

---

## 📝 RECOMENDACIONES PRIORITARIAS

### 🔥 ACCIÓN INMEDIATA (Esta semana)
1. **Arreglar bug de `scannedBarcode`** - Campos BARCODE no funcionan
2. **Cambiar a HTTPS** - Seguridad crítica
3. **Limpiar logs de producción** - No loguear tokens

### ⚡ CORTO PLAZO (Este mes)
4. Eliminar `AuthState` global
5. Crear `BaseViewModel` para eliminar duplicación
6. Implementar compresión de imágenes
7. Añadir confirmaciones en operaciones destructivas

### 🎯 MEDIANO PLAZO (1-2 meses)
8. Implementar Room Database para offline-first
9. Crear sistema de tipos de error robusto
10. Migrar a Hilt/Koin para DI
11. Añadir tests unitarios básicos
12. Mejorar caché de templates con TTL

### 🌟 LARGO PLAZO (3+ meses)
13. Refactorizar navegación con Router pattern
14. Implementar arquitectura Clean
15. Añadir analytics y crash reporting
16. Modo oscuro completo
17. Soporte para tablets

---

## 🎓 CONCLUSIÓN

La aplicación está **funcionalmente completa** pero tiene:
- ✅ Arquitectura MVVM bien implementada
- ✅ UI moderna con Compose
- ✅ Integración completa con backend
- ⚠️ Varios bugs que pueden causar fallos
- ⚠️ Duplicación de código significativa
- ⚠️ Sin estrategia offline
- 🔴 Problemas de seguridad críticos (HTTP, logs)

**Calificación general: 7/10**
- Funcionalidad: 9/10
- Código limpio: 6/10
- Seguridad: 4/10
- Testing: 2/10
- UX: 7/10


