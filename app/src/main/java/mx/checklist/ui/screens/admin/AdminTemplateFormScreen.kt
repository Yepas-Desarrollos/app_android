package mx.checklist.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import mx.checklist.data.api.dto.AdminTemplateDto
import mx.checklist.data.api.dto.FieldType
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.ui.vm.AdminViewModel

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
    var showDeleteDialog by remember { mutableStateOf<ItemTemplateDto?>(null) }

    // Estados del ViewModel
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val currentTemplate by vm.currentTemplate.collectAsStateWithLifecycle()
    val success by vm.operationSuccess.collectAsStateWithLifecycle()

    val isEditing = templateId != null
    val title = if (isEditing) "Editar Template" else "Crear Template"

    // Cargar template si estamos editando
    LaunchedEffect(templateId) {
        if (templateId != null) {
            vm.loadTemplate(templateId)
        }
    }

    // Actualizar campos cuando se carga el template
    LaunchedEffect(currentTemplate) {
        currentTemplate?.let { template ->
            name = template.name
        }
    }

    // Navegar de vuelta cuando se guarda exitosamente
    LaunchedEffect(success) {
        if (success != null) {
            onSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $errorMsg",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Formulario del template
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            if (isEditing && templateId != null) {
                                vm.updateTemplate(
                                    templateId = templateId,
                                    name = name.takeIf { it.isNotBlank() }
                                ) { /* onSuccess handled by LaunchedEffect */ }
                            } else {
                                vm.createTemplate(
                                    name = name
                                ) { /* onSuccess handled by LaunchedEffect */ }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading && name.isNotBlank()
                    ) {
                        Text(if (isEditing) "Actualizar" else "Crear")
                    }
                }
            }
        }

        // Sección de items (solo para templates existentes)
        if (isEditing && currentTemplate != null) {
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
                            text = "Items del Template (${currentTemplate!!.items.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        FilledTonalButton(
                            onClick = { onCreateItem(templateId!!) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar Item")
                        }
                    }

                    if (currentTemplate!!.items.isEmpty()) {
                        Text(
                            text = "No hay items en este template.\nAgrega items para completar tu checklist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                currentTemplate!!.items.sortedBy { it.orderIndex },
                                key = { it.id }
                            ) { item ->
                                ItemCard(
                                    item = item,
                                    onEdit = { onEditItem(templateId!!, item.id) },
                                    onDelete = { showDeleteDialog = item }
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
                        vm.deleteItem(templateId!!, item.id) {
                            showDeleteDialog = null
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
private fun ItemCard(
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