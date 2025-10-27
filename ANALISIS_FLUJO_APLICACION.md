# 📊 Análisis del Flujo de la Aplicación - App Android Checklists

## 1️⃣ ARQUITECTURA GENERAL

### Estructura de Capas
```
┌─────────────────────────────────────────┐
│   UI LAYER (Compose Screens)            │
│   - Screens (Composables)               │
│   - Components (Subcomponentes)         │
│   - Theme (Diseño y estilos)            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│   PRESENTATION LAYER (ViewModels)       │
│   - AuthViewModel (Autenticación)       │
│   - RunsViewModel (Checklists)          │
│   - AdminViewModel (Administración)     │
│   - AssignmentViewModel (Asignaciones)  │
│   - ChecklistStructureViewModel         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│   DATA LAYER (Repository)               │
│   - Repo (Lógica de datos)              │
│   - TokenStore (Almacenamiento local)   │
│   - AuthState (Estado global)           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│   NETWORK LAYER (API)                   │
│   - ApiClient (Configuración HTTP)      │
│   - Retrofit + OkHttp                   │
│   - Endpoints REST                      │
└─────────────────────────────────────────┘
```

---

## 2️⃣ FLUJO DE AUTENTICACIÓN

### 🔐 Inicio de Sesión (Login Flow)

```
┌─────────────────┐
│  MainActivity   │ → Crea AuthViewModel, RunsViewModel, AdminViewModel
│  onCreate()     │   y carga token guardado desde TokenStore
└────────┬────────┘
         │
         ▼
┌──────────────────────────────────┐
│  AuthViewModel.validateSavedToken│
│  - Verifica si hay token guardado│
│  - Intenta llamar repo.stores()  │
│  - Si es válido → Auto-login     │
│  - Si no es válido → Logout      │
└────────┬─────────────────────────┘
         │
    ┌────┴────┐
    │          │
    ▼          ▼
 ✅ VÁLIDO   ❌ INVÁLIDO
    │          │
    │          ▼
    │      Mostrar LoginScreen
    │          │
    │          ▼
    │    Usuario ingresa credenciales
    │          │
    │          ▼
    │    AuthViewModel.login(email, password)
    │          │
    │          ▼
    │    Repo.login(email, password)
    │          │
    │          ▼
    │    api.login() → Backend
    │          │
    │          ▼
    │    ✅ Recibir:
    │    - access_token
    │    - roleCode (ADMIN, MGR_PREV, etc)
    │    - userId, email, fullName
    │          │
    │          ▼
    │    TokenStore.save(auth)
    │    AuthState.token = token
    │    AuthState.roleCode = roleCode
    │    ApiClient.setToken(token)
    │          │
    │          ▼
    │    Navegar a HOME
    │          │
    └──────────┴──────────────────────────┐
               │                          │
               ▼                          │
    ┌──────────────────────┐              │
    │  AppNavHost          │◄─────────────┘
    │  Verificar roleCode  │
    │  isAdmin = ...       │
    └──────────┬───────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
 ADMIN        NO ADMIN (Usuario normal)
    │                     │
    │                     ▼
    │            ┌────────────────┐
    │            │  HomeScreen    │
    │            │  - Ver borradores
    │            │  - Ver enviados
    │            │  - Crear nuevo
    │            └────────────────┘
    │
    ▼
 ┌────────────────────┐
 │ HomeScreen (Admin) │
 │ + Botón Admin      │
 └────────────────────┘
```

### 📋 Estados de Autenticación (AuthState)
```kotlin
object AuthState {
    var token: String? = null              // JWT del backend
    var roleCode: String? = null           // ADMIN, MGR_PREV, MGR_OPS, USER
}
```

### 💾 Almacenamiento Local (TokenStore)
```
SharedPreferences:
├── "auth_token" → String
├── "auth_roleCode" → String
├── "auth_userId" → Long
├── "auth_email" → String
└── "auth_fullName" → String
```

---

## 3️⃣ FLUJO PRINCIPAL DE CHECKLISTS

### 🏠 Pantalla de Inicio (HomeScreen)

