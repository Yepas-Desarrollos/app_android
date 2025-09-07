package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.RunItemDto
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun ItemsScreen(
    runId: Long,
    storeCode: String,
    vm: RunsViewModel,
    onSubmit: () -> Unit,
    readOnly: Boolean = false,
    templateName: String? = null
) {
    // Cargar ítems + info del run (para status y nombre de plantilla)
    LaunchedEffect(runId) {
        vm.loadRunItems(runId)
        vm.loadRunInfo(runId)
    }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val items by vm.runItemsFlow().collectAsStateWithLifecycle()
    val runInfo by vm.runInfoFlow().collectAsStateWithLifecycle()

    // Auto solo-lectura si el backend marca SUBMITTED
    val readOnlyAuto = runInfo?.status == "SUBMITTED"
    val isReadOnly = readOnly || readOnlyAuto

    // Mostrar nombre del checklist: param > runInfo > fallback
    val shownTemplateName = templateName ?: runInfo?.templateName

    val answered = items.count { !it.responseStatus.isNullOrEmpty() }
    val total = items.size.coerceAtLeast(1)
    val allAnswered = items.isNotEmpty() && answered == items.size

    // ✨ Retroalimentación al enviar
    var showSubmittedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!shownTemplateName.isNullOrBlank()) {
            Text("Checklist: $shownTemplateName", style = MaterialTheme.typography.titleLarge)
            Text("Corrida #$runId — Tienda $storeCode", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Items de la Corrida $runId ($storeCode)", style = MaterialTheme.typography.headlineSmall)
        }

        if (isReadOnly) Text("Corrida enviada (solo lectura)", color = MaterialTheme.colorScheme.primary)
        if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

        Text("$answered / ${items.size} respondidos")
        LinearProgressIndicator(progress = answered / total.toFloat(), modifier = Modifier.fillMaxWidth())

        if (loading && items.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    readOnly = isReadOnly,
                    onSave = { status, respText, number ->
                        vm.respond(item.id, status, respText, number)
                    }
                )
            }
        }

        if (!isReadOnly) {
            Button(
                enabled = allAnswered && !loading,
                onClick = {
                    vm.submit(runId) {
                        // mostrar retroalimentación y luego navegar
                        showSubmittedDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Enviando…" else "Enviar checklist") }
        }
    }

    if (showSubmittedDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmittedDialog = false
                onSubmit()
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubmittedDialog = false
                    onSubmit()
                }) { Text("OK") }
            },
            title = { Text("Checklist enviado") },
            text = { Text("Tu checklist se envió correctamente.") }
        )
    }
}

@Composable
private fun ItemCard(
    item: RunItemDto,
    readOnly: Boolean,
    onSave: (status: String?, text: String?, number: Double?) -> Unit
) {
    // Título/metas ESTABLES (no se pierden tras respond())
    val initialTitle = remember(item.id) {
        item.itemTemplate?.title?.takeIf { it.isNotBlank() }
            ?: "Ítem #${item.orderIndex} (id ${item.id})"
    }
    val initialCategory = remember(item.id) { item.itemTemplate?.category.orEmpty() }
    val initialSubcategory = remember(item.id) { item.itemTemplate?.subcategory.orEmpty() }

    var status by remember(item.id) { mutableStateOf(item.responseStatus.orEmpty()) }
    var respText by remember(item.id) { mutableStateOf(item.responseText.orEmpty()) }
    var numberStr by remember(item.id) { mutableStateOf(item.responseNumber?.toString().orEmpty()) }
    var justSaved by remember(item.id) { mutableStateOf(false) }

    // Solo re-sincroniza respuestas; no toques título/metas
    LaunchedEffect(item.id, item.responseStatus, item.responseText, item.responseNumber) {
        status = item.responseStatus.orEmpty()
        respText = item.responseText.orEmpty()
        numberStr = item.responseNumber?.toString().orEmpty()
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(initialTitle, style = MaterialTheme.typography.titleMedium)

            val meta = listOfNotNull(
                initialCategory.takeIf { it.isNotBlank() },
                initialSubcategory.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)

            Text("Estatus (requerido)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("OK", "FAIL", "NA").forEach { opt ->
                    FilterChip(
                        selected = status.equals(opt, ignoreCase = true),
                        onClick = { if (!readOnly) { status = opt; justSaved = false } },
                        enabled = !readOnly,
                        label = { Text(opt) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            OutlinedTextField(
                value = respText,
                onValueChange = { if (!readOnly) respText = it },
                label = { Text("Texto (opcional)") },
                enabled = !readOnly,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = numberStr,
                onValueChange = { input ->
                    if (!readOnly) numberStr = input.filter { ch -> ch.isDigit() || ch == '.' }
                },
                label = { Text("Número (opcional)") },
                enabled = !readOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (!readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val parsed = numberStr.toDoubleOrNull()
                    Button(
                        enabled = status.isNotBlank(),
                        onClick = {
                            onSave(status.trim(), respText.trim().ifBlank { null }, parsed)
                            justSaved = true
                        }
                    ) { Text(if (justSaved) "Guardado ✓" else "Guardar respuesta") }
                }
            }
        }
    }
}
