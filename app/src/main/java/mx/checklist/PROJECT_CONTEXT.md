# CONTEXTO DEL PROYECTO - SISTEMA CHECKLIST

## FASE ACTUAL: FASE 1 - Nuevos Roles de Manager
**Fecha de inicio:** Septiembre 2024
**Estado:** En implementación

## OBJETIVO PRINCIPAL
Implementar sistema de roles jerárquicos con permisos específicos por área:

### JERARQUÍA DE ROLES
```
ADMIN (Máximo nivel)
├── MGR_PREV (Manager Prevención - Amado)  
├── MGR_OPS (Manager Operaciones - Jesus)
├── AUDITOR
└── SUPERVISOR
```

## PLAN DE 3 FASES

### 🔄 FASE 1: NUEVOS ROLES (ACTUAL)
**Objetivo:** Agregar roles MGR_PREV y MGR_OPS con sectores

**Backend Completado:**
- ✅ Schema Prisma con sectores y UserStoreAssignment
- ✅ Nuevos role guards (ManagerLevelGuard, PreventionAreaGuard, OperationsAreaGuard)
- ✅ Usuarios Amado (MGR_PREV) y Jesus (MGR_OPS) creados
- ✅ API endpoints para asignaciones de tiendas
- ✅ Permisos básicos del panel admin actualizados

**Frontend Completado:**
- ✅ Clase Authenticated.kt con métodos de roles
- ✅ AdminComponents.kt con botones específicos por rol
- ✅ Navegación actualizada para múltiples managers

**PENDIENTE FASE 1:**
- ✅ Permisos granulares: Managers NO pueden borrar corridas enviadas  
- ✅ **UI CONDICIONAL POR ROL:**
  - ✅ Botón "Borrar" corridas enviadas NO aparece para Managers
  - ✅ Panel Admin aparece solo para ADMIN/MGR_PREV/MGR_OPS
- ✅ **FILTROS DE VISUALIZACIÓN:**
  - ✅ AUDITOR/SUPERVISOR: Solo ven sus propios borradores y enviadas
  - ✅ MANAGERS: Ven sus propios borradores + TODAS las enviadas  
  - ✅ ADMIN: Ve todo
- ✅ **PANEL DE ASIGNACIONES** para managers gestionar sectores de usuarios
- ✅ **FILTROS POR ÁREA EN ASIGNACIONES** (MGR_PREV solo ve AUDITORES, MGR_OPS solo SUPERVISORES)
- ✅ **UI FRONTEND** para panel de asignaciones
  - ✅ Preselección de sectores actuales del usuario
  - ✅ Diálogo mejorado con indicadores de cambios (+/-) 
  - ✅ Botones rápidos (Todos/Revertir)
  - ✅ Prevención doble tap + Snackbar feedback
  - ✅ Lista de tiendas collapsible
  - ✅ Filtros y paginación en backend summary
- 🔨 Testing completo de permisos por área

### 🏗️ FASE 3: REESTRUCTURACIÓN DE CHECKLIST (ACTUAL)  
**Objetivo:** Reorganizar estructura de templates
- ✅ Nueva estructura jerárquica: Checklist → Secciones → Items
- ✅ Porcentajes en secciones e items (suma 100% en cada nivel)
- ✅ Schema Prisma actualizado con modelos ChecklistSection
- ✅ API endpoints implementados para secciones (GET/POST/PATCH/DELETE/reorder)
- ✅ API endpoints implementados para items (GET/POST/PATCH/DELETE/reorder)
- ✅ Migración inicial de datos existentes completada
- ✅ Correcciones en acceso a modelos Prisma (usando casting a any)
- ✅ Estructura de rutas reorganizada para evitar conflictos
- ✅ Implementar validación avanzada para porcentajes
  - ✅ Validación que suma sea exactamente 100% (con margen de redondeo)
  - ✅ Validación al crear nuevos elementos (no superar 100% disponible)
  - ✅ Distribución automática equitativa de porcentajes
- 🔨 UI para edición de estructura y porcentajes

### 📋 FASE 2: SISTEMA DE PORCENTAJES (PLANEADA)
**Objetivo:** Cambiar scoring de OK/FAIL a porcentajes
- Integración con nueva estructura de secciones
- UI para entrada de porcentajes en respuestas
- Reportes con promedios ponderados