```
┌──────────────────────────────┐
│     HomeScreen               │
│  (después de autenticación)  │
└─────────────┬────────────────┘
              │
    ┌─────────┼─────────┐
    │         │         │
    ▼         ▼         ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│ Nueva   │ │Historial│ │  Admin  │
│Corrida  │ │Checklists│ │(si es  │
│         │ │         │ │ ADMIN)  │
└────┬────┘ └────┬────┘ └────┬────┘
     │           │           │
     │           │           ▼
     │           │    Admin Templates/Items
     │           │
     │           ▼
     │    ┌─────────────────────┐
     │    │ SimpleOptimized     │
     │    │ HistoryScreen       │
     │    │ - Tab: Borradores   │
     │    │ - Tab: Enviados     │
     │    │ - Mostrar últimos   │
     │    │   1000 checklists   │
     │    └────┬────────────────┘
     │         │
     │         ▼
     │    Seleccionar checklist
     │         │
     │         ▼
     │    RunScreen (ver/editar)
     │
     ▼
┌──────────────────────────┐
│  StoresScreen            │
│  - Cargar tiendas        │
│  - Lista desplegable     │
└─────────┬────────────────┘
          │
          ▼
┌──────────────────────────┐
│  SimpleOptimized         │
│  TemplatesScreen         │
│  - Cargar templates      │
│  - Mostrar x tienda      │
└─────────┬────────────────┘
          │
          ▼
┌──────────────────────────┐
│  Botón: Crear Checklist  │
│  POST /api/run/create    │
│  (storeCode, templateId) │
└─────────┬────────────────┘
          │
          ▼
┌──────────────────────────┐
│  Recibir: RunId          │
│  Navegar a RunScreen     │
└─────────┬────────────────┘
          │
          ▼
┌──────────────────────────┐
│  RunScreen               │
│  - runId recibido        │
│  - Cargar items          │
│  - Mostrar checklist     │
└──────────────────────────┘
```

---

## 4️⃣ FLUJO DE RESPUESTA DE ITEMS

### 📝 Respondiendo Items (RunScreen → ItemsScreen)

```
┌────────────────────────────┐
│  RunScreen                 │
│  - Mostrar encabezado      │
│  - Cargar runItems         │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  ItemsScreen               │
│  LazyColumn de items       │
│  Según secciones           │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  Por cada item:            │
│  - Mostrar pregunta        │
│  - Mostrar respuesta (si)  │
│  - Campo de entrada        │
│  - Botón editar/responder  │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  Usuario selecciona item   │
│  o hace clic en "Responder"│
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────────┐
│  Navegar a ItemDetailScreen    │
│  (o modal inline si está en    │
│   pantalla)                    │
└─────────┬──────────────────────┘
          │
          ▼
┌─────────────────────────────────┐
│  Mostrar componentes del item:  │
│  - Si es MULTIPLE_CHOICE        │
│    → Botones de opción          │
│  - Si es TEXT                   │
│    → TextField para texto       │
│  - Si es NUMERIC                │
│    → Campo numérico             │
│  - Si es BARCODE                │
│    → Scanner QR + campo texto   │
│  - Si es PHOTO                  │
│    → Cámara / Galería           │
└─────────┬──────────────────────┘
          │
          ▼
┌─────────────────────────────────┐
│  Usuario responde y hace clic   │
│  en "Guardar" o "Enviar"        │
└─────────┬──────────────────────┘
          │
          ▼
┌──────────────────────────────────┐
│  RunsViewModel.respond()         │
│  - Guardar respuesta LOCAL       │
│  - En drafts (si no envía)       │
└─────────┬──────────────────────────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
 ENVÍO     SOLO GUARDAR
   │            │
   ▼            ▼
┌──────────┐  ┌──────────────┐
│ POST API │  │ Actualizar UI│
│/respond/ │  │ y listo      │
│          │  └──────────────┘
└────┬─────┘
     │
     ▼
┌──────────────────────────┐
│ Backend valida y        │
│ almacena respuesta      │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Actualizar estado del    │
│ item localmente          │
│ (mostrar ✓ completado)   │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Usuario sigue con       │
│ siguiente item          │
└──────────────────────────┘
```

### 📤 Flujo de Evidencia (Fotos)

