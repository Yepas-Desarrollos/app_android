# Especificaciones Android - Sistema de Revisiones y Correcciones

> **Para usar en el workspace de Android**
> 
> Este documento contiene las especificaciones técnicas de lo que se implementó en el backend y lo que necesitas integrar en la aplicación Android.

---

## Resumen del Sistema

### ¿Qué se implementó en el Backend?

Se creó un sistema completo de revisiones y correcciones que funciona así:

1. Cuando un **AUDITOR** envía un checklist con items marcados como `FAIL`, automáticamente se crean registros de `AuditReview` (uno por cada item fallido)
2. Los **SUPERVISORES** ven esas revisiones pendientes y pueden corregirlas subiendo fotos como evidencia
3. Los **AUDITORES** después validan esas correcciones (aprueban o rechazan)

### Base de Datos (Nuevas Tablas)

Se crearon 3 tablas nuevas:

**`AuditReview`**
- Almacena items de auditoría que fallaron y necesitan corrección
- Estados: `PENDING`, `CORRECTED`, `VALIDATED`, `REJECTED`

**`AuditCorrection`**
- Almacena las correcciones hechas por supervisores

**`AuditCorrectionAttachment`**
- Almacena las fotos de evidencia (mínimo 1 foto obligatoria)

---

## Backend Disponible

### URL Base
Tu servidor actual (misma URL que usas para checklists)

### Autenticación
Todos los endpoints requieren JWT (mismo token que usas actualmente)

---

## Especificación de Endpoints

### 1. GET `/audit-reviews/pending`

**Propósito:** Listar revisiones pendientes de corrección

**Quién puede usarlo:**
- **SUPERVISOR**: Ve solo las de sus tiendas asignadas
- **MGR_OPS**: Ve todas

**Query Parameters:**
```
limit: number (default: 20)
page: number (default: 1)  
storeId: number (opcional)
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "auditItemTitle": "Limpieza de pisos",
      "auditItemCategory": "Limpieza",
      "storeName": "Tienda Norte",
      "storeCode": "T001",
      "auditNotes": "Piso mal barrido en pasillo 3",
      "createdAt": "2024-12-05T10:30:00Z",
      "createdByUserName": "Juan Auditor"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "totalPages": 1,
    "hasMore": false
  }
}
```

**¿Qué mostrar en Android?**
- Lista de items que necesitan corrección
- Título del item, tienda, notas del auditor
- Botón para corregir cada item

---

### 2. GET `/audit-reviews/corrected`

**Propósito:** Listar revisiones ya corregidas (para que auditores las validen)

**Quién puede usarlo:**
- **AUDITOR**: Ve solo las que él creó
- **MGR_PREV**: Ve todas las de auditores
- **ADMIN**: Ve todas

**Query Parameters:**
```
limit: number (default: 20)
page: number (default: 1)
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "auditItemTitle": "Limpieza de pisos",
      "storeName": "Tienda Norte",
      "status": "CORRECTED",
      "createdAt": "2024-12-05T10:30:00Z",
      "correctedAt": "2024-12-05T14:00:00Z",
      "correctedByUserName": "Pedro Supervisor",
      "correctionNotes": "Pasillo 3 limpiado completamente",
      "correctionPhotos": [
        "https://url-foto-1.jpg",
        "https://url-foto-2.jpg"
      ]
    }
  ],
  "pagination": { ... }
}
```

**¿Qué mostrar en Android?**
- Lista de correcciones pendientes de validar
- Nombre del supervisor que corrigió
- Notas de la corrección
- Miniaturas de fotos
- Botones: "Aprobar" y "Rechazar"

---

### 3. GET `/audit-reviews/:id`

**Propósito:** Obtener detalle completo de una revisión (con todas sus correcciones)

**Quién puede usarlo:** Todos los roles (el backend valida permisos)

**Response:**
```json
{
  "id": 1,
  "status": "CORRECTED",
  "auditNotes": "Piso mal barrido",
  "createdAt": "2024-12-05T10:30:00Z",
  "auditItem": {
    "id": 123,
    "title": "Limpieza de pisos",
    "category": "Limpieza",
    "responseStatus": "FAIL"
  },
  "store": {
    "id": 5,
    "code": "T001",
    "name": "Tienda Norte"
  },
  "createdByUser": {
    "id": 10,
    "name": "Juan Auditor",
    "email": "juan@example.com"
  },
  "corrections": [
    {
      "id": 50,
      "correctionNotes": "Limpiado completamente",
      "correctedAt": "2024-12-05T14:00:00Z",
      "correctedByUser": {
        "id": 20,
        "name": "Pedro Supervisor"
      },
      "attachments": [
        {
          "id": 100,
          "url": "https://s3.../foto.jpg",
          "type": "PHOTO",
          "createdAt": "2024-12-05T14:01:00Z"
        }
      ]
    }
  ]
}
```