## USUARIOS DEL SISTEMA

### Usuarios Manager Creados:
1. **Amado** (MGR_PREV)
   - Email: amado@company.com / Password: manager123
   - Área: Prevención de Pérdidas
   - Sectores asignados: [Por definir]

2. **Jesus** (MGR_OPS)  
   - Email: jesus@company.com / Password: manager123
   - Área: Operaciones
   - Sectores asignados: [Por definir]

## PERMISOS POR ROL

### MGR_PREV / MGR_OPS  
- ✅ Panel de administración de templates
- ✅ CRUD de templates (crear, editar)
- ✅ Borrar corridas BORRADOR únicamente
- ✅ NO pueden borrar corridas ENVIADAS (protegido por guards)
- ✅ **VEN TODAS las tiendas y sectores**
- ✅ **PUEDEN ASIGNAR** supervisores/auditores a sectores específicos
- ✅ Panel de gestión de asignaciones usuario-sector completo

### ADMIN
- ✅ Acceso total al sistema
- ✅ Gestión de usuarios y roles  
- ✅ CRUD completo de templates
- ✅ Borrar cualquier corrida (borrador o enviada) via endpoint /force
- ✅ Ver todas las tiendas y sectores

### AUDITOR/SUPERVISOR
- ✅ Realizar checklist
- ✅ Ver history propio y poder eliminar solo sus borradores no enviadas
- ❌ NO acceso al panel admin
- 🔨 **SOLO VEN tiendas de sus sectores asignados**
- ❌ NO pueden asignar a otros usuarios

## ESTRUCTURA TÉCNICA

### Backend (NestJS + Prisma)
- **Base de datos:** MySQL
- **Autenticación:** JWT con roles
- **Guards:** Role-based access control
- **API:** REST endpoints con validación de permisos

### Frontend (Android Kotlin)
- **UI:** Jetpack Compose
- **Estado:** StateFlow con AuthViewModel
- **Navegación:** Conditional routing por roles

## COMANDOS ÚTILES

### Backend:
```bash
cd e:\checklist_android\checklist-api
npm run start:dev          # Servidor desarrollo
npm run build              # Construir
npx prisma studio         # Ver base de datos
npx prisma db push        # Aplicar cambios schema

# Comandos para Fase 3 (Reestructuración Checklist):
npx ts-node scripts/migration-prisma-phase3.ts  # Actualizar schema y crear migración
npx ts-node scripts/migration-phase3-manual.ts # Migrar datos existentes a nueva estructura
npx prisma generate                          # Regenerar cliente Prisma después de cambios en schema
```

### Android:
```bash
lo hago directo en andorid studio   # Limpiar y construir
```

## NOTAS DE DESARROLLO

### Última Sesión (Septiembre 23, 2025):
  - Managers: Solo pueden borrar corridas BORRADOR
  - Admin: Puede borrar cualquier corrida via endpoint /force
### Última Sesión (Septiembre 24, 2025):
 - **VALIDADO**: MGR_PREV ve solo AUDITORES, MGR_OPS ve solo SUPERVISORES
  - **VALIDADO**: Filtrado de tiendas asignadas para AUDITOR/SUPERVISOR funciona correctamente (solo ven sus tiendas asignadas, si no tienen asignaciones ven todas)
  - **REAGREGADO**: Endpoint de diagnóstico `/stores/assignments/me` para ver JWT y asignaciones del usuario autenticado
  - **ACLARADO**: Los templates solo aparecen para auditor/supervisor si el scope del endpoint lo permite; en admin aparecen todos
  - Managers VEN TODAS las tiendas (no filtradas)
  - Managers PUEDEN ASIGNAR usuarios a sectores
  - AUDITOR/SUPERVISOR son quienes tienen filtros por sector
  - **IMPLEMENTADO**: Fase 3 - Nueva estructura de checklist con secciones
  - **COMPLETADO**: Schema Prisma actualizado con modelos de jerarquía de 3 niveles
  - **IMPLEMENTADO**: Endpoints completos para gestión de secciones e items de checklist
  - **SOLUCIONADO**: Problemas con permisos de archivos y generación de cliente Prisma
  - **VERIFICADO**: El modelo ChecklistSection está correctamente disponible en el cliente Prisma
  - **CORREGIDO**: Conflictos de rutas con TemplatesAdminController
  - **IMPLEMENTADO**: Nueva estructura de rutas para secciones e items
  - **VALIDADO**: Todos los endpoints de CRUD para secciones e items funcionan correctamente
  
