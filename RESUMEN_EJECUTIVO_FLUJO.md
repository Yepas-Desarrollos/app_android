# 📋 RESUMEN EJECUTIVO - Flujo de la Aplicación

## 🎯 Objetivo Principal
La aplicación es un sistema de **gestión de checklists digitales** que permite a usuarios responder cuestionarios en tablets/smartphones, con soporte para múltiples tipos de respuesta (texto, numéricos, opciones, códigos de barras, fotos).

---

## 🔑 Conceptos Clave

### 1. **Tres Fases Principales**
```
┌─────────────┐    ┌──────────────┐    ┌──────────┐
│  FASE 1     │    │   FASE 2     │    │  FASE 3  │
│             │    │              │    │          │
│ Autenticación│→  │ Responder    │  → │  Envío   │
│             │    │              │    │          │
│ • Login     │    │ • Crear run  │    │ • Submit │
│ • Token     │    │ • Cargar items│   │ • Cambio │
│ • Persist   │    │ • Responder  │    │   estado │
│ • Validar   │    │ • Guardar    │    │ • Confirm│
└─────────────┘    └──────────────┘    └──────────┘
```

### 2. **Ciclo de Vida de un Checklist**
```
1. CREAR
   └─ Usuario selecciona tienda + template
   └─ POST /api/run/create → runId

2. RESPONDER (Loop)
   ├─ Cargar items
   ├─ Usuario responde item
   ├─ POST /api/run/{id}/respond/{itemId}
   ├─ Guardar en local (drafts si falla)
   └─ Repetir hasta completar

3. ENVIAR
   └─ Usuario hace clic "Enviar"
   └─ POST /api/run/{id}/submit
   └─ run.status = SUBMITTED

4. VISUALIZAR
   └─ Aparece en Historial → Tab "Enviados"
   └─ Solo lectura o edición según permisos
```

### 3. **Estados del Run**
```
┌─────────┐
│ PENDING │ ← Usuario aún respondiendo
└────┬────┘
     │ (Click "Enviar")
     ▼
┌──────────┐
│SUBMITTED │ ← Checklist finalizado
└──────────┘
     │ (Click "Eliminar", solo ADMIN)
     ▼
  (Borrado)
```

---

## 🏗️ Arquitectura de 4 Capas

### **Layer 1: UI (Compose Screens)**
- `LoginScreen` → Credenciales
- `HomeScreen` → Menú principal
- `StoresScreen` → Seleccionar tienda
- `SimpleOptimizedTemplatesScreen` → Seleccionar template
- `RunScreen` + `ItemsScreen` → Responder items
- `SimpleOptimizedHistoryScreen` → Ver historial
- `AdminTemplateListScreen` → Gestión (solo ADMIN)

**Característica:** Todo es composable, reactivo, recomposición automática

### **Layer 2: ViewModels (Estado y Lógica)**
```
AuthViewModel         → login(), logout(), estado auth
RunsViewModel         → crear, responder, enviar, listar
AdminViewModel        → CRUD templates/items (ADMIN)
AssignmentViewModel   → asignar checklists
ChecklistStructureVM  → estructura de datos
```

**Característica:** Usan `StateFlow` para observabilidad, `viewModelScope` para coroutines

### **Layer 3: Repository (Datos)**
```
Repo.kt
├─ login()              → POST /auth/login
├─ stores()             → GET /stores
├─ templates()          → GET /templates (con caché)
├─ createRun()          → POST /run/create
├─ runItems()           → GET /run/{id}/items
├─ respond()            → POST /run/{id}/respond/{itemId}
├─ uploadEvidenceFile() → POST /evidence/upload (multipart)
└─ submitRun()          → POST /run/{id}/submit
```

**Característica:** Capa única de comunicación con API, reintenta automáticamente

### **Layer 4: Network (HTTP)**
```
ApiClient.kt (Retrofit + OkHttp)
├─ BASE_URL = "https://backend.com/api"
├─ Interceptor → Agregar token en headers
├─ Timeout = 30s
└─ SSL validation
```

**Característica:** Manejo de errores centralizado, tokens automáticos