```
┌──────────────────────────┐
│  Item requiere PHOTO     │
│  Usuario hace clic       │
│  "Tomar foto"            │
└──────────┬───────────────┘
           │
    ┌──────┴──────┐
    │             │
    ▼             ▼
 Cámara     Galería
    │             │
    └──────┬──────┘
           │
           ▼
┌──────────────────────────┐
│ Seleccionar imagen       │
│ (bitmap)                 │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ RunsViewModel            │
│ uploadEvidenceFile(...)  │
│ - Crear MultipartBody    │
│ - POST /evidence/upload  │
└──────────┬───────────────┘
           │
    ┌──────┴──────┐
    │             │
    ▼             ▼
 ✅ OK        ❌ ERROR
    │             │
    ▼             ▼
┌──────────┐  ┌────────────┐
│ Obtener  │  │ Mostrar    │
│evidenceId│  │error y     │
│          │  │guardar en  │
│          │  │borradores  │
│          │  │(draft)     │
└────┬─────┘  └────────────┘
     │
     ▼
┌──────────────────────────┐
│ Responder item con       │
│ evidenceId               │
│ POST /respond/{itemId}   │
│ {status, evidenceId}     │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ Item marcado como        │
│ respondido ✓             │
└──────────────────────────┘
```

---

## 5️⃣ FLUJO DE ENVÍO FINAL

### 📨 Enviando Checklist (Submit)

```
┌──────────────────────────────────┐
│  Usuario termina todos items     │
│  (o decide enviar parcial)       │
└─────────────┬────────────────────┘
              │
              ▼
┌──────────────────────────────────┐
│  RunScreen muestra botón         │
│  "Enviar Checklist" (SUBMIT)     │
└─────────────┬────────────────────┘
              │
              ▼
┌──────────────────────────────────┐
│  Usuario hace clic en SUBMIT     │
│  - Validar items requeridos      │
│  - Verificar sin errores         │
└─────────────┬────────────────────┘
              │
              ▼
┌──────────────────────────────────┐
│  RunsViewModel.submitRun(runId)  │
│  - Cambiar estado a SUBMITTED    │
│  - POST /api/run/submit/{runId}  │
└─────────────┬────────────────────┘
              │
        ┌─────┴─────┐
        │           │
        ▼           ▼
    ✅ OK       ❌ ERROR
        │           │
        │           ▼
        │      ┌────────────────────┐
        │      │ Mostrar error      │
        │      │ Mantener en PENDING│
        │      └────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Backend actualiza estado         │
│ run.status = SUBMITTED           │
│ run.submittedAt = ahora          │
│ run.assignedTo = usuario actual  │
└─────────────┬────────────────────┘
              │
              ▼
┌──────────────────────────────────┐
│ Mostrar confirmación             │
│ "✓ Checklist enviado"            │
└─────────────┬────────────────────┘
              │
              ▼
┌──────────────────────────────────┐
│ Navegar de vuelta a HISTORY      │
│ o permitir editar otro           │
└──────────────────────────────────┘
```

---

## 6️⃣ FLUJO DE ADMINISTRACIÓN

### 👤 Admin Panel Flow

```
┌────────────────────────────┐
│  HomeScreen (Admin)        │
│  + Botón "Admin"           │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  AdminTemplateListScreen   │
│  - Listar templates        │
│  - CRUD completo           │
└─────────┬──────────────────┘
          │
    ┌─────┼─────┬─────┐
    │     │     │     │
    ▼     ▼     ▼     ▼
 Crear Editar Ver  Eliminar
    │     │     │     │
    └─────┴─────┴─────┘
          │
          ▼
┌────────────────────────────┐
│  AdminTemplateFormScreen   │
│  - Editar nombre           │
│  - Editar descripción      │
│  - Asignar secciones       │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  AdminSectionFormScreen    │
│  - Crear/editar sección    │
│  - Agregar items           │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  AdminItemFormScreen       │
│  - Tipo: MULTIPLE_CHOICE   │
│  - Tipo: TEXT              │
│  - Tipo: NUMERIC           │
│  - Tipo: BARCODE           │
│  - Tipo: PHOTO             │
│  - Requerido: si/no        │
│  - Orden: índice           │
└────────────────────────────┘
```

### 📊 Panel de Asignaciones