### Última Sesión (Septiembre 25, 2025):
  - **IMPLEMENTADO**: Sistema completo de validación de porcentajes para estructura jerárquica
  - **MEJORADO**: Validación de porcentajes para asegurar suma exacta de 100% con manejo de redondeo
  - **CREADOS**: Endpoints para distribución automática equitativa de porcentajes
  - **VALIDADO**: Control para evitar que se excedan porcentajes máximos al crear nuevos elementos
  - **IMPLEMENTADO**: Comprobación que verifica pertenencia de elementos a su contenedor (sección/checklist)
  - **MEJORADOS**: Mensajes de error más descriptivos en validaciones de porcentajes
  - **AGREGADO**: Funcionalidad de margen de error para el redondeo de porcentajes
  - **IMPLEMENTADO**: Validación de porcentajes positivos obligatorios
  - **MEJORADO**: Manejo de errores con códigos HTTP específicos (400 para validaciones, 404 para no encontrado)
  - **PROBADO**: Validación completa de la funcionalidad de porcentajes con tests manuales
  

- ✅ **FILTROS POR SECTOR IMPLEMENTADOS:**
  - stores.controller.ts: Filtros por sector para AUDITOR/SUPERVISOR
  - run-history.controller.ts: Corridas filtradas por sector asignado
- ✅ **APIs DE ASIGNACIONES COMPLETAS:**
  - /admin/assignments/assignable-users (listar usuarios)
  - /admin/assignments/sectors (asignar por sectores)
  - /admin/assignments/summary (resumen con filtros userId/sector/paginación)
- ✅ **PANEL ASIGNACIONES UI COMPLETO:**
  - UX mejorada: preselección, indicadores cambios, feedback
  - Backend: filtros por área manager, paginación, idempotencia
  - Frontend: prevención doble tap, snackbar, estados loading
  - **VALIDADO**: MGR_PREV ve solo AUDITORES, MGR_OPS ve solo SUPERVISORES

### Próximos Pasos:
1. ✅ **COMPLETADO**: Filtrar usuarios por área en panel asignaciones (MGR_PREV → AUDITORES, MGR_OPS → SUPERVISORES)
2. ✅ Testing completo de filtros por sector para AUDITOR/SUPERVISOR (tiendas/corridas)
3. 🔄 **FASE 3**+  **FASE 2**: Sistema de porcentajes en respuestas: Implementar nueva estructura de checklists
   - ✅ Migrar schema Prisma con nuevos modelos
   - ✅ Migrar datos existentes a nueva estructura básica
   - ✅ Completar endpoints para gestión de secciones (PATCH, DELETE, reorder)
   - ✅ Implementar endpoints para gestión de items dentro de secciones
   - ✅ Implementar validación de sumas de porcentajes (deben sumar 100%)
   - ✅ Manejo de errores con códigos HTTP específicos para validaciones
   - 🔨 Desarrollar UI para gestión de secciones e items con porcentajes
   - 🔨 Agregar confirmación antes de eliminar secciones/items con datos

5. **FASE 4 FUTURA**: Reportes con filtros avanzados

## ENDPOINTS IMPLEMENTADOS

### Autenticación
- `POST /auth/login` - Login con email/password

### Gestión de Asignaciones (Managers)
- `GET /admin/assignments/assignable-users` - Listar AUDITOR/SUPERVISOR
- `POST /admin/assignments/sectors` - Asignar usuario a sectores
- `POST /admin/assignments/stores` - Asignar usuario a tiendas específicas
- `GET /admin/assignments/user/:userId/stores` - Ver asignaciones de usuario
- `GET /admin/assignments/summary` - Resumen por área (prevención/operaciones)
- `GET /admin/assignments/sectors` - Listar sectores numéricos disponibles (array plano)

### Gestión de Tiendas (Filtradas por rol)
 - `GET /stores` - Listar tiendas (filtradas por asignación para AUDITOR/SUPERVISOR; si no tiene asignaciones ve todas)
 - `GET /stores/assignments/me` - Endpoint de diagnóstico: muestra JWT y tiendas asignadas del usuario autenticado

