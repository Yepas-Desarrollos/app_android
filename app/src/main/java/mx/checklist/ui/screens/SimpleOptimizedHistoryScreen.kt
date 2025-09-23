package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.RunSummaryDto
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.vm.RunsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryScreen optimizada con datos reales
 */
@Composable
fun SimpleOptimizedHistoryScreen(
    runsVM: RunsViewModel,
    adminVM: AdminViewModel? = null,
    onOpenRun: (Long, String, String?) -> Unit = { _, _, _ -> }
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf<RunSummaryDto?>(null) }
    
    // Verificar si el usuario es admin
    val isAdmin = adminVM != null
    
    // Estados del ViewModel
    val drafts by runsVM.pendingRunsFlow().collectAsStateWithLifecycle()
    val submitted by runsVM.historyRunsFlow().collectAsStateWithLifecycle()
    val loading by runsVM.loading.collectAsStateWithLifecycle()
    val error by runsVM.error.collectAsStateWithLifecycle()
    
    // Cargar datos al inicio
    LaunchedEffect(Unit) {
        runsVM.loadPendingRuns(all = true) // todos los borradores
        runsVM.loadHistoryRuns()           // enviadas recientes
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
            text = "Historial",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Error message
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
        
        // Tab selector
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Borradores") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Enviadas") }
            )
        }
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> DraftsContent(
                drafts = drafts,
                loading = loading,
                isAdmin = isAdmin,
                onOpenRun = onOpenRun,
                onDeleteRun = { run -> showDeleteDialog = run }
            )
            1 -> SubmittedContent(
                submitted = submitted,
                loading = loading,
                isAdmin = isAdmin,
                onOpenRun = onOpenRun,
                onDeleteRun = { run -> showDeleteDialog = run }
            )
        }
    }
    
    // Dialog de confirmación para eliminar runs (solo para admins)
    showDeleteDialog?.let { run ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Run") },
            text = { 
                Text(
                    if (selectedTab == 1) {
                        "¿Estás seguro de que quieres eliminar permanentemente esta run enviada?\n\nTemplate: ${run.templateName}\nTienda: ${run.storeCode}\n\nEsta acción no se puede deshacer."
                    } else {
                        "¿Estás seguro de que quieres eliminar este borrador?\n\nTemplate: ${run.templateName}\nTienda: ${run.storeCode}"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedTab == 1) {
                            // Force delete para runs enviadas (solo admins)
                            adminVM?.forceDeleteRun(run.id) {
                                // Run eliminada exitosamente - refrescar datos
                                runsVM.loadHistoryRuns()
                            }
                        } else {
                            // Delete normal para borradores
                            runsVM.deleteRun(run.id)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
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
private fun DraftsContent(
    drafts: List<RunSummaryDto>,
    loading: Boolean,
    isAdmin: Boolean,
    onOpenRun: (Long, String, String?) -> Unit,
    onDeleteRun: (RunSummaryDto) -> Unit
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(drafts, key = { it.id }) { run ->
                RunCard(
                    run = run,
                    isDraft = true,
                    onOpen = { onOpenRun(run.id, run.storeCode ?: "", run.templateName) },
                    onDelete = if (isAdmin) { { onDeleteRun(run) } } else null
                )
            }
            
            if (drafts.isEmpty()) {
                item {
                    EmptyStateCard("No tienes borradores guardados")
                }
            }
        }
    }
}

@Composable
private fun SubmittedContent(
    submitted: List<RunSummaryDto>,
    loading: Boolean,
    isAdmin: Boolean,
    onOpenRun: (Long, String, String?) -> Unit,
    onDeleteRun: (RunSummaryDto) -> Unit
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(submitted, key = { it.id }) { run ->
                RunCard(
                    run = run,
                    isDraft = false,
                    onOpen = { onOpenRun(run.id, run.storeCode ?: "", run.templateName) },
                    onDelete = if (isAdmin) { { onDeleteRun(run) } } else null
                )
            }
            
            if (submitted.isEmpty()) {
                item {
                    EmptyStateCard("No tienes runs enviadas")
                }
            }
        }
    }
}

@Composable
private fun RunCard(
    run: RunSummaryDto,
    isDraft: Boolean,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null
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
                        text = run.templateName ?: "Template sin nombre",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tienda: ${run.storeCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(run.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isDraft) {
                        Text(
                            text = "📝 Borrador",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "✅ Enviado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onOpen) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = if (isDraft) "Continuar" else "Ver"
                        )
                    }
                    
                    // Mostrar botón de eliminar solo para admins
                    if (onDelete != null) {
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
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(date: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        outputFormat.format(parsedDate ?: Date())
    } catch (e: Exception) {
        date
    }
}