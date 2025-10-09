package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.RunSummaryDto
import mx.checklist.data.auth.AuthState
import mx.checklist.ui.components.admin.AdminBadge
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.vm.RunsViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryScreen(
    vm: RunsViewModel,
    adminVM: AdminViewModel? = null,
    onOpenRun: (runId: Long, storeCode: String, templateName: String?) -> Unit
) {
    var tab by remember { mutableStateOf(0) } // 0 = Borradores, 1 = Enviadas
    var showDeleteDialog by remember { mutableStateOf<RunSummaryDto?>(null) }

    // Verificar si el usuario es admin
    val isAdmin = adminVM != null

    LaunchedEffect(Unit) {
        vm.loadPendingRuns(all = true) // todos los borradores
        vm.loadHistoryRuns()           // enviadas recientes
    }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val pending by vm.pendingRunsFlow().collectAsStateWithLifecycle()
    val history by vm.historyRunsFlow().collectAsStateWithLifecycle()

    // Estados del adminVM si está disponible
    val adminLoading = adminVM?.loading?.collectAsStateWithLifecycle()?.value ?: false
    val adminError = adminVM?.error?.collectAsStateWithLifecycle()?.value
    val adminSuccess = adminVM?.operationSuccess?.collectAsStateWithLifecycle()?.value

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Historial", style = MaterialTheme.typography.headlineSmall)

            if (isAdmin) {
                AdminBadge()
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { tab = 0 }, modifier = Modifier.weight(1f), enabled = tab != 0) { Text("Borradores") }
            Button(onClick = { tab = 1 }, modifier = Modifier.weight(1f), enabled = tab != 1) { Text("Enviadas") }
        }

        if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)
        if (adminError != null) Text("Error Admin: $adminError", color = MaterialTheme.colorScheme.error)

        adminSuccess?.let { successMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = successMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Divider()

        if (tab == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pending, key = { it.id }) { r ->
                    DraftCard(
                        r = r,
                        loading = loading,
                        onResume = { onOpenRun(r.id, r.storeCode ?: "", r.templateName) },
                        onDelete = { vm.deleteRun(r.id) }
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.id }) { r ->
                    HistoryCard(
                        r = r,
                        loading = loading || adminLoading,
                        isAdmin = isAdmin,
                        onOpen = { onOpenRun(r.id, r.storeCode ?: "", r.templateName) },
                        onDelete = if (isAdmin) { { showDeleteDialog = r } } else null
                    )
                }
            }
        }
    }

    // Dialog de confirmación para eliminar corrida enviada (solo para admins)
    showDeleteDialog?.let { run ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Corrida Enviada") },
            text = {
                Column {
                    Text("¿Estás seguro de que deseas eliminar esta corrida enviada?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Template: ${run.templateName}")
                    Text("Tienda: ${run.storeCode}")
                    Text("Items: ${run.totalCount}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Esta acción no se puede deshacer y eliminará todas las respuestas asociadas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminVM?.forceDeleteRun(run.id) {
                            // Recargar historial después de eliminar
                            vm.loadHistoryRuns()
                            // Limpiar mensajes anteriores
                            adminVM.clearError()
                            adminVM.clearSuccess()
                        }
                        showDeleteDialog = null
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

private fun isoUtcToLocalHourMinute(iso: String): String {
    // Backend envía ISO en UTC (puede venir con 'Z' o sin ella)
    val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
    inFmt.timeZone = TimeZone.getTimeZone("UTC")

    val outFmt = SimpleDateFormat("HH:mm 'hs' dd/MM", Locale.getDefault())
    outFmt.timeZone = TimeZone.getDefault() // Hora local del dispositivo

    return try {
        // Remover la 'Z' si existe
        val isoClean = iso.replace("Z", "")
        val date = inFmt.parse(isoClean)
        if (date != null) outFmt.format(date) else iso
    } catch (_: Exception) {
        // Si falla, intentar sin milisegundos
        try {
            val inFmtSimple = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inFmtSimple.timeZone = TimeZone.getTimeZone("UTC")
            val isoClean = iso.replace("Z", "")
            val date = inFmtSimple.parse(isoClean)
            if (date != null) outFmt.format(date) else iso
        } catch (_: Exception) {
            iso
        }
    }
}

@Composable
fun DraftCard(
    r: RunSummaryDto,
    loading: Boolean,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (r.totalCount > 0) "${r.answeredCount}/${r.totalCount}" else "0/0"
    val updatedHuman = remember(r.updatedAt) { isoUtcToLocalHourMinute(r.updatedAt) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.templateName ?: "Checklist", style = MaterialTheme.typography.titleMedium)
            Text("Tienda: ${r.storeCode ?: "-"}  •  Estado: ${r.status}  •  Avance: $progress", style = MaterialTheme.typography.bodySmall)
            Text("Actualizado: $updatedHuman", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !loading, onClick = onResume, modifier = Modifier.weight(1f)) { Text("Reanudar") }
                OutlinedButton(enabled = !loading, onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Eliminar") }
            }
        }
    }
}

@Composable
fun HistoryCard(
    r: RunSummaryDto,
    loading: Boolean,
    isAdmin: Boolean = false,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val progress = if (r.totalCount > 0) "${r.answeredCount}/${r.totalCount}" else "0/0"
    val updatedHuman = remember(r.updatedAt) { isoUtcToLocalHourMinute(r.updatedAt) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.templateName ?: "Checklist", style = MaterialTheme.typography.titleMedium)
            Text("Tienda: ${r.storeCode ?: "-"}  •  Estado: ${r.status}  •  Avance: $progress", style = MaterialTheme.typography.bodySmall)
            Text("Enviado: $updatedHuman", style = MaterialTheme.typography.bodySmall)

            // ✅ NUEVO: Mostrar quién respondió (solo para SUBMITTED)
            if (r.status == "SUBMITTED" && r.assignedTo != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Respondido por: ${r.assignedTo?.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Solo mostrar botón borrar según permisos específicos
            val canDelete = remember(r.status, isAdmin, onDelete) {
                when {
                    r.status == "DRAFT" -> isAdmin && onDelete != null // Borradores: cualquier admin/manager
                    r.status == "SUBMITTED" -> {
                        // Enviadas: solo ADMIN real
                        val isRealAdmin = AuthState.roleCode == "ADMIN"
                        isRealAdmin && onDelete != null
                    }
                    else -> false
                }
            }
            
            if (canDelete) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !loading,
                        onClick = onOpen,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ver")
                    }
                    OutlinedButton(
                        enabled = !loading,
                        onClick = { onDelete?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            } else {
                Button(enabled = !loading, onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Ver") }
            }
        }
    }
}