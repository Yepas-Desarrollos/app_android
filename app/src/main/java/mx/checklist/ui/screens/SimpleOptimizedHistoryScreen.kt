package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.auth.AuthState
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
    onOpenRun: (Long, String?, String?) -> Unit = { _, _, _ -> }
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf<RunSummaryDto?>(null) }
    
    val isAdmin = adminVM != null
    val canDeleteSubmitted = remember(isAdmin) {
        AuthState.roleCode == "ADMIN"
    }
    
    val drafts by runsVM.pendingRunsFlow().collectAsStateWithLifecycle()
    val submitted by runsVM.historyRunsFlow().collectAsStateWithLifecycle()
    val loading by runsVM.loading.collectAsStateWithLifecycle()
    val error by runsVM.error.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        runsVM.loadPendingRuns(all = true)
        runsVM.loadHistoryRuns()
    }
    
    LaunchedEffect(error) {
        if (error != null) {
            kotlinx.coroutines.delay(5000)
            runsVM.clearError()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header mejorado
            Text(
                text = "Historial de Checklists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Error mejorado
            error?.let { errorMsg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Error: $errorMsg",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Tabs mejorados
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Borradores (${drafts.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Enviados (${submitted.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
            }

            // Contenido
            if (loading && drafts.isEmpty() && submitted.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cargando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val currentList = if (selectedTab == 0) drafts else submitted

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (selectedTab == 0) "No hay borradores" else "No hay checklists enviados",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentList, key = { it.id }) { run ->
                            RunCard(
                                run = run,
                                canDelete = selectedTab == 0 || canDeleteSubmitted,
                                onOpen = { onOpenRun(run.id, run.storeCode, run.templateName) },
                                onDelete = { showDeleteDialog = run }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog de confirmación de borrado
    showDeleteDialog?.let { run ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        runsVM.deleteRun(run.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            },
            title = { Text("¿Eliminar checklist?") },
            text = {
                Text("Se eliminará el checklist #${run.id} de ${run.templateName ?: "Sin nombre"} (${run.storeCode})")
            }
        )
    }
}

@Composable
private fun RunCard(
    run: RunSummaryDto,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(run.updatedAt) {
        try {
            // El backend envía en UTC, debemos convertir a hora local
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inFmt.timeZone = TimeZone.getTimeZone("UTC")

            val outFmt = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault())
            outFmt.timeZone = TimeZone.getDefault() // Hora local

            // Remover la 'Z' si existe
            val isoClean = run.updatedAt.replace("Z", "")
            val date = inFmt.parse(isoClean)
            date?.let { outFmt.format(it) } ?: run.updatedAt
        } catch (e: Exception) {
            // Si falla, intentar sin milisegundos
            try {
                val inFmtSimple = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inFmtSimple.timeZone = TimeZone.getTimeZone("UTC")

                val outFmt = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault())
                outFmt.timeZone = TimeZone.getDefault()

                val isoClean = run.updatedAt.replace("Z", "")
                val date = inFmtSimple.parse(isoClean)
                date?.let { outFmt.format(it) } ?: run.updatedAt
            } catch (e: Exception) {
                run.updatedAt
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        run.templateName ?: "Checklist #${run.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Tienda: ${run.storeCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge de estado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when(run.status) {
                        "SUBMITTED" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "PENDING" -> Color(0xFFFFA726).copy(alpha = 0.1f)
                        else -> Color.Gray.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        when(run.status) {
                            "SUBMITTED" -> "Enviado"
                            "PENDING" -> "Borrador"
                            else -> run.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = when(run.status) {
                            "SUBMITTED" -> Color(0xFF4CAF50)
                            "PENDING" -> Color(0xFFFFA726)
                            else -> Color.Gray
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Progreso
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${run.answeredCount} de ${run.totalCount} items completados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { (run.answeredCount.toFloat() / run.totalCount.coerceAtLeast(1)) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Fecha
            Text(
                "Actualizado: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ✅ NUEVO: Mostrar quién respondió (solo para SUBMITTED)
            if (run.status == "SUBMITTED" && run.assignedTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Respondido por: ${run.assignedTo.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (run.status == "SUBMITTED") "Ver" else "Continuar")
                }

                if (canDelete) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}