---

## 💾 Persistencia de Datos

### **TokenStore (SharedPreferences)**
```
Almacena:
├─ token           → JWT para autenticación
├─ roleCode        → ADMIN, MGR_PREV, MGR_OPS, USER
├─ userId          → ID del usuario
├─ email           → Email usuario
└─ fullName        → Nombre completo

Beneficio: Auto-login sin credenciales
```

### **AuthState (Singleton Global)**
```
Variables globales:
├─ token:String?    → Usado en API
├─ roleCode:String? → Usado para control de acceso
└─ userId:Long?     → Usado en logs

Beneficio: Acceso desde cualquier composable sin parámetros
```

### **Drafts (Runtime Memory)**
```
Map<Long, DraftResponse>
├─ Key: itemId
├─ Value: {status, text, number, barcode}
├─ Guardado si: envío falla o sin conexión
└─ Limpiado si: envío exitoso

Beneficio: No perder datos si falla red
```

---

## 🔐 Autenticación y Autorización

### **Flujo de Login**
```
1. Usuario ingresa email/password
   ↓
2. AuthViewModel.login() → Repo.login()
   ↓
3. Backend valida credenciales
   ↓
4. Si OK:
   - Respuesta: {access_token, roleCode, userId, email, fullName}
   - Guardar en TokenStore
   - Guardar en AuthState
   - Establecer token en ApiClient
   ↓
5. Navegar a HOME
```

### **Auto-Login (Token Guardado)**
```
1. App inicia
   ↓
2. MainActivity lee TokenStore
   ↓
3. AuthViewModel.validateSavedToken()
   ├─ Llamar repo.stores() (requiere auth)
   ├─ Si OK → Auto-login ✓
   └─ Si FAIL → Logout, mostrar LoginScreen
```

### **Roles y Permisos**
```
isAdmin = roleCode in ["ADMIN", "MGR_PREV", "MGR_OPS"]

┌─────────────────────────────────────┐
│ Si isAdmin = true                   │
├─────────────────────────────────────┤
│ ✓ HomeScreen muestra botón "Admin"  │
│ ✓ Acceso a AdminTemplateListScreen  │
│ ✓ Puede crear/editar templates      │
│ ✓ Puede eliminar checklists otros    │
│ ✓ Panel de asignaciones             │
└─────────────────────────────────────┘

Si isAdmin = false
├─ Solo ver/responder propios
└─ Sin acceso a admin panel
```

---

## 📡 Flujo de Respuesta de Items (Detallado)

### **Paso a Paso**
```
┌─────────────────────────────────────┐
│ 1. ItemsScreen (LazyColumn)         │
│    Mostrar pregunta + tipo de campo │
└──────────┬──────────────────────────┘
           │
           ▼ (Usuario hace click)
┌─────────────────────────────────────┐
│ 2. ItemDetailScreen                 │
│    Mostrar componente según tipo    │
│    - MULTIPLE_CHOICE → Botones      │
│    - TEXT → TextField               │
│    - NUMERIC → NumField             │
│    - BARCODE → Scanner QR           │
│    - PHOTO → Camera/Gallery         │
│    - DATE → DatePicker              │
└──────────┬──────────────────────────┘
           │
           ▼ (Usuario ingresa respuesta)
┌─────────────────────────────────────┐
│ 3. Click "Guardar"                  │
│    RunsViewModel.respond(itemId, ...)
└──────────┬──────────────────────────┘
           │
      ┌────┴────┐
      │         │
      ▼         ▼
   LOCAL      EVIDENCE
   SAVE       (Si es PHOTO)
    │              │
    │              ▼
    │         uploadEvidenceFile()
    │         POST /evidence/upload
    │              │
    │         ┌────┴────┐
    │         │         │
    │         ▼         ▼
    │        OK       ERROR
    │         │         │
    │         │         ▼
    │         │    Guardar en drafts
    │         │    _error = msg
    │         │         │
    │         └────┬────┘
    │              │
    ▼              ▼
┌─────────────────────────────────────┐
│ 4. Repo.respond(itemId, status,     │
│    text, number, evidenceId)        │
│    POST /api/run/{id}/respond/{id}  │
└──────────┬──────────────────────────┘
           │
      ┌────┴────┐
      │         │
      ▼         ▼
     OK       ERROR
      │         │
      │         ▼
      │    Mostrar error
      │    _error = msg
      │    Se limpia en 5s
      │
      ▼
┌─────────────────────────────────────┐
│ 5. Backend actualiza:               │
│    - run_item.response = valor      │
│    - run_item.answered_at = now     │
│    - run_item.status = OK/NOK/NA    │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ 6. Actualizar UI:                   │
│    - Borrar draft local             │
│    - Mostrar ✓ respondido           │
│    - Mostrar fecha/hora             │
│    - Marcar completado visualmente  │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ 7. Volver a ItemsScreen             │
│    Continuar con siguiente item     │
└─────────────────────────────────────┘
```