**¿Cuándo usarlo?**
- Cuando el usuario toca un item de la lista
- Para mostrar todos los detalles en un diálogo/pantalla completa
- Para ver las fotos de evidencia en tamaño completo

---

### 4. PATCH `/audit-reviews/:id/validate`

**Propósito:** Validar una corrección (aprobar o rechazar)

**Quién puede usarlo:**
- **AUDITOR**: Solo las que él creó
- **MGR_PREV**: Todas
- **ADMIN**: Todas

**Request Body:**
```json
{
  "approved": true
}
```

**Response:**
```json
{
  "id": 1,
  "status": "VALIDATED",  // o "REJECTED" si approved=false
  "updatedAt": "2024-12-05T15:00:00Z"
}
```

**¿Cuándo llamarlo?**
- Cuando el auditor presiona "Aprobar" → `approved: true`
- Cuando el auditor presiona "Rechazar" → `approved: false`

**Después de llamarlo:**
- Recargar la lista de revisiones
- Mostrar mensaje de éxito
- Cerrar el diálogo de detalle

---

### 5. POST `/audit-corrections`

**Propósito:** Crear una corrección para una revisión

**Quién puede usarlo:** Solo **SUPERVISOR**

**Request Body:**
```json
{
  "reviewId": 1,
  "correctionNotes": "Pasillo 3 limpiado completamente"
}
```

**Response:**
```json
{
  "id": 50,
  "reviewId": 1,
  "correctedAt": "2024-12-05T14:00:00Z"
}
```

**Flujo en Android:**
1. Supervisor selecciona un item a corregir
2. Escribe notas (opcional)
3. Selecciona fotos (MÍNIMO 1)
4. Presiona "Enviar Corrección"
5. Primero llamas a este endpoint
6. Con el `id` de la respuesta, subes las fotos (ver siguiente endpoint)

---

### 6. POST `/audit-corrections/:id/attachments`

**Propósito:** Subir fotos de evidencia a una corrección

**Quién puede usarlo:** Solo el **SUPERVISOR** que creó esa corrección

**Request:** Multipart form-data
- Campo: `files` (array de archivos)
- Tipo: `image/*`
- Límite: Hasta 10 fotos
- **OBLIGATORIO: Mínimo 1 foto**

**Response:**
```json
[
  {
    "id": 100,
    "type": "PHOTO",
    "url": "https://s3.../foto.jpg",
    "createdAt": "2024-12-05T14:01:00Z"
  },
  {
    "id": 101,
    "type": "PHOTO",
    "url": "https://s3.../foto2.jpg",
    "createdAt": "2024-12-05T14:01:00Z"
  }
]
```

**Implementación en Android:**
- Usa la misma lógica que ya tienes para subir fotos en checklists normales
- Es multipart igual que `POST /items/:id/attachments`
- Cada foto va como `MultipartBody.Part` en el campo `files`

**IMPORTANTE:**
- Validar en UI que haya al menos 1 foto antes de enviar
- Si envías sin fotos, el backend retorna error 400

---

### 7. GET `/audit-corrections/my-team`

**Propósito:** Ver correcciones del equipo (para managers)

**Quién puede usarlo:**
- **MGR_OPS**: Para ver qué están haciendo sus supervisores
- **ADMIN**: Ve todo

**Query Parameters:**
```
limit: number (default: 20)
page: number (default: 1)  
storeId: number (opcional)
userId: number (opcional - filtrar por supervisor)
```

**Response:**
```json
{
  "data": [
    {
      "id": 50,
      "reviewId": 1,
      "auditItemTitle": "Limpieza de pisos",
      "storeName": "Tienda Norte",
      "storeCode": "T001",
      "correctedByUser": {
        "id": 20,
        "name": "Pedro Supervisor"
      },
      "correctionNotes": "Limpiado",
      "correctedAt": "2024-12-05T14:00:00Z",
      "attachments": [
        {
          "id": 100,
          "url": "https://s3.../foto.jpg",
          "type": "PHOTO"
        }
      ]
    }
  ],
  "pagination": { ... }
}
```

**¿Qué mostrar?**
- Lista de correcciones del equipo
- Filtros por tienda o por supervisor
- Solo lectura (no pueden editar)

---

## Flujo Completo del Sistema

### Flujo 1: Creación Automática de Revisiones

**Ya está implementado en el backend, no necesitas hacer nada en Android**

Cuando un auditor envía un checklist:
```
POST /runs/:id/submit
```

El backend detecta automáticamente:
- Si el template es de `scope = 'Auditores'`
- Busca items con `responseStatus = 'FAIL'`
- Crea automáticamente un `AuditReview` por cada item FAIL con estado `PENDING`

