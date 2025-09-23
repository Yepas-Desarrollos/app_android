package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.TemplateDto
import mx.checklist.ui.vm.RunsViewModel

/**
 * TemplatesScreen optimizada con datos reales
 */
@Composable
fun SimpleOptimizedTemplatesScreen(
    storeCode: String,
    runsVM: RunsViewModel,
    onRunCreated: (Long, String) -> Unit = { _, _ -> }
) {
    // Estados del ViewModel
    val allTemplates by runsVM.getTemplates().collectAsStateWithLifecycle()
    val loading by runsVM.loading.collectAsStateWithLifecycle()
    val error by runsVM.error.collectAsStateWithLifecycle()
    
    // Filtrar solo templates activos para usuarios normales
    val templates = remember(allTemplates) {
        allTemplates.filter { it.isActive }
    }
    
    // Cargar templates al inicio
    LaunchedEffect(storeCode) {
        // Los templates se cargan automáticamente con getTemplates()
    }
    
    // Auto-dismiss de errores después de 5 segundos
    LaunchedEffect(error) {
        if (error != null) {
            kotlinx.coroutines.delay(5000)
            runsVM.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Templates — $storeCode",
            style = MaterialTheme.typography.headlineSmall
        )
        
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
        
        // Estado de carga
        if (loading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Lista de templates REAL
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onStart = { 
                            runsVM.createRun(storeCode, template.id) { runId ->
                                onRunCreated(runId, template.name)
                            }
                        }
                    )
                }
                
                if (templates.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay templates disponibles para esta tienda",
                                    style = MaterialTheme.typography.bodyMedium,
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
private fun TemplateCard(
    template: TemplateDto,
    onStart: () -> Unit
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
                        text = "Versión: ${template.version ?: 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    template.scope?.let { scope ->
                        Text(
                            text = "Scope: $scope",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    template.frequency?.let { freq ->
                        Text(
                            text = "Frecuencia: $freq",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onStart) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Iniciar checklist",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}