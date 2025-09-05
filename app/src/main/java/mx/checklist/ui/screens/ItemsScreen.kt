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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
    readOnly: Boolean = false
) {
    // Carga ítems una sola vez por corrida
    LaunchedEffect(runId) { vm.loadRunItems(runId) }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val items by vm.runItemsFlow().collectAsStateWithLifecycle()

    val answered = items.count { !it.responseStatus.isNullOrEmpty() }
    val total = items.size.coerceAtLeast(1)
    val allAnswered = items.isNotEmpty() && answered == items.size

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Items de la Corrida $runId ($storeCode)",
            style = MaterialTheme.typography.headlineSmall
        )

        // Banner de estado / errores
        if (readOnly) {
            Text(
                "Corrida enviada (solo lectura)",
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }

        // Progreso
        Text("$answered / ${items.size} respondidos")
        LinearProgressIndicator(
            progress = answered / total.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

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
                    readOnly = readOnly,
                    onSave = { status, respText, number ->
                        vm.respond(item.id, status, respText, number)
                    }
                )
            }
        }

        // Botón Enviar checklist (oculto en solo lectura)
        if (!readOnly) {
            Button(
                enabled = allAnswered && !loading,
                onClick = { vm.submit(runId) { onSubmit() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Enviando…" else "Enviar checklist")
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: RunItemDto,
    readOnly: Boolean,
    onSave: (status: String?, text: String?, number: Double?) -> Unit
) {
    // Estado local por ítem (estable por id)
    var status by remember(item.id) { mutableStateOf(item.responseStatus.orEmpty()) }
    var respText by remember(item.id) { mutableStateOf(item.responseText.orEmpty()) }
    var numberStr by remember(item.id) { mutableStateOf(item.responseNumber?.toString().orEmpty()) }
    var justSaved by remember(item.id) { mutableStateOf(false) }

    // Re-sincroniza si el VM refresca el ítem (p. ej. normalización del backend)
    LaunchedEffect(item.id, item.responseStatus, item.responseText, item.responseNumber) {
        status = item.responseStatus.orEmpty()
        respText = item.responseText.orEmpty()
        numberStr = item.responseNumber?.toString().orEmpty()
    }

    val dirty = status != item.responseStatus.orEmpty() ||
            respText != item.responseText.orEmpty() ||
            numberStr != item.responseNumber?.toString().orEmpty()

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Ítem #${item.orderIndex} (id ${item.id})",
                style = MaterialTheme.typography.titleMedium
            )

            // Estatus con chips (OK / FAIL / NA)
            Text("Estatus (requerido)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("OK", "FAIL", "NA").forEach { opt ->
                    FilterChip(
                        selected = status.equals(opt, ignoreCase = true),
                        onClick = {
                            if (!readOnly) {
                                status = opt
                                justSaved = false
                            }
                        },
                        enabled = !readOnly,
                        label = { Text(opt) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            // Texto opcional
            OutlinedTextField(
                value = respText,
                onValueChange = { if (!readOnly) respText = it },
                label = { Text("Texto (opcional)") },
                enabled = !readOnly,
                modifier = Modifier.fillMaxWidth()
            )

            // Número opcional
            OutlinedTextField(
                value = numberStr,
                onValueChange = { input ->
                    if (!readOnly) {
                        numberStr = input.filter { ch -> ch.isDigit() || ch == '.' }
                    }
                },
                label = { Text("Número (opcional)") },
                enabled = !readOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Botón Guardar (oculto en solo lectura)
            if (!readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val parsed = numberStr.toDoubleOrNull()
                    Button(
                        enabled = status.isNotBlank() && dirty,
                        onClick = {
                            onSave(status.trim(), respText.trim().ifBlank { null }, parsed)
                            justSaved = true
                        }
                    ) {
                        Text(if (justSaved && !dirty) "Guardado ✓" else "Guardar respuesta")
                    }
                }
            }
        }
    }
}
