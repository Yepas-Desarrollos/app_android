# 🎯 FLUJO DE DATOS Y COMPONENTES - Diagrama Detallado

## ESTRUCTURA DE CAPAS Y FLUJO DE DATOS

### 1. FLOW DE DATOS VERTICAL

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                         UI LAYER (Compose)                               ║
║  ┌────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐    ║
║  │LoginScreen │  │HomeScreen    │  │HistoryScreen │  │RunScreen   │    ║
║  │            │  │              │  │              │  │            │    ║
║  │SimpleOpt.* │  │SimpleOpt.*   │  │SimpleOpt.*   │  │ItemsScreen │    ║
║  └──────┬─────┘  └───────┬──────┘  └──────┬───────┘  └─────┬──────┘    ║
║         │                │                │                │            ║
║         └────────────────┼────────────────┼────────────────┘            ║
║                          │                │                             ║
╚══════════════════════════╪════════════════╪═════════════════════════════╝
                           │                │
╔══════════════════════════╪════════════════╪═════════════════════════════╗
║                      PRESENTATION LAYER                                 ║
║              ┌─────────────────────────────────┐                        ║
║              │   VIEWMODELS (Estado)            │                        ║
║              │ ┌─────────────────────────────┐ │                        ║
║              │ │ AuthViewModel               │ │                        ║
║              │ │ - state: StateFlow<Auth>    │ │                        ║
║              │ │ - login()                   │ │                        ║
║              │ └─────────────────────────────┘ │                        ║
║              │ ┌─────────────────────────────┐ │                        ║
║              │ │ RunsViewModel               │ │                        ║
║              │ │ - pendingRunsFlow()         │ │                        ║
║              │ │ - historyRunsFlow()         │ │                        ║
║              │ │ - runItemsFlow()            │ │                        ║
║              │ │ - respond()                 │ │                        ║
║              │ │ - submitRun()               │ │                        ║
║              │ │ - deleteRun()               │ │                        ║
║              │ │ - drafts: Map<Long, Draft> │ │                        ║
║              │ └─────────────────────────────┘ │                        ║
║              │ ┌─────────────────────────────┐ │                        ║
║              │ │ AdminViewModel              │ │                        ║
║              │ │ - templates                 │ │                        ║
║              │ │ - sections                  │ │                        ║
║              │ │ - items                     │ │                        ║
║              │ └─────────────────────────────┘ │                        ║
║              │ ┌─────────────────────────────┐ │                        ║
║              │ │ AssignmentViewModel         │ │                        ║
║              │ │ - runsPendingAssignment     │ │                        ║
║              │ │ - assign()                  │ │                        ║
║              │ └─────────────────────────────┘ │                        ║
║              │ ┌─────────────────────────────┐ │                        ║
║              │ │ ChecklistStructureVM        │ │                        ║
║              │ │ - structure: StateFlow      │ │                        ║
║              │ └─────────────────────────────┘ │                        ║
║              └─────────────────────────────────┘                        ║
║                           │                                            ║
║                           └────────────────┬─────────────────────────┐ ║
║                                            │                         │ ║
╚════════════════════════════════════════════╪═════════════════════════╪═╝
                                             │                         │