**No necesitas modificar nada en Android para el envío de checklists.**

---

### Flujo 2: Supervisor corrige un item

**Pasos en Android:**

1. Supervisor abre la app
2. Ve opción de menú "Correcciones" (solo si su rol es SUPERVISOR)
3. Entra a pantalla de correcciones
4. Llama: `GET /audit-reviews/pending`
5. Muestra lista de items a corregir
6. Supervisor toca un item
7. Abre formulario con:
   - Campo de texto para notas (opcional)
   - Selector de fotos (mínimo 1)
   - Botón "Enviar Corrección"
8. Al enviar:
   - Valida que haya al menos 1 foto
   - Llama: `POST /audit-corrections` con reviewId y notas
   - Recibe correctionId en respuesta
   - Llama: `POST /audit-corrections/:id/attachments` con las fotos
9. Muestra mensaje de éxito
10. Cierra formulario y recarga lista

---

### Flujo 3: Auditor valida la corrección

**Pasos en Android:**

1. Auditor abre la app
2. Ve opción de menú "Revisiones" (solo si su rol es AUDITOR)
3. Entra a pantalla de revisiones
4. Llama: `GET /audit-reviews/corrected`
5. Muestra lista de correcciones pendientes de validar
6. Auditor toca una corrección
7. Llama: `GET /audit-reviews/:id` para ver detalle completo
8. Muestra:
   - Título del item
   - Tienda
   - Notas originales del auditor
   - Notas de la corrección del supervisor
   - Fotos de evidencia (galería)
   - Botones: "Aprobar" y "Rechazar"
9. Al presionar algún botón:
   - Llama: `PATCH /audit-reviews/:id/validate` con `approved: true/false`
10. Muestra mensaje de éxito
11. Cierra detalle y recarga lista

---

## Requisitos de UI por Rol

### AUDITOR / MGR_PREV

**Opción de menú:** "Revisiones"

**Pantalla debe mostrar:**
- Lista de correcciones pendientes de validar
- Cada item debe mostrar:
  - Título del item de checklist
  - Tienda
  - Nombre del supervisor que corrigió
  - Cantidad de fotos adjuntas
  - Botón para ver detalle

**Al tocar un item:**
- Ver detalle completo con galería de fotos
- Botones: "Aprobar" (verde) y "Rechazar" (rojo)

---

### SUPERVISOR / MGR_OPS

**Opción de menú:** "Correcciones"

**Pantalla debe mostrar:**
- Lista de items pendientes de corrección
- Cada item debe mostrar:
  - Título del item de checklist
  - Tienda
  - Notas del auditor (si las hay)
  - Botón "Corregir"

**Al tocar "Corregir":**
- Abrir formulario con:
  - Título del item (solo lectura)
  - Campo de texto para notas (opcional)
  - Selector de fotos (galería/cámara)
  - Lista de fotos seleccionadas con opción de eliminar
  - Contador de fotos (ej: "2 fotos seleccionadas")
  - Mensaje: "Mínimo 1 foto requerida"
  - Botón "Enviar Corrección" (deshabilitado si no hay fotos)

---

## Validaciones Importantes

### En Pantalla de Correcciones (Supervisores)

✅ **OBLIGATORIO:** Mínimo 1 foto
- Deshabilitar botón "Enviar" si no hay fotos
- Mostrar mensaje claro: "Debes adjuntar al menos 1 foto"

✅ **OPCIONAL:** Notas de corrección
- El campo puede estar vacío
- No es obligatorio

✅ **Límite:** Máximo 10 fotos
- Si el usuario intenta agregar más, mostrar mensaje

---

### En Pantalla de Revisiones (Auditores)

✅ Confirmar antes de aprobar/rechazar
- Mostrar diálogo de confirmación
- "¿Estás seguro de aprobar esta corrección?"
- "¿Estás seguro de rechazar esta corrección?"

✅ Mostrar todas las fotos
- Usar galería swipeable
- Permitir zoom
- Mostrar contador (ej: "Foto 1 de 3")

---

## Permisos y Seguridad

### Matriz de Acceso

| Pantalla | AUDITOR | SUPERVISOR | MGR_PREV | MGR_OPS | ADMIN |
|----------|---------|------------|----------|---------|-------|
| Revisiones (validar) | ✅ (las suyas) | ❌ | ✅ (todas) | ❌ | ✅ |
| Correcciones (corregir) | ❌ | ✅ (sus tiendas) | ❌ | Ver solo | ✅ |

### Implementación en Android

**Mostrar/ocultar opciones de menú según rol:**
```
Si rol == AUDITOR o MGR_PREV:
  → Mostrar opción "Revisiones"

Si rol == SUPERVISOR:
  → Mostrar opción "Correcciones"

Si rol == MGR_OPS:
  → Mostrar opción "Correcciones" (solo lectura)
```

