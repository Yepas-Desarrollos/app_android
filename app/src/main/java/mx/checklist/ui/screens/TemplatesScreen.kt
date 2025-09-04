package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.TemplateDto
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun TemplatesScreen(
    storeCode: String,
    vm: RunsViewModel,
    onRunCreated: (Long, String) -> Unit
) {
    val templatesFlow = vm.getTemplates()
    val templates by templatesFlow.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    var existingRunId by remember { mutableStateOf(TextFieldValue("")) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Plantillas — Tienda $storeCode", style = MaterialTheme.typography.headlineMedium)

        // Continuar corrida existente
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Continuar corrida existente", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = existingRunId,
                    onValueChange = { existingRunId = it },
                    label = { Text("Run ID (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = existingRunId.text.trim().isNotEmpty(),
                    onClick = {
                        val id = existingRunId.text.trim().toLongOrNull()
                        if (id != null) onRunCreated(id, storeCode)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continuar") }
            }
        }

        // Crear nueva corrida
        Text("Crear nueva corrida", style = MaterialTheme.typography.titleMedium)

        if (loading && templates.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(templates) { tpl: TemplateDto ->
                ElevatedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tpl.name, style = MaterialTheme.typography.titleMedium)
                        val meta = listOfNotNull(
                            tpl.scope?.let { "Ámbito: $it" },
                            tpl.frequency?.let { "Frecuencia: $it" },
                            tpl.version?.let { "Versión: $it" }
                        ).joinToString(" • ")
                        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)

                        Button(
                            onClick = {
                                vm.createRun(storeCode = storeCode, templateId = tpl.id) { newRunId ->
                                    onRunCreated(newRunId, storeCode)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Crear corrida") }
                    }
                }
            }
        }
    }
}
