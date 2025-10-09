package mx.checklist.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.FieldType
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.data.auth.AuthState
import mx.checklist.ui.vm.AdminViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTemplateFormScreen(
    vm: AdminViewModel,
    templateId: Long?, // null = crear nuevo, not null = editar existente
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onCreateItem: (Long) -> Unit,
    onEditItem: (Long, Long) -> Unit
) {
    // Estados locales del formulario
    var name by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf<ItemTemplateDto?>(null) }
    var currentTemplateId by remember { mutableStateOf(templateId) }
    var isCreatingTemplate by remember { mutableStateOf(false) }

    // Estados del ViewModel
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val currentTemplate by vm.currentTemplate.collectAsStateWithLifecycle()
    val success by vm.operationSuccess.collectAsStateWithLifecycle()

    val isEditing = currentTemplateId != null
    val title = if (isEditing) "Editar Checklist" else "Crear Checklist"

    // ✅ CAMBIADO: Inferir automáticamente el scope en español según el rol del usuario
    val targetScope = remember {
        when (AuthState.roleCode) {
            "MGR_PREV" -> "Auditores"      // MGR Prevención administra Auditores
            "MGR_OPS" -> "Supervisores"    // MGR Operaciones administra Supervisores
            "ADMIN" -> "Auditores"         // ADMIN por defecto crea para Auditores
            else -> "Auditores"
        }
    }

    // Cargar template si estamos editando, limpiar si estamos creando
    LaunchedEffect(currentTemplateId) {
        val templateIdToLoad = currentTemplateId
        if (templateIdToLoad != null && templateIdToLoad != 0L) {
            vm.loadTemplate(templateIdToLoad)
        } else {
            vm.clearCurrentTemplate()
        }
    }

    // Actualizar campos cuando se carga el template
    LaunchedEffect(currentTemplate?.id, currentTemplate?.name, currentTemplate?.isActive) {
        val template = currentTemplate
        if (template != null) {
            name = template.name
            isActive = template.isActive
            //targetRoleCode = template.targetRoleCode ?: "ROL_AUD" // ✅ NUEVO: Cargar rol objetivo
        } else if (currentTemplateId == null) {
            name = ""
            isActive = true
            //targetRoleCode = "ROL_AUD" // ✅ NUEVO: Rol objetivo por defecto al crear
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con botón de volver
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Error: $err",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Formulario del template
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Información del Checklist",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Checklist *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    isError = name.isBlank()
                )

                // Switch para activar/desactivar el template
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estado del Template",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isActive) "Activo" else "Inactivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            enabled = !loading
                        )
                    }
                }

                // Información del rol objetivo (solo lectura)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rol objetivo",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Mostrar texto con el rol seleccionado
                    Text(
                        text = targetScope,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            vm.clearError()
                            val templateId = currentTemplateId
                            if (isEditing && templateId != null) {
                                // Actualizar nombre si cambió
                                if (currentTemplate?.name != name) {
                                    vm.updateTemplate(templateId = templateId, name = name.takeIf { it.isNotBlank() }) {}
                                }
                                // Actualizar estado activo/inactivo si cambió
                                if (currentTemplate?.isActive != isActive) {
                                    vm.updateTemplateStatus(templateId, isActive) {}
                                }
                                // NOTA: targetRoleCode no se puede cambiar después de crear el template
                            } else {
                                // Crear nuevo template
                                if (!isCreatingTemplate && name.isNotBlank()) {
                                    isCreatingTemplate = true
                                    vm.createTemplate(name = name, scope = targetScope) { newTemplateId ->
                                        currentTemplateId = newTemplateId
                                        isCreatingTemplate = false
                                        vm.loadTemplate(newTemplateId)
                                        onSaved()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading && !isCreatingTemplate && name.isNotBlank()
                    ) {
                        Text(
                            text = when {
                                isCreatingTemplate -> "Creando..."
                                isEditing -> "Actualizar"
                                else -> "Crear"
                            }
                        )
                    }
                }
            }
        }

        // ✅ NUEVO: Sección de items agrupados por categoría (solo para templates existentes)
        currentTemplate?.let { template ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Obtener todos los items de todas las secciones
                        val allItems = template.sections.flatMap { it.items }

                        Text(
                            text = "Items del Template (${allItems.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        FilledTonalButton(
                            onClick = {
                                vm.clearError()
                                vm.clearSuccess()
                                onCreateItem(template.id)
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar item")
                        }
                    }

                    // Agrupar items por categoría
                    val itemsByCategory = remember(template.sections) {
                        template.sections
                            .flatMap { it.items }
                            .groupBy { it.category ?: "Sin categoría" }
                            .toSortedMap()
                    }

                    if (itemsByCategory.isEmpty()) {
                        Text(
                            text = "No hay items creados. Presiona 'Agregar item' para crear el primero.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(min = 50.dp, max = 500.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsByCategory.forEach { (categoryName, categoryItems) ->
                                // Encabezado de categoría
                                item(key = "category_$categoryName") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "${categoryItems.size} item(s)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                // Items de esta categoría
                                items(
                                    items = categoryItems.sortedBy { it.orderIndex },
                                    key = { item -> item.id ?: 0L }
                                ) { item ->
                                    AdminTemplateItemCard(
                                        item = item,
                                        onEdit = {
                                            item.id?.let { itemId ->
                                                onEditItem(template.id, itemId)
                                            }
                                        },
                                        onDelete = {
                                            item.id?.let { itemId ->
                                                vm.deleteItem(template.id, itemId) {
                                                    vm.loadTemplate(template.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog de confirmación para eliminar item
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Item") },
            text = {
                Text("¿Estás seguro de que quieres eliminar el item '${item.title}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        templateId?.let { tid ->
                            if (item.id != null && item.id > 0) {
                                vm.deleteItem(tid, item.id) {
                                    showDeleteDialog = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun AdminTemplateItemCard(
    item: ItemTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${item.orderIndex} - ${item.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = FieldType.fromValue(item.expectedType ?: "")?.displayName ?: item.expectedType ?: "Sin tipo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!item.subcategory.isNullOrBlank()) {
                        Text(
                            text = "Subcategoría: ${item.subcategory}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}