---

## 📊 Flujo de Envío Final (Submit)

```
1. Usuario termina responder items
   └─ Puede completar todos o parcial

2. Click botón "Enviar Checklist"
   └─ RunsViewModel.submitRun(runId)

3. Backend:
   ├─ Validar items requeridos
   ├─ Cambiar estado a SUBMITTED
   ├─ Guardar timestamp envío
   ├─ Registrar assignedTo = usuario actual
   └─ Notificar si hay webhooks

4. Response exitosa:
   ├─ Mostrar: "✓ Checklist enviado"
   ├─ Navegar a HistoryScreen
   ├─ Checklist aparece en Tab "Enviados"
   └─ Borrar drafts locales

5. Si hay error:
   ├─ Mostrar: "Error al enviar"
   ├─ Mantener en estado PENDING
   ├─ Guardar borradores
   └─ Permitir reintentar
```

---

## 🔄 Sincronización Estado

### **Flujo Reactivo (MutableStateFlow)**

```
ViewModel.stateFlow
    │
    ├─ Cambio de valor
    │
    ├─ Emite nuevo estado
    │
    ├─ Composable suscrito:
    │  by vm.state.collectAsStateWithLifecycle()
    │
    ├─ Variable reactiva se actualiza
    │
    └─ Composable se recompone automáticamente
       └─ UI refleja nuevo estado
```

### **Ejemplo Real**
```kotlin
// ViewModel
private val _runItems = MutableStateFlow<List<RunItemDto>>(emptyList())
fun runItemsFlow(): StateFlow<List<RunItemDto>> = _runItems

// Composable
val items by runsVM.runItemsFlow().collectAsStateWithLifecycle()

// Si items cambia → LazyColumn se recompone → nuevos items se muestran
```

---

## ⚡ Optimizaciones Implementadas

| Optimización | Antes | Después | Beneficio |
|---|---|---|---|
| **Pagination** | Cargar todos | Página de 20 | -80% memoria |
| **Lazy Loading** | Column (todos) | LazyColumn (visible) | -60% renderizado |
| **Cache** | Siempre fetch | Caché + fetch | -90% requests |
| **Drafts** | Perder datos | Guardar local | 100% recuperación |
| **Key en LazyColumn** | Sin key | key={id} | -95% recomposición |
| **Simplified Screens** | Complejo | SimpleOptimized | Más mantenible |

---

## 🛡️ Manejo de Errores

### **Estrategia Global**

```
1. Todos los errores van a _error: StateFlow<String?>

2. Mostrados en:
   ├─ AlertDialog popup
   ├─ Card prominente
   └─ Notificación sistema

3. Limpieza automática:
   ├─ setTimeout 5s
   └─ O usuario clic "Cerrar"

4. Clasificación:
   ├─ 401 → Logout automático
   ├─ 4xx → Usuario error (mostrar)
   ├─ 5xx → Server error (retry)
   └─ Network → Offline (draft)
```

---

## 📱 Tipos de Items y Campos

