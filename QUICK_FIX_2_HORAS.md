# ⚡ QUICK FIX: EMPEZÁ AHORA (1-2 horas)

Aquí está el código que DEBES cambiar HOY. No requiere refactorización completa.

---

## 1️⃣ PASO 1: ENCRIPTAR TOKENS (45 minutos)

### 1. Agregar dependencia

**Archivo: `build.gradle.kts` (en la sección `dependencies`)**

```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

Sincronizar Gradle.

### 2. Reemplazar TokenStore.kt completo

**Archivo: `app/src/main/java/mx/checklist/data/TokenStore.kt`**

```kotlin
package mx.checklist.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    
    init {
        val savedToken = prefs.getString("token", null)
        _tokenFlow.value = savedToken
    }
    
    fun save(auth: mx.checklist.data.auth.Authenticated) {
        prefs.edit().putString("token", auth.token).apply()
        _tokenFlow.value = auth.token
    }
    
    fun clear() {
        prefs.edit().clear().apply()
        _tokenFlow.value = null
    }
    
    fun getToken(): String? = _tokenFlow.value
}
```

✅ **LISTO. Los tokens ahora están encriptados.**

---

## 2️⃣ PASO 2: REMOVER LOGS SENSIBLES (15 minutos)

### En `Repo.kt`

Reemplazar esto:
```kotlin
// ❌ ACTUAL (ELIMINAR)
Log.d("Repo", "🌐 Backend response - access_token: ${res.access_token?.take(20)}...")
Log.d("Repo", "🌐 Backend response - roleCode: ${res.roleCode}")
Log.d("Repo", "📦 Authenticated object - token: ${auth.token.take(20)}...")
Log.d("Repo", "📦 Authenticated object - roleCode: ${auth.roleCode}")
Log.d("Repo", "💾 TokenStore.save() llamado con auth")
```

Por esto:
```kotlin
// ✅ NUEVO (SEGURO)
if (BuildConfig.DEBUG) {
    Log.d("Repo", "✅ Login successful")
}
```

✅ **LISTO. Tokens no se loguean.**

---

## 3️⃣ PASO 3: CREAR ERROR BANNER REUTILIZABLE (20 minutos)

### Crear nuevo archivo

**Archivo: `app/src/main/java/mx/checklist/ui/components/ErrorBanner.kt`**

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
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
```

### Reemplazar en SimpleOptimizedHistoryScreen.kt

Buscar:
```kotlin
// ❌ ACTUAL
error?.let { errorMsg ->
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "Error: $errorMsg",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
```

Reemplazar con:
```kotlin
// ✅ NUEVO
ErrorBanner(
    error = error,
    onDismiss = { runsVM.clearError() }
)
```

**Necesitas agregar import:**
```kotlin
import mx.checklist.ui.components.ErrorBanner
```

✅ **LISTO. Componente reutilizable.**

---

## 4️⃣ PASO 4: CENTRALIZAR CONVERSIÓN DE FECHAS (30 minutos)

### Crear nuevo archivo

**Archivo: `app/src/main/java/mx/checklist/utils/DateUtils.kt`**

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
            
            val date = try {
                utcFormat.parse(isoClean)
            } catch (e: Exception) {
                utcFormatNoMs.parse(isoClean)
            }
            
            date?.let { localFormat.format(it) } ?: iso
        } catch (e: Exception) {
            iso
        }
    }
}
```

### Usar en SimpleOptimizedHistoryScreen.kt

Buscar el bloque LARGO:
```kotlin
// ❌ ACTUAL (REMOVER ESTE BLOQUE COMPLETO DE 20+ LÍNEAS)
val formattedDate = remember(run.updatedAt) {
    try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        inFmt.timeZone = TimeZone.getTimeZone("UTC")
        // ... 15 líneas más ...
    }
}
```

Reemplazar con:
```kotlin
// ✅ NUEVO (UNA LÍNEA)
val formattedDate = DateUtils.isoUtcToLocalDateTime(run.updatedAt)
```

Agregar import:
```kotlin
import mx.checklist.utils.DateUtils
```

✅ **LISTO. Código limpio y reutilizable.**

---

## 5️⃣ PASO 5: LIMITAR CACHÉ DE IMÁGENES (20 minutos)

### En MainActivity.kt

Agregar ANTES de `setContent`:

```kotlin
// Agregar después de: val tokenStore = TokenStore(this)
// ✅ NUEVO
val imageLoader = coil.ImageLoader.Builder(this)
    .memoryCache {
        coil.memory.MemoryCache(
            maxSizeBytes = 50 * 1024 * 1024  // 50MB
        )
    }
    .diskCache {
        coil.disk.DiskCache.Builder()
            .maxSizePercent(0.02)  // 2% del storage
            .directory(java.io.File(cacheDir, "image_cache"))
            .build()
    }
    .build()

coil.Coil.setImageLoader(imageLoader)
```

Agregar imports:
```kotlin
import coil.ImageLoader
import coil.Coil
import coil.memory.MemoryCache
import coil.disk.DiskCache
import java.io.File
```

✅ **LISTO. No habrá crashes por falta de memoria.**

---

## ✅ VERIFICACIÓN: ¿TERMINASTE TODO?

Checklist rápido:

```
[ ] 1. Dependencia EncryptedSharedPreferences agregada
[ ] 2. TokenStore.kt reemplazado con encriptación
[ ] 3. Logs sensibles removidos de Repo.kt
[ ] 4. ErrorBanner.kt creado
[ ] 5. ErrorBanner usado en SimpleOptimizedHistoryScreen.kt
[ ] 6. DateUtils.kt creado
[ ] 7. DateUtils usado en SimpleOptimizedHistoryScreen.kt
[ ] 8. ImageLoader configurado en MainActivity.kt
[ ] 9. Gradle sincronizado
[ ] 10. App compila y abre sin crashes
```

---

## 🧪 TESTING RÁPIDO

1. **Build y run:** `Shift + F10`
2. **Intenta**: Login → Historial → Scroll
3. **Verifica**:
   - ✅ No hay crashes
   - ✅ Errores se muestran bien
   - ✅ Fechas formateadas correctamente
   - ✅ Imágenes cargan sin lag

---

## ⏱️ TIEMPO TOTAL

```
Paso 1 (Tokens):       45 min
Paso 2 (Logs):         15 min
Paso 3 (ErrorBanner):  20 min
Paso 4 (Fechas):       30 min
Paso 5 (Caché):        20 min
Sync + Testing:        20 min
               ─────────────
TOTAL:        2 horas 30 min
```

---

## 🎉 RESULTADO FINAL

Después de estos cambios:

✅ **Seguridad:** Tokens encriptados  
✅ **Mantenimiento:** Código duplicado eliminado  
✅ **UX:** Errores claros  
✅ **Performance:** Sin crashes por imagen  
✅ **Logs:** Seguros  

**App lista para PRODUCCIÓN en estos aspectos básicos.**

---

## 📚 PRÓXIMOS PASOS (La próxima semana)

Después de completar esto, lee:
1. `ROADMAP_ARQUITECTURA_HILT.md` (para arquitectura)
2. `SOLUCIONES_PRACTICAS_INMEDIATAS.md` (para más mejoras)
3. `ANALISIS_COMPLETO_PROYECTO.md` (referencia)

---

**¡Ahora mismo: Los 5 pasos, 2.5 horas, major improvement!** 🚀

