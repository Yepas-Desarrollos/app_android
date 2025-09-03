package mx.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun TemplatesScreen(
    storeCode: String,
    onRunCreated: (Long) -> Unit,
    vm: RunsViewModel = viewModel()
) {
    val tmpls by vm.templates.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadTemplates() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Plantillas para $storeCode", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        tmpls.forEach { t ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        vm.createRun(storeCode, t.id,
                            onCreated = { runId -> onRunCreated(runId) },
                            onError = { error = it.message })
                    }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(t.name, style = MaterialTheme.typography.titleMedium)
                    t.frequency?.let { Text("Frecuencia: $it") }
                }
            }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}
