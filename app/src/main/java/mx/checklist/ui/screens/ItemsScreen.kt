package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.foundation.text.KeyboardOptions
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
    onSubmit: () -> Unit
) {
    // Carga SOLO una vez por runId
    LaunchedEffect(runId) { vm.loadRunItems(runId) }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val items by vm.runItemsFlow().collectAsStateWithLifecycle()

    val allAnswered = items.isNotEmpty() && items.all { !it.responseStatus.isNullOrEmpty() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Items de la Corrida $runId ($storeCode)", style = MaterialTheme.typography.headlineSmall)

        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }

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
                    onSave = { status, respText, number ->
                        vm.respond(item.id, status, respText, number)
                    }
                )
            }
        }

        Button(
            enabled = allAnswered && !loading,
            onClick = { vm.submit(runId) { onSubmit() } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar checklist")
        }
    }
}

@Composable
private fun ItemCard(
    item: RunItemDto,
    onSave: (status: String?, text: String?, number: Double?) -> Unit
) {
    // Estado local por ítem, estable por clave id
    var status by remember(item.id) { mutableStateOf(item.responseStatus.orEmpty()) }
    var respText by remember(item.id) { mutableStateOf(item.responseText.orEmpty()) }
    var numberStr by remember(item.id) { mutableStateOf(item.responseNumber?.toString().orEmpty()) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ítem #${item.orderIndex} (id ${item.id})", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = status,
                onValueChange = { status = it },
                label = { Text("Estatus (requerido)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = respText,
                onValueChange = { respText = it },
                label = { Text("Texto (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = numberStr,
                onValueChange = { input ->
                    numberStr = input.filter { ch -> ch.isDigit() || ch == '.' }
                },
                label = { Text("Número (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val parsed = numberStr.toDoubleOrNull()
                Button(
                    enabled = status.isNotBlank(),
                    onClick = { onSave(status.trim(), respText.trim().ifBlank { null }, parsed) }
                ) { Text("Guardar respuesta") }
            }
        }
    }
}
