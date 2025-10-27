# 🚀 INICIO RÁPIDO - Lee esto Primero

## 📋 ¿Qué necesitas saber sobre la App?

### La App en 30 Segundos
```
Android Checklist System permite a usuarios responder 
cuestionarios digitales en tablets/smartphones:

• Crear checklists a partir de templates
• Responder preguntas (texto, opciones, números, fotos, etc)
• Subir evidencia (fotos)
• Enviar reportes automáticamente
• Ver historial de checklists
• Control de acceso por roles (USER, MANAGER, ADMIN)
```

---

## 🎯 ¿Por Dónde Empiezo?

### Opción A: "Solo quiero entender rápido"
**Tiempo: 15 minutos**
1. Lee esta página (5 min)
2. Lee **RESUMEN_EJECUTIVO_FLUJO.md** secciones 1-7 (10 min)
3. Listo ✓

### Opción B: "Necesito conocer la app completamente"
**Tiempo: 1-2 horas**
1. Lee **RESUMEN_EJECUTIVO_FLUJO.md** (completo)
2. Lee **ANALISIS_FLUJO_APLICACION.md** (completo)
3. Consulta **DIAGRAMAS_FLUJO_COMPONENTES.md** cuando necesites visualizar
4. Listo ✓

### Opción C: "Soy developer y voy a hacer cambios"
**Tiempo: 2-3 horas**
1. Lee todo lo anterior
2. Lee **GUIA_RAPIDA_TROUBLESHOOTING.md**
3. Ten a mano **INDICE_NAVEGACION_DOCUMENTOS.md** para referencia
4. Listo ✓

### Opción D: "Solo tengo 5 minutos"
**Tiempo: 5 minutos**
1. Lee **CONCEPTOS CLAVE** de esta página
2. Luego busca lo que necesites en el índice
3. ✓

---

## 🔑 Conceptos Clave (Lo Más Importante)

### 1. **3 Fases del Checklist**

```
FASE 1: CREAR          FASE 2: RESPONDER       FASE 3: ENVIAR
┌──────────┐          ┌──────────────┐        ┌──────────┐
│ Usuario  │          │ Usuario      │        │ Usuario  │
│ selecciona│          │ responde     │        │ hace     │
│ tienda + │──────→   │ cada item    │────→  │ clic     │
│ template │          │ (TEXT, PHOTO)│        │ ENVIAR   │
└──────────┘          └──────────────┘        └──────────┘
     ↓                       ↓                      ↓
State: PENDING      State: PENDING       State: SUBMITTED
               (POST a backend)
```

### 2. **4 Capas Arquitectónicas**

```
UI (Composables)
    ↓
ViewModel (Lógica + Estado)
    ↓
Repository (Datos)
    ↓
API (HTTP + Backend)
```

### 3. **3 Roles Principales**

```
👤 USER           👥 MANAGER          🔐 ADMIN
└─ Ver mis      └─ Ver todos      └─ CRUD Templates
  checklists      (asignados)        └─ Gestionar items
└─ Crear        └─ Asignar          └─ Admin Panel
└─ Responder    └─ Reportes         └─ Eliminar
└─ Enviar       └─ Auditoría        └─ Ver logs
```

### 4. **Token & Persistencia**

```
┌─ Usuario login
│  ├─ Email + Password → Backend
│  ├─ Backend responde: Token + RoleCode
│  └─ Guardar en TokenStore (SharedPreferences)
│
└─ App reinicia
   ├─ Cargar Token desde TokenStore
   ├─ Validar token (llamar repo.stores())
   ├─ Si OK → Auto-login (SIN requerir credenciales)
   └─ Si NO → Mostrar LoginScreen
```

### 5. **Flujo de Respuesta**

```
Usuario selecciona item
    ↓
Abre ItemDetailScreen
    ↓
Ingresa respuesta (según tipo)
    ↓
Click "Guardar"
    ↓
RunsViewModel.respond()
    ├─ Guardar en memoria (drafts)
    └─ POST a backend
         ├─ Si OK → Actualizar UI
         └─ Si ERROR → Mantener en drafts (retry después)
```

---

## 📊 La Aplicación de un Vistazo

### Stack Técnico
```
Frontend:  Kotlin + Jetpack Compose + Retrofit + OkHttp
Database:  SharedPreferences (local) + Backend (remota)
API:       REST HTTP/HTTPS
Auth:      JWT (JSON Web Tokens)
State:     StateFlow + Coroutines
```