El backend ya valida permisos en cada endpoint, así que no hay problema si alguien intenta acceder a una pantalla incorrecta.

---

## Manejo de Errores

### Errores Comunes del Backend

**403 Forbidden:**
- Usuario no tiene permiso para esa acción
- Mostrar: "No tienes permiso para realizar esta acción"

**404 Not Found:**
- Revisión no existe
- Mostrar: "La revisión ya no está disponible"

**400 Bad Request (sin fotos):**
- Intentó crear corrección sin fotos
- Mostrar: "Debes adjuntar al menos 1 foto de evidencia"

**401 Unauthorized:**
- Token expirado o inválido
- Redirigir a login

---

## Actualización de Datos

### ¿Cuándo recargar listas?

**Después de:**
- Crear una corrección → Recargar lista de pendientes
- Validar una revisión → Recargar lista de corregidas
- Al volver a la pantalla desde otra parte de la app
- Pull-to-refresh manual del usuario

**Pull-to-refresh:**
- Implementar swipe down para recargar
- Usar mismo endpoint con page=1

**Paginación:**
- Si `pagination.hasMore = true`, mostrar botón "Cargar más"
- Al presionarlo, incrementar page y agregar resultados a la lista

---

## Testing Recomendado

### Casos de Prueba

1. **Flujo completo:**
   - Auditor hace checklist con items FAIL
   - Supervisor recibe notificación (ver lista)
   - Supervisor corrige con fotos
   - Auditor ve corrección y aprueba

2. **Validaciones:**
   - Intentar enviar corrección sin fotos → Error
   - Intentar corregir item de tienda no asignada → 403
   - Auditor intenta validar revisión de otro auditor → 403

3. **Edge cases:**
   - Supervisores sin tiendas asignadas → Lista vacía
   - Items sin notas de auditor → No mostrar campo
   - Corrección sin notas → Solo mostrar fotos

---

## Resumen de Cambios en Backend

**Lo que se hizo:**

1. ✅ Creadas 3 nuevas tablas en BD
2. ✅ Migración aplicada exitosamente
3. ✅ Modificado endpoint `/runs/:id/submit` para auto-crear revisiones
4. ✅ Creados 7 nuevos endpoints
5. ✅ Validaciones de permisos por rol
6. ✅ Subida de fotos con multipart
7. ✅ Todo compilado y probado

**Archivos de referencia:**
- `walkthrough.md` - Documentación técnica completa del backend
- `implementation_plan.md` - Plan original del proyecto

---

## Próximos Pasos para Android

### Orden de Implementación Sugerido

1. **Crear modelos de datos**
   - Clases para parsear JSON de respuestas
   - Usar tus data classes existentes como referencia

2. **Agregar endpoints a tu API service**
   - Ya tienes un service de Retrofit
   - Agregar los 7 nuevos métodos

3. **Crear repositorio**
   - Llamadas a los endpoints
   - Manejo de errores
   - Mismo patrón que usas actualmente

4. **Pantalla de Correcciones (Supervisores)**
   - Lista de pendientes
   - Formulario de corrección
   - Selector de fotos
   - Subida multipart (igual que attachments de checklists)

5. **Pantalla de Revisiones (Auditores)**
   - Lista de correcciones
   - Galería de fotos
   - Botones aprobar/rechazar

6. **Navegación**
   - Agregar opciones de menú según rol
   - Rutas a las nuevas pantallas

7. **Testing**
   - Probar flujo completo
   - Validar permisos
   - Probar con diferentes roles

---

## Prompt Sugerido para Iniciar

**Para tu próxima sesión en el workspace de Android:**

```
Necesito integrar el sistema de Revisiones y Correcciones de Auditorías en mi app Android.

El backend ya está implementado con 7 endpoints nuevos:
- Listar revisiones pendientes (supervisores)
- Listar revisiones corregidas (auditores)
- Detalle de revisión
- Validar corrección (aprobar/rechazar)
- Crear corrección
- Subir fotos de evidencia
- Ver correcciones del equipo (managers)

Tengo las especificaciones técnicas completas de todos los endpoints (request/response).

Necesito implementar:
1. Pantalla "Correcciones" para SUPERVISORES (corregir items y subir fotos)
2. Pantalla "Revisiones" para AUDITORES (validar correcciones)

¿Puedes ayudarme a integrar esto siguiendo la arquitectura actual del proyecto?
```

---

## Contacto y Soporte

**Documentación de referencia:**
- `walkthrough.md` - Backend completo documentado
- `implementation_plan.md` - Plan original
- Este documento - Especificaciones para Android

**Backend URL:** Tu servidor actual (mismo que checklists)  
**Autenticación:** JWT (mismo token actual)