╔════════════════════════════════════════════╪═════════════════════════╪═╗
║                       DATA LAYER                                       ║
║                        ┌──────────────────────────────────┐            ║
║                        │      REPO (Lógica)               │            ║
║                        │                                  │            ║
║                        │ ┌────────────────────────────┐  │            ║
║                        │ │ login()                    │  │            ║
║                        │ │ stores()                   │  │            ║
║                        │ │ templates()                │  │            ║
║                        │ │ createRun()                │  │            ║
║                        │ │ runItems()                 │  │            ║
║                        │ │ respond()                  │  │            ║
║                        │ │ submitRun()                │  │            ║
║                        │ │ deleteRun()                │  │            ║
║                        │ │ uploadEvidenceFile()       │  │            ║
║                        │ └────────────────────────────┘  │            ║
║                        └────────┬─────────────────────────┘            ║
║                                 │                                     ║
║  ┌──────────────────────────┐   │   ┌──────────────────────────┐      ║
║  │    TokenStore            │   │   │    AuthState (Singleton) │      ║
║  │ (SharedPreferences)      │   │   │                          │      ║
║  │ - token: String          │   │   │ var token: String?       │      ║
║  │ - roleCode: String       │   │   │ var roleCode: String?    │      ║
║  │ - userId: Long           │   │   │ var userId: Long?        │      ║
║  │ - email: String          │   │   │ var email: String?       │      ║
║  │ - fullName: String       │   │   │ var fullName: String?    │      ║
║  │ - Flows para listeners   │   │   │                          │      ║
║  └──────────────────────────┘   │   └──────────────────────────┘      ║
║                                 │                                     ║
╚═════════════════════════════════╪═════════════════════════════════════╝
                                  │
╔═════════════════════════════════╪═════════════════════════════════════╗
║                     NETWORK LAYER (API)                               ║
║                                 │                                     ║
║                    ┌────────────▼──────────────┐                      ║
║                    │    ApiClient              │                      ║
║                    │ (Retrofit + OkHttp)       │                      ║
║                    │                           │                      ║
║                    │ - BASE_URL                │                      ║
║                    │ - Interceptor (headers)   │                      ║
║                    │ - setToken()              │                      ║
║                    │ - Timeouts                │                      ║
║                    └────────────┬──────────────┘                      ║
║                                 │                                     ║
║                    ┌────────────▼──────────────┐                      ║
║                    │    Api Interface          │                      ║
║                    │ (@GET, @POST, @DELETE)   │                      ║
║                    └────────────┬──────────────┘                      ║
║                                 │                                     ║
╚═════════════════════════════════╪═════════════════════════════════════╝
                                  │
╔═════════════════════════════════╪═════════════════════════════════════╗
║                  BACKEND (API REST)                                   ║
║                                 │                                     ║
║              ┌──────────────────▼──────────────────┐                  ║
║              │   Spring Boot / Nest / Express      │                  ║
║              │   - /api/auth/login                 │                  ║
║              │   - /api/stores                     │                  ║
║              │   - /api/templates                  │                  ║
║              │   - /api/run/create                 │                  ║
║              │   - /api/run/{id}/items             │                  ║
║              │   - /api/run/{id}/respond/{itemId}  │                  ║
║              │   - /api/evidence/upload            │                  ║
║              │   - /api/run/{id}/submit            │                  ║
║              │   - /api/run/{id} DELETE            │                  ║
║              │   - Admin endpoints                 │                  ║
║              └──────────────────┬──────────────────┘                  ║
║                                 │                                     ║
║                    ┌────────────▼──────────────┐                      ║
║                    │    DATABASE               │                      ║
║                    │  (PostgreSQL / MySQL)     │                      ║
║                    │  - users                  │                      ║
║                    │  - templates              │                      ║
║                    │  - sections               │                      ║
║                    │  - items                  │                      ║
║                    │  - runs                   │                      ║
║                    │  - run_items (respuestas) │                      ║
║                    │  - evidence (fotos)       │                      ║
║                    │  - assignments            │                      ║
║                    └───────────────────────────┘                      ║
║                                                                         ║
╚═════════════════════════════════════════════════════════════════════════╝
```

---

## 2. FLUJO DE INTERACCIÓN: USUARIO RESPONDE ITEM

```
┌─────────────┐
│   Usuario   │ ← Hace click en item
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ ItemsScreen (LazyColumn)                │
│ - Muestra item no respondido            │
│ - Botón "Responder"                     │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Navegar a ItemDetailScreen              │
│ - Recibir: runId, itemId                │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ ItemDetailScreen                        │
│ - Mostrar pregunta (item.question)      │
│ - Cargar respuesta anterior (si existe) │
└──────┬──────────────────────────────────┘
       │
       ▼ (Según item.type)
       │
  ┌────┴────┬──────────┬────────┬────────┬────────┐
  │          │          │        │        │        │
  ▼          ▼          ▼        ▼        ▼        ▼
