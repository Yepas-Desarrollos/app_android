package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.AdminTemplateDto
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.vm.RunsViewModel

/**
 * AdminTemplateListScreen optimizada con datos reales
 */
@Composable
fun SimpleOptimizedAdminScreen(
    adminVM: AdminViewModel,
    runsVM: RunsViewModel,
    onCreateTemplate: () -> Unit = { },
    onEditTemplate: (Long) -> Unit = { },
    onViewTemplate: (Long) -> Unit = { },
    onBack: () -> Unit = { }
) {
    var showDeleteDialog by remember { mutableStateOf<AdminTemplateDto?>(null) }
    
    // Estados del ViewModel
    val templates by adminVM.templates.collectAsStateWithLifecycle()
    val loading by adminVM.loading.collectAsStateWithLifecycle()
    val error by adminVM.error.collectAsStateWithLifecycle()
    val operationSuccess by adminVM.operationSuccess.collectAsStateWithLifecycle()
    
    // Cargar templates al inicio y limpiar estados
    LaunchedEffect(Unit) {
        adminVM.clearError()
        adminVM.clearSuccess()
        adminVM.loadTemplates()
    }
    
    // Limpiar mensaje de éxito después de un tiempo
    LaunchedEffect(operationSuccess) {
        if (operationSuccess != null) {
            kotlinx.coroutines.delay(3000)
            adminVM.clearSuccess()
        }
    }
    
    // Auto-dismiss de errores después de 5 segundos
    LaunchedEffect(error) {
        if (error != null) {
            kotlinx.coroutines.delay(5000)
            adminVM.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con botones de refresh y crear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Templates Admin",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de refresh
                IconButton(
                    onClick = {
                        adminVM.loadTemplates()
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refrescar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Botón de crear
                FloatingActionButton(
                    onClick = onCreateTemplate,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Crear Template")
                    }
                }
            }
        }
        
        // Estado de carga
        if (loading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error
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
        
        // Lista de templates REAL
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onEdit = { onEditTemplate(template.id) },
                    onDelete = { showDeleteDialog = template },
                    onView = { onViewTemplate(template.id) },
                    onStatusChange = { isActive -> 
                        adminVM.updateTemplateStatus(template.id, isActive) {
                            // Template status updated successfully
                            // Limpiar cache de templates para que usuarios vean cambios inmediatamente
                            runsVM.clearCache()
                        }
                    }
                )
            }
            
            if (templates.isEmpty() && !loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay templates creados\nToca + para crear uno nuevo",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog de confirmación para borrar
    showDeleteDialog?.let { template ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Template") },
            text = { Text("¿Estás seguro de que quieres eliminar '${template.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminVM.deleteTemplate(template.id) {
                            // Template eliminado exitosamente
                            // Limpiar cache de templates para refrescar lista de usuarios
                            runsVM.clearCache()
                        }
                        showDeleteDialog = null
                    }
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
fun TemplateCard(
    template: AdminTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onStatusChange: (Boolean) -> Unit = { }
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Items: ${template.items?.size ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onView) {
                        Icon(Icons.Default.Edit, contentDescription = "Ver/Editar")
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
            
            // Switch para activar/desactivar template
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (template.isActive) "Template Activo" else "Template Inactivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (template.isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Switch(
                    checked = template.isActive,
                    onCheckedChange = onStatusChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}