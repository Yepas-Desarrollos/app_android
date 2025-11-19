# 🔧 SOLUCIONES PRÁCTICAS INMEDIATAS

Este documento contiene soluciones de código listos para implementar, priorizadas por urgencia.

---

## ⚡ URGENTES (Implementar en 24-48 horas)

### 1. ENCRIPTAR TOKENS EN ALMACENAMIENTO

**Archivo:** `TokenStore.kt`  
**Riesgo:** 🔴 CRÍTICO

**Paso 1:** Agregar dependencia en `build.gradle.kts`

```kotlin
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

**Paso 2:** Reemplazar TokenStore.kt

```kotlin
// ✅ NUEVO TokenStore.kt
package mx.checklist.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mx.checklist.data.auth.Authenticated
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow
    
    private val _authFlow = MutableStateFlow<Authenticated?>(null)
    val authFlow: StateFlow<Authenticated?> = _authFlow
    
    init {
        // Cargar token guardado
        val savedToken = prefs.getString("token", null)
        _tokenFlow.value = savedToken
        
        if (savedToken != null) {
            val savedAuth = prefs.getString("auth_data", null)
            if (savedAuth != null) {
                try {
                    _authFlow.value = Json.decodeFromString(savedAuth)
                } catch (e: Exception) {
                    clear()
                }
            }
        }
    }
    
    fun save(auth: Authenticated) {
        // Guardar token
        prefs.edit().putString("token", auth.token).apply()
        
        // Guardar datos de auth completos
        val authJson = Json.encodeToString(auth)
        prefs.edit().putString("auth_data", authJson).apply()
        
        // Actualizar flows
        _tokenFlow.value = auth.token
        _authFlow.value = auth
    }
    
    fun clear() {
        prefs.edit().clear().apply()
        _tokenFlow.value = null
        _authFlow.value = null
    }
    
    fun getToken(): String? = _tokenFlow.value
    fun getAuth(): Authenticated? = _authFlow.value
}
```

---

### 2. REMOVER LOGGING DE INFORMACIÓN SENSIBLE

**Archivo:** `Repo.kt`  
**Riesgo:** 🟡 ALTO

Reemplazar:
```kotlin
// ❌ ACTUAL
Log.d("Repo", "🌐 Backend response - access_token: ${res.access_token?.take(20)}...")

// ✅ NUEVO
if (BuildConfig.DEBUG) {
    Log.d("Repo", "✅ Token received successfully (masked for security)")
}
```

Agregar a `build.gradle.kts`:
```kotlin
buildTypes {
    debug {
        buildConfigField("Boolean", "LOG_SENSITIVE_DATA", "true")
    }
    release {
        buildConfigField("Boolean", "LOG_SENSITIVE_DATA", "false")
    }
}
```

---

## 🔧 IMPORTANTES (Próximos 7 días)

### 3. CREAR ERROR BANNER REUTILIZABLE

**Archivo:** Crear nuevo `ui/components/ErrorBanner.kt`

```kotlin
package mx.checklist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(
    error: String?,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (error != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessBanner(
    message: String?,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (message != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar mensaje",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
```

**Uso en SimpleOptimizedHistoryScreen.kt:**
```kotlin
// Reemplazar el Card actual con:
ErrorBanner(
    error = error,
    onDismiss = { runsVM.clearError() },
    modifier = Modifier.fillMaxWidth()
)

// Y agregar success banner
SuccessBanner(
    message = success,
    onDismiss = { runsVM.clearSuccess() }
)
```

---

### 4. CENTRALIZAR CONVERSIÓN DE FECHAS

**Archivo:** Crear nuevo `utils/DateUtils.kt`

```kotlin
package mx.checklist.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val utcFormatNoMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val localFormat = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    
    fun isoUtcToLocalDateTime(iso: String): String {
        return try {
            val isoClean = iso.replace("Z", "")
            
            // Intentar con milisegundos primero
            val date = try {
                utcFormat.parse(isoClean)
            } catch (e: Exception) {
                // Si falla, intentar sin milisegundos
                utcFormatNoMs.parse(isoClean)
            }
            
            date?.let { localFormat.format(it) } ?: iso
        } catch (e: Exception) {
            iso  // Retornar formato original si todo falla
        }
    }
    
    fun isToday(iso: String): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = utcFormat.parse(iso.replace("Z", "")) ?: return false
            
            val today = Calendar.getInstance()
            
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }
}
```

**Reemplazar en SimpleOptimizedHistoryScreen.kt:**
```kotlin
// ❌ Remover bloque completo de conversión
// ✅ Agregar:
val formattedDate = remember(run.updatedAt) {
    DateUtils.isoUtcToLocalDateTime(run.updatedAt)
}
```

---

### 5. CACHÉ DE IMÁGENES LÍMITADO

**Archivo:** Modificar `MainActivity.kt`

```kotlin
// Agregar después de setContent {
// En onCreate, antes de setContent
val imageLoader = ImageLoader.Builder(this)
    .memoryCache {
        MemoryCache(
            maxSizeBytes = 50 * 1024 * 1024  // 50MB
        )
    }
    .diskCache {
        DiskCache.Builder()
            .maxSizePercent(0.02)  // 2% del storage
            .directory(File(cacheDir, "image_cache"))
            .build()
    }
    .httpClient {
        OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            ))
            .build()
    }
    .build()

Coil.setImageLoader(imageLoader)
```

---

## 📚 MEJORAS OPCIONALES (Próximas 2 semanas)

### 6. CREAR COMPOSABLE PARA VALIDAR VIEWMODELS

**Archivo:** `ui/components/ViewModelValidation.kt`

```kotlin
@Composable
inline fun <reified VM : ViewModel> RequireViewModel(
    viewModel: VM?,
    errorMessage: String = "ViewModel no puede ser null",
    content: @Composable (VM) -> Unit
) {
    if (viewModel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    } else {
        content(viewModel)
    }
}

// Uso:
RequireViewModel(runsVM) { vm ->
    SimpleOptimizedHistoryScreen(
        runsVM = vm,
        adminVM = adminVM
    )
}
```

---

### 7. AGREGAR RETRY AUTOMÁTICO EN ERRORES DE RED

**Archivo:** Crear `utils/RetryPolicy.kt`

```kotlin
object RetryPolicy {
    suspend inline fun <T> withRetry(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        backoffMultiplier: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = delayMs
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffMultiplier).toLong()
                }
            }
        }
        
        throw lastException ?: Exception("Unknown error")
    }
}

// Uso en Repo.kt:
suspend fun historyRuns(...) = withRetry {
    api.historyRuns(...)
}
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [ ] Encriptar tokens (URGENTE)
- [ ] Remover logging sensible (URGENTE)  
- [ ] Crear ErrorBanner reutilizable (IMPORTANTE)
- [ ] Centralizar DateUtils (IMPORTANTE)
- [ ] Caché de imágenes límitado (IMPORTANTE)
- [ ] ViewModelValidation (OPCIONAL)
- [ ] Retry automático (OPCIONAL)
- [ ] Tests unitarios (IMPORTANTE, próximas semanas)
- [ ] Migrar a Hilt (IMPORTANTE, 2+ semanas)

---

## 🚀 PRÓXIMOS PASOS

1. **Hoy:** Encriptar tokens + remover logging
2. **Mañana:** ErrorBanner + DateUtils
3. **Esta semana:** Caché de imágenes
4. **Próxima semana:** Empezar migration a Hilt
5. **2 semanas:** Suite de tests básica

**Estimado total:** 40-60 horas de trabajo

