package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun RunScreen(
    runId: Long,
    vm: RunsViewModel,
    onSubmitted: () -> Unit
) {
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Resumen de corrida #$runId", style = MaterialTheme.typography.headlineSmall)

            Button(
                enabled = !loading,
                onClick = { vm.submit(runId, onSubmitted) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar corrida")
            }

            if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}