CHOICE    TEXT        NUM      BARCODE PHOTO     DATE
  │          │          │        │        │        │
  ▼          ▼          ▼        ▼        ▼        ▼
Botones   TextField   NumField QR/Text Camera   DatePicker
  │          │          │        │        │        │
  └──────────┴──────────┴────────┴────────┴────────┘
             │
             ▼
    ┌────────────────────┐
    │ Usuario ingresa    │
    │ respuesta          │
    └────────┬───────────┘
             │
             ▼
    ┌────────────────────┐
    │ Click "Guardar"    │
    │ o "Enviar"         │
    └────────┬───────────┘
             │
             ▼
┌─────────────────────────────────────────────────┐
│ RunsViewModel.respond(                         │
│   itemId, status, text, number, barcode       │
│ )                                              │
└────────┬────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ ¿Tiene evidencia requerida?             │
│ (Si item.type == PHOTO)                 │
└────────┬────────────────────────────────┘
         │
    ┌────┴──────┐
    │           │
    ▼           ▼
 NO/SKIP      SÍ
    │           │
    │           ▼
    │  ┌──────────────────────┐
    │  │ upload evidence file │
    │  │ (multipart)          │
    │  │ POST /evidence/upload│
    │  └────────┬─────────────┘
    │           │
    │      ┌────┴────┐
    │      │         │
    │      ▼         ▼
    │    ✅ OK    ❌ FAIL
    │      │         │
    │      │         ▼
    │      │    ┌──────────────────┐
    │      │    │ Guardar en draft │
    │      │    │ + Mostrar error  │
    │      │    │ (retry later)    │
    │      │    └──────────────────┘
    │      │
    │      ▼
    └────────┬───────────────────────────┐
             │                           │
             ▼                           │
    ┌──────────────────────────────────┐│
    │ Repo.respond(                    ││
    │   itemId,                        ││
    │   status (OK/NOK/NA)             ││
    │   text, number, barcode,         ││
    │   evidenceId (si hay foto)       ││
    │ )                                ││
    │                                  ││
    │ POST /api/run/{runId}/respond    ││
    │ /{itemId}                        ││
    └──────────┬───────────────────────┘│
               │                         │
               ▼                         │
    ┌──────────────────────────────────┐│
    │ Backend valida y guarda          ││
    │ run_item.response = respuesta    ││
    │ run_item.answered_at = ahora     ││
    │ run_item.status = OK/NOK/NA      ││
    └──────────┬───────────────────────┘│
               │                         │
           ┌───┴────┐                   │
           │        │                   │
           ▼        ▼                   │
         ✅ OK   ❌ ERROR               │
           │        │                   │
           │        ▼                   │
           │    ┌─────────────────┐    │
           │    │ Mostrar error   │    │
           │    │ en UI           │    │
           │    │ _error Flow     │    │
           │    │ Se limpia en 5s │    │
           │    └─────────────────┘    │
           │                           │
    ┌──────┴───────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│ Actualizar UI                        │
