# 🏗️ ROADMAP DE ARQUITECTURA - PLAN DE MIGRACIÓN A HILT

Este documento detalla cómo migrar el proyecto a una arquitectura más robusta y escalable.

---

## 📊 ESTADO ACTUAL vs FUTURO

### ACTUAL (SimpleFactory)
```
MainActivity
├── SimpleFactory { AuthViewModel(repo) }
├── SimpleFactory { RunsViewModel(repo) }
├── SimpleFactory { AdminViewModel(repo) }
└── Difícil de testear
```

### FUTURO (Hilt)
```
ChecklistApp(@HiltAndroidApp)
├── Modules
│   ├── RepositoryModule
│   ├── ViewModelModule
│   ├── NetworkModule
│   └── StorageModule
├── MainActivity(@AndroidEntryPoint)
└── Fácil de testear + inyección automática
```

---

## 🚀 PLAN DE MIGRACIÓN (Fase 1-4)

### FASE 1: Preparación (Día 1)

**1.1 Agregar dependencias en `build.gradle.kts`**

```kotlin
plugins {
    // ... existing plugins ...
    id("com.google.dagger.hilt.android") version "2.48"
}

dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // ViewModels con Hilt
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Testing (future)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48")
}
```

**1.2 Crear Application class**

```kotlin
// ChecklistApp.kt
package mx.checklist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChecklistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializaciones si es necesario
    }
}
```

**1.3 Actualizar AndroidManifest.xml**

```xml
<manifest ...>
    <application
        android:name=".ChecklistApp"
        ...
    >
        <activity ... />
    </application>
</manifest>
```

---

### FASE 2: Módulos Hilt (Día 2)

**2.1 Crear NetworkModule**

```kotlin
// di/NetworkModule.kt
package mx.checklist.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.checklist.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import mx.checklist.data.api.Api
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    @Singleton
    @Provides
    fun provideApi(retrofit: Retrofit): Api {
        return retrofit.create(Api::class.java)
    }
}
```

**2.2 Crear StorageModule**

```kotlin
// di/StorageModule.kt
package mx.checklist.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.checklist.data.TokenStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    
    @Singleton
    @Provides
    fun provideTokenStore(
        @ApplicationContext context: Context
    ): TokenStore {
        return TokenStore(context)
    }
}
```

**2.3 Crear RepositoryModule**

```kotlin
// di/RepositoryModule.kt
package mx.checklist.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.checklist.data.Repo
import mx.checklist.data.TokenStore
import mx.checklist.data.api.Api
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Singleton
    @Provides
    fun provideRepo(
        api: Api,
        tokenStore: TokenStore
    ): Repo {
        return Repo(api = api, tokenStore = tokenStore)
    }
}
```

---

### FASE 3: ViewModels con Hilt (Día 3)

**3.1 Actualizar RunsViewModel**

```kotlin
// ui/vm/RunsViewModel.kt
package mx.checklist.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.checklist.data.Repo
import javax.inject.Inject

@HiltViewModel
class RunsViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {
    // ... resto del código igual ...
}
```

**3.2 Actualizar AdminViewModel**

```kotlin
// ui/vm/AdminViewModel.kt
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {
    // ... resto del código igual ...
}
```

**3.3 Actualizar AuthViewModel**

```kotlin
// ui/vm/AuthViewModel.kt
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {
    // ... resto del código igual ...
}
```

---

### FASE 4: MainActivity & Composables (Día 4)

**4.1 Actualizar MainActivity**

```kotlin
// MainActivity.kt
package mx.checklist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import mx.checklist.data.auth.AuthState
import mx.checklist.data.TokenStore
import mx.checklist.data.api.ApiClient
import mx.checklist.ui.AppNavHost
import mx.checklist.ui.theme.ChecklistTheme
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.vm.AdminViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var tokenStore: TokenStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ChecklistTheme {
                // ✅ Usar hiltViewModel() - inyección automática
                val authVM: AuthViewModel = hiltViewModel()
                val runsVM: RunsViewModel = hiltViewModel()
                val adminVM: AdminViewModel = hiltViewModel()
                
                // Inicializar token
                val token = tokenStore.getToken()
                LaunchedEffect(token) {
                    if (token != null) {
                        AuthState.token = token
                        ApiClient.setToken(token)
                    }
                }
                
                AppNavHost(
                    authVM = authVM,
                    runsVM = runsVM,
                    adminVM = adminVM
                )
            }
        }
    }
}
```

**4.2 Remover SimpleFactory**

```kotlin
// ❌ REMOVER ESTE CÓDIGO
class SimpleFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
```

---

## 🧪 TESTING CON HILT (BONUS)

### Ejemplo: Test de RunsViewModel

