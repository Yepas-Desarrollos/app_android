package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNuevaCorrida: () -> Unit,
    onVerTiendas: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text("Checklist Yepas", style = MaterialTheme.typography.headlineMedium)
        Text("Elige qué quieres hacer")

        Button(onClick = onNuevaCorrida, modifier = Modifier.fillMaxWidth()) {
            Text("Nueva corrida (elegir tienda)")
        }
        Button(onClick = onVerTiendas, modifier = Modifier.fillMaxWidth()) {
            Text("Ver tiendas")
        }
    }
}