│ - Borrar draft                       │
│ - Mostrar respuesta en item          │
│ - Marcar como respondido (✓)         │
│ - Mostrar fecha/hora respuesta       │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│ Volver a ItemsScreen                 │
│ - Item ahora muestra: ✓ Respondido   │
│ - Usuario puede responder siguiente  │
└──────────────────────────────────────┘
```

---

## 3. CICLO DE VIDA DE PANTALLAS (Activity/Composables)

```
App Inicia
  │
  ├─ MainActivity.onCreate()
  │   └─ setContent { ChecklistTheme {...} }
  │      └─ Cargar ViewModels
  │         └─ AppNavHost(...)
  │
  ├─ TokenStore.tokenFlow.collect()
  │   └─ Cargar token guardado
  │
  ├─ AuthViewModel.validateSavedToken()
  │   ├─ ¿Token válido?
  │   ├─ SÍ → state = authenticated
  │   └─ NO → state = null, ir a LoginScreen
  │
  ▼ NavHost detecta destino
  
  ┌─ ¿Autenticado?
  │  ├─ SÍ → HOME
  │  └─ NO → LOGIN
  │
  ▼ Login Screen (si no autenticado)
  │
  ├─ Usuario ingresa email/password
  ├─ AuthViewModel.login()
  ├─ Backend devuelve token + roleCode
  ├─ Guardar en TokenStore + AuthState
  ├─ Ir a HOME
  │
  ▼ Home Screen
  │
  ├─ Mostrar "Nueva Corrida"
  ├─ Mostrar "Historial"
  ├─ Si isAdmin: mostrar "Admin"
  │
  ├─ Usuario selecciona opción
  │
  ▼ Stores Screen (si "Nueva Corrida")
  │
  ├─ Cargar tiendas
  ├─ Usuario selecciona tienda
  │
  ▼ SimpleOptimizedTemplatesScreen
  │
  ├─ Cargar templates para tienda
  ├─ Usuario selecciona template
  ├─ Botón "Crear Checklist"
  ├─ RunsViewModel.createRun()
  ├─ Backend devuelve runId
  │
  ▼ RunScreen (con runId)
  │
  ├─ Cargar runInfo
  ├─ Cargar runItems (por secciones)
  ├─ ItemsScreen (LazyColumn de items)
  ├─ Usuario selecciona item
  │
  ▼ ItemDetailScreen
  │
  ├─ Mostrar pregunta + tipo
  ├─ Usuario responde
  ├─ Click "Guardar"
  ├─ RunsViewModel.respond()
  ├─ Actualizar item en UI
  ├─ Volver a ItemsScreen
  │
  ├─ Repetir para cada item...
  │
  ▼ RunScreen: Checklist completo
  │
  ├─ Usuario click "Enviar"
  ├─ RunsViewModel.submitRun()
  ├─ Backend: run.status = SUBMITTED
  ├─ Mostrar confirmación
  ├─ Navegar a HistoryScreen
  │
  ▼ SimpleOptimizedHistoryScreen
  │
  ├─ Tab "Enviados"
  ├─ Checklist aparece en lista
  ├─ Usuario puede:
  │  ├─ Ver (de solo lectura)
  │  ├─ Eliminar (si ADMIN)
  │  └─ Re-editar (si permitido)
  │
  └─ Continuar usando app...
```

---

## 4. ESTADOS Y TRANSICIONES

### AuthViewModel.state
```
┌──────────────────────────────────────────────────┐
│          LoginState                              │
│  ┌──────────────────────────────────────────┐   │
│  │ loading: Boolean = false                 │   │
│  │ error: String? = null                    │   │
│  │ authenticated: Authenticated? = null     │   │
│  │ welcomeMessage: String? = null           │   │
│  └──────────────────────────────────────────┘   │
│                                                  │
│  Transiciones:                                   │
│  ├─ login() → loading = true                    │
│  ├─ error? → error = "message", loading = false │
│  ├─ success? → authenticated = auth,             │
│  │             loading = false                  │
│  └─ clearWelcome() → welcomeMessage = null       │
└──────────────────────────────────────────────────┘
```

### RunsViewModel States
```
_loading: Boolean
  ├─ true durante operación API
  └─ false cuando termina

_error: String?
  ├─ null normalmente
  ├─ "error message" si falla
  └─ Se limpia automáticamente en 5s

_pendingRuns: List<RunSummaryDto>
  ├─ Status = "PENDING"
  ├─ Usuario aún edita
  └─ Pueda eliminar/continuar

_historyRuns: List<RunSummaryDto>
  ├─ Status = "SUBMITTED"
  ├─ Usuario puede ver
  └─ ADMIN puede eliminar

_runItems: List<RunItemDto>
  ├─ Items del run actual
  └─ Se cargan cuando se abre run