```
┌────────────────────────────┐
│  AssignmentScreen          │
│  (solo para ADMIN)         │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  Listar runs sin asignar   │
│  - Filtrar por estado      │
│  - Mostrar tienda/template │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  Seleccionar usuarios      │
│  disponibles para asignar  │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  POST /assignment          │
│  {runId, userId}           │
└─────────┬──────────────────┘
          │
          ▼
┌────────────────────────────┐
│  Run asignado              │
│  Usuario recibe en su      │
│  lista "Mis asignaciones"  │
└────────────────────────────┘
```

---

## 7️⃣ FLUJO DE HISTORIAL

### 📜 History Screen

```
┌──────────────────────────────┐
│  SimpleOptimizedHistoryScreen│
│  (Optimizado para ≤1000)     │
└─────────┬────────────────────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
┌─────────┐  ┌─────────────┐
│Borradores│  │  Enviados   │
│(PENDING) │  │(SUBMITTED)  │
└────┬─────┘  └──────┬──────┘
     │               │
     ▼               ▼
┌─────────────────────────────┐
│ LazyColumn items            │
│ - Mostrar card por run      │
│ - Template name             │
│ - Store code                │
│ - Progress bar              │
│ - Status badge              │
│ - Fecha actualización       │
│ - Quién respondió (si)      │
│ - Botones: Ver/Eliminar     │
└─────────┬───────────────────┘
          │
    ┌─────┴──────┐
    │            │
    ▼            ▼
  Ver       Eliminar
    │            │
    ▼            ▼
┌────────┐  ┌─────────────┐
│Run     │  │ Confirmar   │
│Screen  │  │ Dialog      │
│        │  │ DELETE API  │
│        │  │ /run/{id}   │
└────────┘  └─────────────┘
```

---

## 8️⃣ CACHE Y ESTADO LOCAL

### 🗄️ Sistemas de Cache

#### **Caché Global (RunsViewModel)**
```kotlin
private val _templates = MutableStateFlow<List<TemplateDto>>
private val _runItems = MutableStateFlow<List<RunItemDto>>
private val _runInfo = MutableStateFlow<RunInfoDto?>
private val _pendingRuns = MutableStateFlow<List<RunSummaryDto>>
private val _historyRuns = MutableStateFlow<List<RunSummaryDto>>
```

#### **Borradores Locales (Drafts)**
```kotlin
data class DraftResponse(
    val status: String? = null,
    val text: String? = null,
    val number: Double? = null
)

private val _drafts = MutableStateFlow<Map<Long, DraftResponse>>
```
- Se guardan cuando hay error de envío
- Se borran después de envío exitoso
- Permiten no perder datos si falla la red

#### **TokenStore (SharedPreferences)**
```
- Token persistente
- RoleCode persistente
- userId, email, fullName persistentes
```

---

## 9️⃣ MANEJO DE ERRORES

### ⚠️ Estrategia de Errores

```
┌──────────────────────────────┐
│  Llamada API                 │
└──────────┬───────────────────┘
           │
    ┌──────┴──────┐
    │             │
    ▼             ▼
  ✅ OK       ❌ ERROR
    │             │
    │             ▼
    │         ┌────────────────────┐
    │         │ HttpException      │
    │         │ (4xx, 5xx)         │
    │         └────┬───────────────┘
    │             │
    │         ┌───┴──────┬─────────┐
    │         │          │         │
    │         ▼          ▼         ▼
    │      401       400-403   500+
    │   (Token         (Bad    (Server
    │   inválido)   Request)   Error)
    │         │          │         │
    │         ▼          ▼         ▼
    │      Logout   Show error  Retry
    │         │        msg      │
    │         │          │      ▼
    │         │          │   [Reintentar
    │         │          │    en 5s]
    │         │          │
    ▼         ▼          ▼
┌────────────────────────────────┐
│ _error = StateFlow<String?>    │
│ Mostrar en AlertDialog/Card    │
│ Se limpia después de 5s        │
└────────────────────────────────┘
```

### 🛡️ Tipos de Errores Comunes

| Error | Causa | Manejo |
|-------|-------|--------|
| 401 Unauthorized | Token expirado | Auto-logout |
| 400 Bad Request | Datos inválidos | Mostrar en formulario |
| 403 Forbidden | No tiene permisos | Mostrar alerta |
| 500 Internal Server Error | Fallo backend | Retry con delay |
| Network Error | Sin conexión | Guardar en drafts |

---