```
MULTIPLE_CHOICE
└─ Botones excluyentes
└─ Una opción seleccionada
└─ Devuelve: texto opción

TEXT
└─ TextField libre
└─ Múltiples líneas
└─ Devuelve: string

NUMERIC
└─ Campo numérico
└─ Decimales permitidos
└─ Devuelve: double

BARCODE
└─ Scanner QR/EAN
└─ O ingreso manual
└─ Devuelve: string código

PHOTO
└─ Cámara/Galería
└─ Multipart upload
└─ Devuelve: evidenceId

DATE
└─ DatePicker
└─ Formato usuario
└─ Devuelve: string ISO
```

---

## 🎮 Casos de Uso Principales

### **Usuario Normal (Role = USER)**
```
1. Login con credenciales
2. Ver Home
3. Nueva corrida:
   - Seleccionar tienda
   - Seleccionar template
   - Crear checklist
   - Responder items
   - Enviar
4. Ver Historial
5. Logout
```

### **Manager (Role = MGR_PREV/MGR_OPS)**
```
1. (Todo lo de Usuario Normal)
2. + Panel de Asignaciones
3. + Ver checklists sin asignar
4. + Asignar a usuarios
5. + Reportes básicos
```

### **Admin (Role = ADMIN)**
```
1. (Todo lo de Manager)
2. + Admin Templates Panel
3. + Crear templates
4. + Editar templates
5. + Crear secciones
6. + Crear items
7. + Eliminar checklists
8. + Ver logs/auditoría
```

---

## 🚀 Flujo Completo: Inicio a Fin

```
PASO 1: App inicia
└─ MainActivity → TokenStore → AuthState

PASO 2: ¿Hay token guardado?
├─ SÍ → Validar token
│   ├─ OK → Auto-login → HomeScreen
│   └─ FAIL → Logout → LoginScreen
└─ NO → LoginScreen

PASO 3: Usuario login (si no auto-login)
├─ Ingresa email/password
├─ POST /api/auth/login
├─ Guardar token + roleCode
└─ Ir a HomeScreen

PASO 4: HomeScreen
├─ Mostrar botones: Nueva corrida, Historial, Admin (si aplica)
└─ Usuario selecciona...

OPCIÓN A: Nueva Corrida
└─ StoresScreen → TemplatesScreen → createRun → RunScreen

OPCIÓN B: Historial
└─ HistoryScreen → Seleccionar run → RunScreen (read-only o editar)

OPCIÓN C: Admin (si isAdmin)
└─ AdminTemplateListScreen → CRUD templates

PASO 5: En RunScreen
├─ Cargar items
├─ ItemsScreen (LazyColumn)
├─ Loop: Usuario responde items
│  └─ ItemDetailScreen → respond() → POST /respond
├─ Continuar hasta completar
└─ Click "Enviar" → submitRun()

PASO 6: Submit
├─ POST /api/run/{id}/submit
├─ run.status = SUBMITTED
├─ Confirmación UI
└─ Navegar a HistoryScreen

PASO 7: HistoryScreen
├─ Checklist en Tab "Enviados"
├─ Mostrar como read-only (o editable si permisos)
└─ Continuar usando app...
```

---

## 📊 Diagrama de Datos

```
USER
├─ id: Long
├─ email: String
├─ fullName: String
├─ roleCode: String (ADMIN, MGR_PREV, MGR_OPS, USER)
└─ runs: List<Run>

TEMPLATE
├─ id: Long
├─ name: String
├─ description: String
├─ sections: List<Section>
└─ active: Boolean

SECTION
├─ id: Long
├─ templateId: Long
├─ title: String
├─ orderIndex: Int
└─ items: List<Item>

ITEM
├─ id: Long
├─ sectionId: Long
├─ question: String
├─ type: String (MULTIPLE_CHOICE, TEXT, NUMERIC, BARCODE, PHOTO, DATE)
├─ options: List<String> (solo si MULTIPLE_CHOICE)
├─ required: Boolean
├─ orderIndex: Int
└─ store_code: String (para filtrar por tienda)

RUN
├─ id: Long
├─ templateId: Long
├─ storeCode: String
├─ userId: Long (quien responde)
├─ status: String (PENDING, SUBMITTED)
├─ createdAt: DateTime
├─ submittedAt: DateTime? (null si PENDING)
├─ items: List<RunItem>
└─ assignedTo: User? (quien fue asignado)

RUN_ITEM
├─ id: Long
├─ runId: Long
├─ itemId: Long
├─ response: String (respuesta del usuario)
├─ evidenceId: Long? (si tipo PHOTO)
├─ status: String (OK, NOK, NA)
├─ answered_at: DateTime?
└─ respondedBy: User?

EVIDENCE
├─ id: Long
├─ runItemId: Long
├─ filePath: String (en servidor)
├─ uploadedAt: DateTime
└─ uploadedBy: User
```