_drafts: Map<Long, DraftResponse>
  ├─ itemId → respuesta en borrador
  ├─ Se guarda si falla envío
  └─ Se limpia cuando envía exitoso

_uploadingImages: Set<Long>
  ├─ itemIds en proceso de upload
  └─ Para mostrar loading spinner
```

---

## 5. FLUJO DE INYECCIÓN DE DEPENDENCIAS

```
MainActivity
  │
  ├─ TokenStore(context)
  │   └─ SharedPreferences
  │
  ├─ Repo(tokenStore)
  │   └─ Acceso a API
  │
  ├─ ViewModels (SimpleFactory)
  │   ├─ AuthViewModel(repo)
  │   ├─ RunsViewModel(repo)
  │   ├─ AdminViewModel(repo)
  │   ├─ AssignmentViewModel(repo)
  │   └─ ChecklistStructureViewModel(repo)
  │
  ├─ AppNavHost(
  │   authVM, runsVM, adminVM, 
  │   assignmentVM, checklistVM
  │ )
  │
  └─ Todas las screens reciben VMs
     como parámetros
```

---

## 6. FLUJO DE SINCRONIZACIÓN DE ESTADO

```
Usuario hace cambio
  │
  ├─ ViewModel.method()
  │   ├─ Actualizar StateFlow
  │   ├─ Llamar API en background
  │   │ (viewModelScope.launch)
  │   └─ Recolectar en Composable
  │       (collectAsStateWithLifecycle)
  │
  ▼ StateFlow cambió
  │
  ├─ by vm.state.collectAsStateWithLifecycle()
  │   └─ variable reactiva
  │
  ▼ Composable se recompone automáticamente
  │
  └─ UI muestra nuevo estado
```

---

## 7. MAPA DE NAVEGACIÓN

```
┌─────────────────────────────────────────────────────────────┐
│                        NavHost                              │
└─────────────────────────────────────────────────────────────┘
        │
        ├─ LOGIN
        │   ├─ onLoggedIn → HOME { popUpTo(0) }
        │
        ├─ HOME
        │   ├─ onNuevaCorrida → STORES
        │   ├─ onOpenHistory → HISTORY
        │   ├─ onAdminAccess → ADMIN_TEMPLATES (si isAdmin)
        │   ├─ onLogout → LOGIN { popUpTo(0) }
        │
        ├─ STORES
        │   ├─ onStoreSelected(code) → TEMPLATES?storeCode=code
        │   ├─ onAdminAccess → ADMIN_TEMPLATES (si isAdmin)
        │
        ├─ TEMPLATES (storeCode: String)
        │   ├─ onRunCreated(runId) → RUN?runId=runId
        │   ├─ Navegar atrás → STORES
        │
        ├─ RUN (runId: Long)
        │   ├─ Mostrar ItemsScreen
        │   ├─ Completar checklist
        │   ├─ Submit → HISTORY (popUpTo HOME)
        │   ├─ Navegar atrás → HISTORY
        │
        ├─ HISTORY
        │   ├─ onOpenRun(runId) → RUN?runId=runId
        │   ├─ Navegar atrás → HOME
        │
        ├─ ADMIN_TEMPLATES (ADMIN)
        │   ├─ Crear template → ADMIN_TEMPLATE_FORM?id=new
        │   ├─ Editar template → ADMIN_TEMPLATE_FORM?id={id}
        │   ├─ Ver sections → ADMIN_SECTION_FORM?templateId=X
        │   ├─ Ver items → ADMIN_ITEM_FORM?sectionId=X
        │
        └─ Más rutas según funcionalidad...
```

---

## 8. FLUJO DE CACHÉ

```
┌─────────────────────────────────┐
│ Primer acceso a templates       │
└──────────┬──────────────────────┘
           │
           ▼