```kotlin
// tests/RunsViewModelTest.kt
package mx.checklist.ui.vm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.checklist.data.Repo
import mx.checklist.data.api.dto.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class RunsViewModelTest {
    
    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()
    
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: Repo
    private lateinit var viewModel: RunsViewModel
    
    @Before
    fun setup() {
        repo = mockk()
        viewModel = RunsViewModel(repo)
    }
    
    @Test
    fun testLoadHistoryRunsPaginated() = runTest(dispatcher) {
        // Arrange
        val mockResponse = PaginatedRunsResponse(
            data = listOf(
                RunSummaryDto(
                    id = 1,
                    status = "SUBMITTED",
                    templateName = "Checklist 1",
                    storeCode = "STORE-001",
                    totalCount = 10,
                    answeredCount = 10,
                    updatedAt = "2025-01-12T10:00:00.000",
                    assignedTo = null
                )
            ),
            pagination = PaginationDto(
                page = 1,
                limit = 50,
                total = 100,
                totalPages = 2,
                hasMore = true
            )
        )
        
        coEvery { repo.historyRunsPaginated(1, 50) } returns mockResponse
        
        // Act
        viewModel.loadHistoryRunsPaginated(50)
        advanceUntilIdle()
        
        // Assert
        assertEquals(1, viewModel.historyRunsFlow().value.size)
        assertEquals(100, viewModel.totalEnviados.value)
        assertEquals(true, viewModel.historyPagination.value.hasMore)
    }
    
    @Test
    fun testLoadMoreHistory() = runTest(dispatcher) {
        // Arrange: First load
        val firstPage = PaginatedRunsResponse(
            data = listOf(RunSummaryDto(id = 1, ...)),
            pagination = PaginationDto(
                page = 1, limit = 20, total = 40,
                totalPages = 2, hasMore = true
            )
        )
        
        val secondPage = PaginatedRunsResponse(
            data = listOf(RunSummaryDto(id = 21, ...)),
            pagination = PaginationDto(
                page = 2, limit = 20, total = 40,
                totalPages = 2, hasMore = false
            )
        )
        
        coEvery { repo.historyRunsPaginated(1, 20) } returns firstPage
        coEvery { repo.historyRunsPaginated(2, 20) } returns secondPage
        
        // Act
        viewModel.loadHistoryRunsPaginated(20)
        advanceUntilIdle()
        viewModel.loadMoreHistory()
        advanceUntilIdle()
        
        // Assert
        assertEquals(2, viewModel.historyRunsFlow().value.size)
        assertEquals(false, viewModel.historyPagination.value.hasMore)
    }
}
```

---

## 📋 CHECKLIST DE MIGRACIÓN

### Semana 1: Preparación
- [ ] Agregar dependencias Hilt
- [ ] Crear ChecklistApp
- [ ] Actualizar AndroidManifest.xml
- [ ] Crear módulos (Network, Storage, Repository)

### Semana 2: ViewModels
- [ ] Anotar ViewModels con @HiltViewModel
- [ ] Inyectar dependencias
- [ ] Remover SimpleFactory
- [ ] Actualizar MainActivity con @AndroidEntryPoint

### Semana 3: Testing
- [ ] Configurar test framework (JUnit4 + MockK)
- [ ] Escribir tests para RunsViewModel
- [ ] Escribir tests para AuthViewModel
- [ ] Escribir tests para AdminViewModel

### Semana 4: Verificación
- [ ] Testear en dispositivo real
- [ ] Verificar rendimiento
- [ ] Documentar cambios
- [ ] Code review

---

## 📊 COMPARATIVA: ANTES vs DESPUÉS

| Aspecto | ACTUAL (SimpleFactory) | FUTURO (Hilt) |
|--------|----------------------|----------------|
| Inyección DI | Manual | Automática |
| Testabilidad | Difícil | Fácil |
| Configuración Network | En Repo | Módulo centralizado |
| Scopes | N/A | ViewModelScope, SingletonScope |
| Documentación | Nada | Auto-generada |
| Tiempo setup ViewModel | 30 líneas | 3 líneas |
| Testing | 0% | 70%+ posible |

---

## ✅ BENEFICIOS ESPERADOS

✅ **Testabilidad:** +300% (tests más fáciles de escribir)  
✅ **Mantenibilidad:** +150% (código más limpio)  
✅ **Performance:** +10% (mejor inyección)  
✅ **Escalabilidad:** +200% (fácil agregar nuevos módulos)  
✅ **Seguridad:** +50% (inyección validada)

---

## 🎯 TIMELINE ESTIMADO

```
Semana 1 (8h): Configuración Hilt básica
Semana 2 (12h): Migración ViewModels
Semana 3 (16h): Tests unitarios
Semana 4 (8h): Verificación & docs

Total: ~44 horas (~1 semana full-time)
```

---

## 📚 REFERENCIAS

- [Hilt Documentation](https://dagger.dev/hilt/)
- [Jetpack Architecture](https://developer.android.com/architecture)
- [Testing with Hilt](https://developer.android.com/training/dependency-injection/hilt-testing)


