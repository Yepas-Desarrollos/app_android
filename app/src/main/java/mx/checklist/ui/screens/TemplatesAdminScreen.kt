package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.AdminTemplateDto
import mx.checklist.ui.vm.AdminViewModel

/**
 * Pantalla dedicada para administración de templates
 * Similar al Panel de Asignaciones pero para templates
 */
@Composable
fun TemplatesAdminScreen(
    adminVM: AdminViewModel,
    onCreateTemplate: () -> Unit = { },
    onEditTemplate: (Long) -> Unit = { },
    onViewTemplate: (Long) -> Unit = { },
    onBack: () -> Unit = { }
) {
    var showDeleteDialog by remember { mutableStateOf<AdminTemplateDto?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Estados del ViewModel
    val templates by adminVM.templates.collectAsStateWithLifecycle()
    val loading by adminVM.loading.collectAsStateWithLifecycle()
    val error by adminVM.error.collectAsStateWithLifecycle()
    val operationSuccess by adminVM.operationSuccess.collectAsStateWithLifecycle()
    
    // Cargar templates al inicio
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con botón de regreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Settings, contentDescription = "Regresar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Templates Admin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Templates") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Estadísticas") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contenido según el tab seleccionado
        when (selectedTab) {
            0 -> TemplatesTab(
                templates = templates,
                loading = loading,
                error = error,
                operationSuccess = operationSuccess,
                onCreateTemplate = onCreateTemplate,
                onEditTemplate = onEditTemplate,
                onViewTemplate = onViewTemplate,
                onDeleteTemplate = { template -> showDeleteDialog = template },
                onRefresh = { adminVM.loadTemplates() }
            )
            1 -> StatsTab(templates = templates)
        }
    }
    
    // Dialog de confirmación para eliminar
    showDeleteDialog?.let { template ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Template") },
            text = { Text("¿Estás seguro de que quieres eliminar el template '${template.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        adminVM.deleteTemplate(template.id) {
                            // Refrescar lista tras eliminación
                            adminVM.loadTemplates()
                        }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TemplatesTab(
    templates: List<AdminTemplateDto>,
    loading: Boolean,
    error: String?,
    operationSuccess: String?,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onViewTemplate: (Long) -> Unit,
    onDeleteTemplate: (AdminTemplateDto) -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        // Botón para crear nuevo template
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Button(
                onClick = onCreateTemplate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Crear Template",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Nuevo Template", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        // Mensajes de estado
        if (operationSuccess != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "✅ ${operationSuccess}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        error?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "❌ Error: $it",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Lista de templates
        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onEdit = { onEditTemplate(template.id) },
                        onView = { onViewTemplate(template.id) },
                        onDelete = { onDeleteTemplate(template) }
                    )
                }
                
                if (templates.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay templates disponibles",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTab(templates: List<AdminTemplateDto>) {
    // Placeholder de estadísticas - contenido comentado temporalmente
    /*
    Aquí irán las métricas definidas (KPI) futuras sobre templates:
    - Uso por rol
    - Frecuencia de actualización
    - % de items críticos
    - Etc.
    */
    Column {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "(Sección de estadísticas – pendiente definir métricas)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TemplateCard(
    template: AdminTemplateDto,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (template.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Items: ${template.items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (template.isActive) "✅ Activo" else "❌ Inactivo",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Row {
                    IconButton(onClick = onView) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Ver template",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar template",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar template",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}