### Archivos Importantes
```
UI:          screens/ → LoginScreen, HomeScreen, RunScreen, etc
ViewModel:   ui/vm/ → AuthViewModel, RunsViewModel, AdminViewModel
Data:        data/ → Repo.kt, TokenStore.kt, AuthState.kt
Network:     data/api/ → ApiClient.kt, Api.kt
```

### Endpoints Principales
```
POST   /auth/login              → Login
GET    /stores                  → Tiendas
GET    /templates               → Templates
POST   /run/create              → Crear checklist
GET    /run/{id}/items          → Items
POST   /run/{id}/respond/{id}   → Responder item
POST   /evidence/upload         → Subir foto
POST   /run/{id}/submit         → Enviar checklist
DELETE /run/{id}                → Eliminar checklist
```

---

## ⚡ Flujo Usuario Completo (5 minutos)

```
1. APP INICIA
   └─ ¿Token guardado? 
      ├─ SÍ → Auto-login → HomeScreen
      └─ NO → LoginScreen

2. LOGIN (si no auto-login)
   ├─ Ingresa email/password
   ├─ POST /auth/login
   ├─ Guardar token + roleCode
   └─ Ir a HomeScreen

3. HOMESCREEN
   ├─ Mostrar:
   │  ├─ "Nueva Corrida"
   │  ├─ "Historial"
   │  └─ "Admin" (si isAdmin)
   └─ Usuario selecciona...

4. NUEVA CORRIDA
   ├─ StoresScreen → Seleccionar tienda
   ├─ TemplatesScreen → Seleccionar template
   ├─ Click "Crear" → POST /run/create
   └─ Recibir runId → Navegar a RunScreen

5. RUNSCREEN
   ├─ Cargar items
   ├─ ItemsScreen (LazyColumn)
   └─ Para cada item:
      ├─ Click item
      ├─ ItemDetailScreen
      ├─ Responder
      ├─ Click "Guardar"
      ├─ POST /run/{id}/respond/{itemId}
      └─ Volver a ItemsScreen

6. ENVIAR
   ├─ Click "Enviar Checklist"
   ├─ POST /run/{id}/submit
   ├─ run.status = SUBMITTED
   ├─ Mostrar confirmación
   └─ Navegar a HistoryScreen

7. HISTORIAL
   ├─ HistoryScreen
   ├─ Tab "Enviados"
   ├─ Checklist aparece con ✓
   └─ Continuar usando app...

FIN ✓
```

---

## 🔐 Seguridad en Poco Tiempo

```
✅ IMPLEMENTADO:
• HTTPS/TLS obligatorio
• JWT para autenticación
• Token no guardado en claro (mejor: EncryptedSharedPreferences)
• Validación de permisos por rol
• Logout borra token

⚠️  VERIFICAR:
• SSL pinning (en producción)
• Encriptación de SharedPreferences
• Rate limiting en API
• Logs de auditoría
```

---

## 🐛 Si Algo Falla...

```
PROBLEMA               SOLUCIÓN
───────────────────────────────────────────────
"401 Unauthorized"     → Token expirado → Auto-logout
"Checklist no sube"    → Error en respond() → Ver logs
"Foto no sube"         → uploadEvidenceFile() → Validar tamaño
"Admin no ve botón"    → roleCode incorrecto → Validar backend
"App crashea"          → Ver hs_err_pid*.log
"Sin conexión"         → Usar drafts locales → Retry después
```

**Para más problemas:** Ver **GUIA_RAPIDA_TROUBLESHOOTING.md**

---

## 📚 Documentos Disponibles

| Documento | Para quién | Tiempo |
|-----------|-----------|--------|
| **RESUMEN_EJECUTIVO_FLUJO.md** | Todos | 25 min |
| **ANALISIS_FLUJO_APLICACION.md** | Developers | 40 min |
| **DIAGRAMAS_FLUJO_COMPONENTES.md** | Visuales | 20 min |
| **GUIA_RAPIDA_TROUBLESHOOTING.md** | Referencias | 15 min |
| **INDICE_NAVEGACION_DOCUMENTOS.md** | Navegación | 5 min |
| **ANALISIS_360_VISION_COMPLETA.md** | Visión General | 30 min |

---

## 🎯 Siguientes Pasos