## 🔟 FLUJOS ESPECIALES

### 🔄 Sincronización de Estado

```
┌──────────────────────────────┐
│  Usuario hace cambio         │
│  (responde, elimina, etc)    │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  ViewModel actualiza STATE   │
│  (MutableStateFlow)          │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  Compose recomposición       │
│  Actualiza UI automáticamente│
└──────────────────────────────┘
```

### 🔌 Flujo de Configuración (Config)

```
AppConfig.kt
├── ENABLE_PAGINATION_OPTIMIZATIONS = true
│   → Usa SimpleOptimized screens
│   → Carga limit = 1000 max
│
├── Backend URL
├── Timeouts
├── Headers personalizados
```

### 🚀 Flujo de Inicio de Sesión con Token Guardado

```
App inicia
    │
    ▼
MainActivity.onCreate()
    │
    ▼
TokenStore.tokenFlow.collect()
    │
    ▼
AuthState.token = savedToken
AuthState.roleCode = savedRole
    │
    ▼
AuthViewModel.validateSavedToken()
    │
    ▼
Llamar repo.stores()
    │
    ┌────┴────┐
    │         │
    ▼         ▼
  ✅ OK   ❌ FAIL
    │         │
    │         ▼
    │      Logout
    │      Mostrar LoginScreen
    │
    ▼
Auto-login exitoso
Ir a HomeScreen
(sin requerir input usuario)
```

---

## 1️⃣1️⃣ DIAGRAMA DE ROLES Y PERMISOS

### 👥 Control de Acceso por Rol

```
┌────────────────────────────────┐
│  Rol de Usuario                │
└────────┬───────────────────────┘
         │
    ┌────┼────┬──────┬──────┐
    │    │    │      │      │
    ▼    ▼    ▼      ▼      ▼
  USER ADMIN MGR_PREV MGR_OPS SUPERVISOR
    │     │    │       │        │
    │     │    │       │        │
    ▼     ▼    ▼       ▼        ▼
   Ver   Todo  Ver    Ver      Ver
  Mis    Admin  &      &       Reportes
  Runs   Panel  Crear Asignar
         Crear
         Edit.
         Items
```

### 🔓 Validaciones de Acceso

```kotlin
// En AppNavHost
val isAdmin = currentRoleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")

// En SimpleOptimizedHistoryScreen
val canDeleteSubmitted = AuthState.roleCode == "ADMIN"

// Navegar solo si:
if (isAdmin) {
    onAdminAccess?.invoke()  // Acceso a Admin Templates
}
```

---

## 1️⃣2️⃣ OPTIMIZACIONES IMPLEMENTADAS

### ⚡ Performance

| Optimización | Descripción | Beneficio |
|--------------|-------------|-----------|
| Pagination | Cargar templates/runs en páginas | Reducir memoria |
| Lazy Loading | LazyColumn en lugar de Column | Renderizar solo visible |
| Caché local | Guardar templates/runs | Evitar re-fetches |
| Drafts | Guardar respuestas locales | No perder datos |
| Key en LazyColumn | key = { it.id } | Evitar recomposición |

### 🎨 UI/UX

| Mejora | Descripción |
|--------|-------------|
| Simplified Screens | Versiones optimizadas (SimpleOptimized*) |
| Progress Bar | Mostrar avance del checklist |
| Status Badges | Visualizar estado (Borrador/Enviado) |
| Icons | Iconografía clara (Edit, Delete, Info) |
| Error Cards | Errores en cards prominentes |
| Loading Spinners | Indicadores de carga |

---

## 1️⃣3️⃣ PUNTOS DE INTEGRACIÓN API

### 📡 Endpoints Principales

```
POST   /api/auth/login                    → Autenticación
GET    /api/stores                        → Listar tiendas
GET    /api/templates?page=1&limit=20     → Templates paginados
POST   /api/run/create                    → Crear checklist
GET    /api/run/{runId}/items             → Items del checklist
POST   /api/run/{runId}/respond/{itemId}  → Responder item
POST   /api/evidence/upload               → Upload foto
POST   /api/run/{runId}/submit            → Enviar checklist
DELETE /api/run/{runId}                   → Eliminar checklist

ADMIN ENDPOINTS:
POST   /api/admin/template                → Crear template
PUT    /api/admin/template/{id}           → Editar template
DELETE /api/admin/template/{id}           → Eliminar template
POST   /api/admin/section                 → Crear sección
PUT    /api/admin/section/{id}            → Editar sección
POST   /api/admin/item                    → Crear item
PUT    /api/admin/item/{id}               → Editar item
POST   /api/assignment                    → Asignar checklist
```

