package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun HomeScreen(
    vm: RunsViewModel,
    authVM: AuthViewModel? = null,
    onNuevaCorrida: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogout: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Checklist Yepas", style = MaterialTheme.typography.headlineMedium)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNuevaCorrida, modifier = Modifier.weight(1f)) {
                Text("Nueva corrida")
            }
            Button(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                Text("Historial")
            }
        }

        // Botón de logout si se proporciona authVM
        authVM?.let {
            OutlinedButton(
                onClick = {
                    vm.clearCache()
                    authVM.logout(onLogout)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}