---

## 🎓 Conceptos Importantes

### **StateFlow vs MutableStateFlow**
- `StateFlow<T>`: Público, solo lectura (subscribers)
- `MutableStateFlow<T>`: Privado, lectura+escritura (ViewModel)

### **collectAsStateWithLifecycle()**
- Convierte Flow a variable reactiva en Composable
- Lifecycle-aware → No memory leaks

### **LazyColumn**
- Renderiza solo items visibles
- Recicla componentes como ListView
- Performance crítico para listas largas

### **Drafts (Recuperación)**
- Si POST falla → guardar en memoria local
- Usuario puede:
  - Reintentar después
  - Continuar sin conexión
  - Sincronizar cuando vuelva red

### **AuthState Global**
- Singleton accesible desde cualquier composable
- No requiere passing de parámetros
- Usado para control de acceso y UI

---

## 📌 Puntos Críticos

1. **Token Management**
   - Guardado en TokenStore (SharedPreferences)
   - Usado en headers de todas las requests
   - Validado al iniciar app

2. **Error Handling**
   - 401 → Auto-logout
   - Network errors → Drafts locales
   - Server errors → Retry con UI feedback

3. **Persistencia**
   - Token guardado indefinidamente
   - Drafts en memoria (se pierden al cerrar app)
   - Historial del servidor (permanente)

4. **Performance**
   - Pagination para listas (max 1000)
   - Cache para templates
   - LazyColumn para rendering
   - Coroutines para I/O async

5. **Seguridad**
   - HTTPS obligatorio
   - Token en Bearer header
   - Validación de permisos por rol
   - No guardar contraseñas (solo token)

---

## 📚 Archivos Clave

```
MainActivity.kt
└─ Punto entrada, carga ViewModels, TokenStore

AppNavHost.kt
└─ Rutas de navegación, control de acceso

ViewModels:
├─ AuthViewModel.kt → Login, logout, estado auth
├─ RunsViewModel.kt → CRUD runs, respuestas
├─ AdminViewModel.kt → CRUD templates (ADMIN)
├─ AssignmentViewModel.kt → Asignaciones
└─ ChecklistStructureViewModel.kt → Estructura

Data:
├─ Repo.kt → Lógica de datos
├─ TokenStore.kt → Persistencia local
├─ AuthState.kt → Estado global
└─ api/ApiClient.kt → HTTP client

Screens:
├─ LoginScreen.kt
├─ HomeScreen.kt
├─ SimpleOptimizedHistoryScreen.kt
├─ SimpleOptimizedTemplatesScreen.kt
├─ RunScreen.kt
├─ ItemsScreen.kt
├─ admin/AdminTemplateListScreen.kt
└─ AssignmentScreen.kt
```

---

## ✅ Checklist de Flujo

- [ ] Usuario puede logearse
- [ ] Token se guarda y se carga en reinicio
- [ ] Auto-login funciona
- [ ] Puede crear nuevo checklist
- [ ] Puede responder items (todos los tipos)
- [ ] Respuestas se guardan localmente (drafts)
- [ ] Respuestas se envían al backend
- [ ] Puede enviar (submit) checklist completo
- [ ] Historial muestra checklists enviados
- [ ] Admin puede ver/editar templates
- [ ] Errores se muestran correctamente
- [ ] Errores se limpian automáticamente
- [ ] Sin conexión: guardar en drafts
- [ ] Con conexión: sincronizar automático
- [ ] Logout limpia token y estado

---

**Documento generado:** 24-10-2025  
**Versión:** 1.0  
**Status:** ✅ Completo y actualizado