---

## 1️⃣4️⃣ CICLO DE VIDA DEL CHECKLIST

### 📅 Estados del Checklist

```
PENDING (Borrador)
    │ Usuario responde items
    │ Guarda borradores
    ▼
PENDING (Con respuestas parciales)
    │ Usuario hace clic "Enviar"
    ▼
SUBMITTED (Enviado)
    │ Ahora visible en Historial
    │ Estado: de lectura (solo ver)
    │ O permitir re-edición si permisos
    ▼
ARCHIVED (Opcional, después de tiempo)
```

### 🔄 Transiciones de Estado

```
PENDING ──[Submit]──> SUBMITTED
   ↑                      │
   └──[Edit]──[Save]──────┘ (si permitido)

PENDING ──[Delete]──> (eliminado)
SUBMITTED ──[Delete]──> (eliminado, si ADMIN)
```

---

## 1️⃣5️⃣ FLUJO COMPLETO: DE INICIO A ENVÍO

```
┌──────────────────────────────────────────────────────────────────┐
│                    FLUJO COMPLETO                                 │
└──────────────────────────────────────────────────────────────────┘

1. APP INICIA
   ▼
2. MainActivity carga token guardado
   ▼
3. ¿Token válido?
   ├─ SÍ → Auto-login, ir a HomeScreen
   └─ NO → Mostrar LoginScreen
   ▼
4. USUARIO INGRESA CREDENCIALES
   ▼
5. AuthViewModel.login() → Repo.login() → Backend
   ▼
6. ✓ Recibir token + roleCode
   ▼
7. Guardar en TokenStore + AuthState + ApiClient
   ▼
8. Ir a HomeScreen
   ▼
9. USUARIO SELECCIONA "NUEVA CORRIDA"
   ▼
10. StoresScreen → Seleccionar tienda
   ▼
11. SimpleOptimizedTemplatesScreen → Seleccionar template
   ▼
12. RunsViewModel.createRun() → Backend crea run
   ▼
13. ✓ Recibir runId
   ▼
14. Navegar a RunScreen con runId
   ▼
15. Cargar runItems del servidor
   ▼
16. ItemsScreen muestra items por sección
   ▼
17. USUARIO RESPONDE ITEMS (Loop)
    ├─ Selecciona item
    ├─ Abre ItemDetailScreen
    ├─ Ingresa respuesta
    ├─ Hace click "Guardar"
    ├─ RunsViewModel.respond() → Backend
    ├─ ✓ Item actualizado
    └─ Vuelve a ItemsScreen
   ▼
18. USUARIO TERMINA Y HACE CLICK "ENVIAR"
   ▼
19. RunsViewModel.submitRun() → Backend
   ▼
20. ✓ run.status = SUBMITTED
   ▼
21. Mostrar confirmación
   ▼
22. Navegar a HistoryScreen
   ▼
23. Checklist aparece en Tab "Enviados"
   ▼
24. ✓ FLUJO COMPLETADO
```

---

## 📝 NOTAS IMPORTANTES

1. **AuthState es Global**: Accesible desde cualquier composable sin pasar por parámetros
2. **ViewModels usan Repo**: La única capa que habla con la API es Repo
3. **Drafts = Recuperación**: Si falla envío, las respuestas se guardan localmente
4. **TokenStore = Persistencia**: Permite auto-login sin credenciales
5. **Optimizations = Config**: Se activan/desactivan desde AppConfig.ENABLE_PAGINATION_OPTIMIZATIONS
6. **Roles = Acceso**: Solo ADMIN/MGR_* acceden a AdminPanel
7. **Paginación = Escalabilidad**: SimpleOptimized carga hasta 1000 registros
8. **Errores = Manejo Robusto**: 401 → logout, 500 → retry, network → draft

---

**Generado:** 2025-10-24  
**Versión de la App:** Android Checklist v1.0  
**Framework:** Jetpack Compose + Kotlin Coroutines

