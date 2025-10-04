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
    onEditItem: (Long, Long) -> Unit,
    onCreateSection: (Long) -> Unit // Nuevo parámetro para crear secciones
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
    val title = if (isEditing) "Editar Template" else "Crear Template"

    // Cargar template si estamos editando (solo una vez)
    LaunchedEffect(currentTemplateId) {
        val templateIdToLoad = currentTemplateId
        if (templateIdToLoad != null && templateIdToLoad != 0L) {
            vm.loadTemplate(templateIdToLoad)
        }
    }

    // Actualizar campos cuando se carga el template (solo una vez por template)
    LaunchedEffect(currentTemplate?.id, currentTemplate?.name, currentTemplate?.isActive) {
        val template = currentTemplate // Crear variable local para permitir smart cast
        template?.let {
            name = it.name
            isActive = it.isActive
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
                val friendlyMsg = when {
                    err.contains("404", ignoreCase = true) || err.contains("Not Found", ignoreCase = true) ->
                        "No se pudo completar la acción: el recurso ya no existe o fue eliminado previamente."
                    err.contains("DELETE", ignoreCase = true) ->
                        "No se pudo eliminar la sección. Puede que ya haya sido eliminada o no exista."
                    else -> "Error: $err"
                }
                Text(
                    text = friendlyMsg,
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
                    text = "Información del Template",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Template *") },
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
                            val templateId = currentTemplateId // Crear variable local para permitir smart cast
                            if (isEditing && templateId != null) {
                                // Actualizar nombre si cambió
                                if (currentTemplate?.name != name) {
                                    vm.updateTemplate(templateId = templateId, name = name.takeIf { it.isNotBlank() }) {
                                        // Mantener en la misma pantalla después de actualizar
                                    }
                                }
                                // Actualizar estado activo/inactivo si cambió
                                if (currentTemplate?.isActive != isActive) {
                                    vm.updateTemplateStatus(templateId, isActive) {
                                        // No recargar template innecesariamente
                                    }
                                }
                            } else {
                                // Solo permitir crear si no estamos ya creando
                                if (!isCreatingTemplate && name.isNotBlank()) {
                                    isCreatingTemplate = true
                                    vm.createTemplate(name = name) { newTemplateId ->
                                        // Actualizar el estado local para cambiar a modo edición
                                        currentTemplateId = newTemplateId
                                        isCreatingTemplate = false
                                        // Cargar el template recién creado
                                        vm.loadTemplate(newTemplateId)
                                        // Llamar onSaved para navegar de vuelta a la lista
                                        onSaved()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading && !isCreatingTemplate && name.isNotBlank() // Bloquear cuando está creando
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

        // Sección de secciones (solo para templates existentes)
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
                        Text(
                            text = "Secciones del Template (${template.sections.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        FilledTonalButton(
                            onClick = {
                                vm.clearError()
                                vm.clearSuccess()
                                onCreateSection(template.id)
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar sección")
                        }
                    }

                    // Calcular valores directamente
                    val sections = template.sections
                    val totalSectionPercentage = sections.sumOf { it.percentage ?: 0.0 }
                    val isSectionPercentageValid = totalSectionPercentage.roundToInt() == 100

                    // Mostrar advertencia si la suma de porcentajes no es 100
                    if (!isSectionPercentageValid && sections.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "La suma de porcentajes de las secciones debe ser 100%. Actual: ${"%.2f".format(totalSectionPercentage)}%",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Botón para distribuir porcentajes entre secciones
                    if (sections.isNotEmpty()) {
                        Button(
                            onClick = { vm.distributeSectionPercentages(template.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Distribuir porcentajes equitativamente entre secciones")
                        }
                    }

                    // LISTADO DE SECCIONES
                    Text(
                        text = if (sections.isEmpty()) "No hay secciones creadas" else "Secciones (${sections.size}):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(min = 50.dp, max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sections.isEmpty()) {
                            item {
                                Text(
                                    text = "Presiona 'Agregar sección' para crear la primera sección",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(
                                items = sections.sortedBy { it.orderIndex },
                                key = { section -> section.id ?: 0L }
                            ) { section ->
                                SectionCard(
                                    section = section,
                                    onEdit = { onEditItem(template.id, section.id ?: 0L) }, // Editar SECCIÓN
                                    onDelete = { vm.deleteSection(section.id ?: 0L) {
                                        vm.loadTemplate(template.id)
                                    } },
                                    onCreateItem = { sectionId -> onCreateItem(sectionId) },
                                    onEditItem = { itemId ->
                                        // Editar ITEM: usar el callback correcto con templateId y itemId
                                        if (itemId != null && itemId > 0) {
                                            // Buscar la sección que contiene este item
                                            val sectionWithItem = template.sections.find { s ->
                                                s.items.any { i -> i.id == itemId }
                                            }
                                            sectionWithItem?.let { sec ->
                                                // Navegar correctamente al editor de items
                                                // onEditItem espera (templateId, itemId)
                                                onEditItem(template.id, itemId)
                                            }
                                        }
                                    },
                                    onDeleteItem = { itemId ->
                                        if (itemId != null && itemId > 0) {
                                            section.id?.let { sectionId ->
                                                vm.deleteSectionItem(sectionId, itemId) {
                                                    vm.loadTemplate(template.id)
                                                }
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
                        templateId?.let { tid -> if (item.id != null && item.id > 0) vm.deleteItem(tid, item.id!!) {
                            showDeleteDialog = null
                        } }
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

                    if (!item.category.isNullOrBlank() || !item.subcategory.isNullOrBlank()) {
                        val categoryText = listOfNotNull(
                            item.category?.takeIf { it.isNotBlank() },
                            item.subcategory?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")

                        Text(
                            text = categoryText,
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

@Composable
fun SectionCard(
    section: mx.checklist.data.api.dto.SectionTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateItem: (Long) -> Unit = {}, // Callback para crear un ítem en esta sección
    onEditItem: (Long) -> Unit = {},   // Callback para editar un ítem específico
    onDeleteItem: (Long) -> Unit = {}  // Callback para eliminar un ítem específico
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${section.orderIndex} - ${section.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Porcentaje: ${section.percentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar sección",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar sección",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Botón para agregar ítem
            Button(
                onClick = { section.id?.let { onCreateItem(it) } },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar ítem")
            }

            // Listado de ítems de esta sección
            if (section.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ítems (${section.items.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    section.items.forEach { item ->
                        AdminTemplateItemCard(
                            item = item,
                            onEdit = { item.id?.let { onEditItem(it) } },
                            onDelete = { item.id?.let { onDeleteItem(it) } }
                        )
                    }
                }
            }
        }
    }
}