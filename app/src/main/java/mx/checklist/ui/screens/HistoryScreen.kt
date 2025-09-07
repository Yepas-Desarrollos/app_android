package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.RunSummaryDto
import mx.checklist.ui.vm.RunsViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryScreen(
    vm: RunsViewModel,
    onOpenRun: (runId: Long, storeCode: String, templateName: String?) -> Unit
) {
    var tab by remember { mutableStateOf(0) } // 0 = Borradores, 1 = Enviadas

    LaunchedEffect(Unit) {
        vm.loadPendingRuns(all = true) // todos los borradores
        vm.loadHistoryRuns()           // enviadas recientes
    }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val pending by vm.pendingRunsFlow().collectAsStateWithLifecycle()
    val history by vm.historyRunsFlow().collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Historial", style = MaterialTheme.typography.headlineSmall)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { tab = 0 }, modifier = Modifier.weight(1f), enabled = tab != 0) { Text("Borradores") }
            Button(onClick = { tab = 1 }, modifier = Modifier.weight(1f), enabled = tab != 1) { Text("Enviadas") }
        }

        if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

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
                        loading = loading,
                        onOpen = { onOpenRun(r.id, r.storeCode ?: "", r.templateName) }
                    )
                }
            }
        }
    }
}

private fun isoUtcToLocalHourMinute(iso: String): String {
    // Backend envía ISO en UTC (Z). Lo convertimos a hora local.
    val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    inFmt.timeZone = TimeZone.getTimeZone("UTC")
    val outFmt = SimpleDateFormat("HH:mm 'hs' dd/MM", Locale.getDefault())
    outFmt.timeZone = TimeZone.getDefault()
    return try {
        val date = inFmt.parse(iso)
        if (date != null) outFmt.format(date) else iso
    } catch (_: Exception) {
        iso
    }
}

@Composable
private fun DraftCard(
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
private fun HistoryCard(
    r: RunSummaryDto,
    loading: Boolean,
    onOpen: () -> Unit
) {
    val progress = if (r.totalCount > 0) "${r.answeredCount}/${r.totalCount}" else "0/0"
    val updatedHuman = remember(r.updatedAt) { isoUtcToLocalHourMinute(r.updatedAt) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.templateName ?: "Checklist", style = MaterialTheme.typography.titleMedium)
            Text("Tienda: ${r.storeCode ?: "-"}  •  Estado: ${r.status}  •  Avance: $progress", style = MaterialTheme.typography.bodySmall)
            Text("Enviado: $updatedHuman", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Button(enabled = !loading, onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Ver") }
        }
    }
}
