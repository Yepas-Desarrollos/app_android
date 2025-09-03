package mx.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.checklist.ui.navigation.NavRoutes
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun StoresScreen(
    onStoreSelected: (String) -> Unit,
    vm: RunsViewModel = viewModel()
) {
    val stores by vm.stores.collectAsState()

    LaunchedEffect(Unit) { vm.loadStores() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tiendas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        stores.forEach { s ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onStoreSelected(s.code) }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("${s.code} - ${s.name}", style = MaterialTheme.typography.titleMedium)
                    if (!s.isActive) Text("Inactiva", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
