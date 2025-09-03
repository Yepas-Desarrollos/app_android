package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun RunScreen(
    runId: Long,
    onSubmitted: () -> Unit,
    vm: RunsViewModel = viewModel()
) {
    val items by vm.items.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(runId) { vm.loadRunItems(runId) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Corrida #$runId", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        items.forEach { it ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Ítem ${it.orderIndex}")
                    Text("Estado: ${it.responseStatus ?: "-"}")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.respond(it.id, "OK") }) { Text("OK") }
                        Button(onClick = { vm.respond(it.id, "NOK") }) { Text("NOK") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            vm.submit(runId,
                onOk = onSubmitted,
                onError = { error = it.message })
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Enviar corrida")
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}