### Si eres Developer
```
1. ✅ Lee esta página
2. ✅ Lee RESUMEN_EJECUTIVO_FLUJO.md
3. ✅ Abre el código fuente (MainActivity.kt, AppNavHost.kt)
4. ✅ Entiende la arquitectura
5. ✅ Haz un cambio pequeño para practicar
```

### Si eres PM/Product
```
1. ✅ Lee esta página
2. ✅ Lee RESUMEN_EJECUTIVO_FLUJO.md secciones 1-3
3. ✅ Entiende los 3 roles y casos de uso
4. ✅ Revisa la matriz de control de acceso
5. ✅ Planea features considerando arquitectura
```

### Si eres QA
```
1. ✅ Lee esta página
2. ✅ Ve GUIA_RAPIDA_TROUBLESHOOTING.md
3. ✅ Usa el testing checklist (25 items)
4. ✅ Prueba en múltiples dispositivos
5. ✅ Documenta bugs claramente
```

---

## 💡 Consejos Prácticos

### ✅ Para Dominar Rápido
1. Lee RESUMEN_EJECUTIVO_FLUJO.md + DIAGRAMAS_FLUJO_COMPONENTES.md
2. Abre el código y busca las clases mencionadas
3. Sigue el flujo paso a paso con debugger
4. Implementa un cambio pequeño
5. Listo, ya entiendes ✓

### ✅ Para Code Review
1. Valida que mantenga arquitectura de 4 capas
2. Verifica que use StateFlow correctamente
3. Revisa manejo de errores
4. Valida que respete matriz de permisos
5. Listo ✓

### ✅ Para Debugging
1. Busca en GUIA_RAPIDA_TROUBLESHOOTING.md
2. Si no está, busca el flujo en ANALISIS_FLUJO_APLICACION.md
3. Usa DIAGRAMAS para entender
4. Ve los logs (adb logcat)
5. Listo ✓

---

## 🚀 Cheat Sheet Rápido

```
LOGIN                          CHECKLIST
─────────────────────────────  ────────────────────────────────
authVM.login(email, password)  runsVM.createRun(storeCode, id)
→ Repo.login()                 → POST /run/create
→ Guardar token                → Obtener runId
→ Auto-login en reinicio       → runsVM.loadRunItems(runId)
                               → Mostrar ItemsScreen

RESPONDER ITEM                 ENVIAR
─────────────────────────────  ────────────────────────────────
runsVM.respond(itemId, ...)    runsVM.submitRun(runId)
→ Guardar localmente           → POST /run/{id}/submit
→ POST /respond/               → Cambiar status a SUBMITTED
→ Si falla: guardar en drafts  → Mostrar confirmación
→ Si OK: actualizar UI         → Navegar a HistoryScreen

LOGOUT                         ADMIN
─────────────────────────────  ────────────────────────────────
authVM.logout()                AdminViewModel.createTemplate(...)
→ TokenStore.clear()           → AdminTemplateFormScreen
→ AuthState.token = null       → POST /admin/template
→ LoginScreen                  → CRUD templates/items
```

---

## 📞 ¿Preguntas?

```
"¿Cómo funciona X?"
→ Busca en INDICE_NAVEGACION_DOCUMENTOS.md

"¿Por qué falla esto?"
→ Ve GUIA_RAPIDA_TROUBLESHOOTING.md

"¿Dónde está el código de X?"
→ GUIA_RAPIDA_TROUBLESHOOTING.md sección "Referencia archivos"

"¿Cuál es la arquitectura?"
→ RESUMEN_EJECUTIVO_FLUJO.md sección 4

"¿Qué datos persiste?"
→ RESUMEN_EJECUTIVO_FLUJO.md sección 9

"¿Cuáles son los permisos?"
→ ANALISIS_360_VISION_COMPLETA.md sección "Matriz de Control"
```

---

## ✅ Checklist de Iniciación

- [ ] Leí esta página
- [ ] Entiendo las 3 fases del checklist
- [ ] Sé cuáles son las 4 capas
- [ ] Conozco los 3 roles
- [ ] Entiendo token + persistencia
- [ ] Leí el flujo usuario completo
- [ ] Sé dónde buscar si falla algo
- [ ] Tengo marcados los 6 documentos
- [ ] Estoy listo para trabajar en la app ✓

---

**Inicio Rápido:** ✅ Completo  
**Tiempo de lectura:** 10-15 minutos  
**Siguiente paso:** Lee RESUMEN_EJECUTIVO_FLUJO.md  
**Versión:** 1.0

