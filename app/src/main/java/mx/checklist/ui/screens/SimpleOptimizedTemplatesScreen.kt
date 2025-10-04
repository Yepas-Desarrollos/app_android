package mx.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            text = "Selecciona un Checklist",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Tienda: $storeCode",
            style = MaterialTheme.typography.bodyMedium
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
                        loading = loading,
                        onStart = {
                            runsVM.createRun(storeCode, template.id) { runId ->
                                // ✅ CORREGIDO: Pasar runId (devuelto por createRun) en lugar de templateId
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
                                    text = "No hay checklists disponibles para esta tienda",
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
    loading: Boolean,
    onStart: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading) { onStart() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium
            )

            val meta = listOfNotNull(
                template.version?.let { "Versión: $it" },
                template.scope?.takeIf { it.isNotBlank() }?.let { "Ámbito: $it" },
                template.frequency?.takeIf { it.isNotBlank() }?.let { "Frecuencia: $it" }
            ).joinToString("  •  ")

            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Texto indicativo en lugar del botón play
            Text(
                text = if (loading) "Cargando..." else "Toca para iniciar →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}