┌─────────────────────────────────┐
│ ¿cachedTemplates != null?       │
└────┬───────────────────────┬────┘
     │                       │
     ▼                       ▼
   SÍ                      NO
     │                       │
     │          ┌────────────┘
     │          │
     │          ▼
     │  ┌──────────────────────────┐
     │  │ Repo.templatesPaginated()│
     │  │ (page=1, limit=100)      │
     │  │ GET /api/templates       │
     │  └──────────┬───────────────┘
     │             │
     │             ▼
     │  ┌──────────────────────────┐
     │  │ cachedTemplates =        │
     │  │ response.data            │
     │  └──────────┬───────────────┘
     │             │
     └─────────────┴──────────────────┐
                                      │
                                      ▼
                          ┌───────────────────────────┐
                          │ Retornar cachedTemplates  │
                          │ (no hace request nuevas   │
                          │ veces que sea accedido)   │
                          └───────────────────────────┘

Limpieza de caché:
  └─ logout() → cachedTemplates = null
  └─ clearCache() (al cambiar usuario)
```

---

## 9. MATRIZ DE PERMISOS POR ROL

```
┌──────────────┬────────┬────────┬───────────┬──────────────┬────────────┐
│ Acción       │ USER   │ ADMIN  │ MGR_PREV  │ MGR_OPS      │ SUPERVISOR │
├──────────────┼────────┼────────┼───────────┼──────────────┼────────────┤
│Ver checklists│   ✓    │   ✓    │     ✓     │      ✓       │     ✓      │
│ Crear nuevo  │   ✓    │   ✓    │     ✓     │      ✓       │     ✓      │
│ Responder    │   ✓    │   ✓    │     ✓     │      ✓       │     ✓      │
│ Enviar       │   ✓    │   ✓    │     ✓     │      ✓       │     ✓      │
│ Editar propio│   ✓    │   ✓    │     ✓     │      ✓       │     ✓      │
│ Editar otros │   ✗    │   ✓    │     ✗     │      ✗       │     ✗      │
│ Eliminar     │   ✗    │   ✓    │     ✗     │      ✗       │     ✗      │
│ Admin Panel  │   ✗    │   ✓    │     ✗     │      ✗       │     ✗      │
│ Create Templ │   ✗    │   ✓    │     ✗     │      ✗       │     ✗      │
│ Edit Templ   │   ✗    │   ✓    │     ✗     │      ✗       │     ✗      │
│ Asignar      │   ✗    │   ✓    │     ✓     │      ✓       │     ✓      │
│ Ver reportes │   ✗    │   ✓    │     ✓     │      ✓       │     ✓      │
└──────────────┴────────┴────────┴───────────┴──────────────┴────────────┘
```

---

## 10. FLUJO DE MANEJO DE ERRORES

```
Llamada API
  │
  ├─ try { ... } catch (e: Throwable)
  │
  ├─ e instanceof HttpException?
  │   │
  │   ├─ 401 Unauthorized
  │   │   └─ Repo.logout()
  │   │   └─ AuthState.token = null
  │   │   └─ Navegar a LOGIN
  │   │
  │   ├─ 400 Bad Request
  │   │   └─ _error = "Datos inválidos"
  │   │   └─ Mostrar en AlertDialog
  │   │   └─ Usuario puede reintentar
  │   │
  │   ├─ 403 Forbidden
  │   │   └─ _error = "No tiene permisos"
  │   │   └─ Mostrar alerta
  │   │
  │   ├─ 500+ Server Error
  │   │   └─ _error = "Error del servidor"
  │   │   └─ Opción: Reintentar
  │   │
  │
  ├─ e instanceof IOException?
  │   │
  │   └─ Sin conexión a red
  │       └─ Guardar en _drafts
  │       └─ _error = "Sin conexión"
  │       └─ Usuario puede continuar offline
  │       └─ Auto-sincronizar cuando vuelva conexión
  │
  └─ Desconocido
      └─ _error = e.message
      └─ Log para debugging

Auto-limpieza de error:
  ├─ launch {
  │   delay(5000)  // 5 segundos
  │   _error.value = null
  └─ }
```

---

**Diagrama generado:** 2025-10-24  
**Versión:** 1.0  
**Estado:** ✅ Actualizado