### Historial de Corridas (Filtradas por rol)
- `GET /runs/history/sent` - Corridas enviadas (filtradas por usuario/sector según rol)
- `GET /runs/history/pending` - Corridas borrador (filtradas por usuario/sector según rol)

### Administración de Corridas
- `DELETE /admin/runs/:id` - Borrar borrador (Managers + Admin)
- `DELETE /admin/runs/:id/force` - Borrar cualquier corrida (Solo Admin)

### Templates Admin
- `GET /admin/templates` - Listar templates (Managers + Admin)
- `POST /admin/templates` - Crear template (Managers + Admin)
- `PATCH /admin/templates/:id` - Editar template (Managers + Admin)
- `DELETE /admin/templates/:id` - Borrar template (Managers + Admin)

### Nuevos Endpoints (Fase 3: Estructura Checklist)
**Implementados:**
- ✅ `GET /api/v3/sections/checklist/:checklistId` - Listar secciones de un checklist
- ✅ `POST /api/v3/sections` - Crear sección
- ✅ `PATCH /api/v3/sections/:id` - Actualizar sección
- ✅ `DELETE /api/v3/sections/:id` - Eliminar sección
- ✅ `PATCH /api/v3/sections/reorder/:checklistId` - Reordenar secciones
- ✅ `GET /api/v3/section-items/section/:sectionId` - Listar items de una sección
- ✅ `POST /api/v3/section-items` - Crear item en sección
- ✅ `GET /debug/prisma` - Endpoint de diagnóstico de modelos disponibles en Prisma

**Implementados (Nueva Estructura de Rutas):**
- ✅ `GET /admin/checklist-sections/checklist/:checklistId` - Listar secciones de un checklist
- ✅ `POST /admin/checklist-sections/checklist/:checklistId` - Crear nueva sección
- ✅ `PATCH /admin/checklist-sections/:id` - Actualizar sección
- ✅ `DELETE /admin/checklist-sections/:id` - Eliminar sección
- ✅ `PATCH /admin/checklist-sections/checklist/:checklistId/reorder` - Reordenar secciones
- ✅ `PATCH /admin/checklist-sections/checklist/:checklistId/update-percentages` - Actualizar porcentajes de secciones
- ✅ `PATCH /admin/checklist-sections/checklist/:checklistId/distribute-percentages` - Distribuir porcentajes equitativamente entre secciones
- ✅ `GET /admin/checklist-items/section/:sectionId` - Obtener items de una sección
- ✅ `POST /admin/checklist-items/section/:sectionId` - Crear nuevo item en sección
- ✅ `PATCH /admin/checklist-items/:id` - Actualizar item
- ✅ `DELETE /admin/checklist-items/:id` - Eliminar item
- ✅ `PATCH /admin/checklist-items/section/:sectionId/reorder` - Reordenar items
- ✅ `PATCH /admin/checklist-items/section/:sectionId/update-percentages` - Actualizar porcentajes de items
- ✅ `PATCH /admin/checklist-items/section/:sectionId/distribute-percentages` - Distribuir porcentajes equitativamente entre items
- ✅ `PATCH /admin/checklist-items/:itemId/move-to-section/:targetSectionId` - Mover item a otra sección

**Endpoints Legacy (Obsoletos):**
- `GET /api/v3/sections/checklist/:checklistId` - Listar secciones (reemplazado)
- `POST /api/v3/sections` - Crear sección (reemplazado)
- `PATCH /api/v3/sections/:id` - Actualizar sección (reemplazado)
- `DELETE /api/v3/sections/:id` - Eliminar sección (reemplazado)
- `PATCH /api/v3/sections/reorder/:checklistId` - Reordenar secciones (reemplazado)
- `GET /api/v3/section-items/section/:sectionId` - Listar items (reemplazado)
- `POST /api/v3/section-items` - Crear item en sección (reemplazado)

<!-- Placeholder Estadísticas Templates: actualmente el apartado en la UI está visible pero las métricas detalladas se comentaron temporalmente a la espera de definición final -->

---

**IMPORTANTE:** Mantener este archivo actualizado en cada sesión de desarrollo.