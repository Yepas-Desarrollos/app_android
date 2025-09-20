package mx.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.components.admin.AdminAccessButton
import mx.checklist.ui.components.admin.AdminBadge

@Composable
fun StoresScreen(
    vm: RunsViewModel,
    onStoreSelected: (String) -> Unit,
    onAdminAccess: (() -> Unit)? = null
) {
    val storesFlow = vm.getStores()
    val stores by storesFlow.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf(TextFieldValue("")) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header con título y badge admin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tiendas", style = MaterialTheme.typography.headlineMedium)
            AdminBadge()
        }

        // Botón de acceso admin
        onAdminAccess?.let { adminCallback ->
            AdminAccessButton(
                onAdminAccess = adminCallback,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar por código o nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        if (loading && stores.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }

        val filtered = remember(stores, query) {
            val q = query.text.trim().lowercase()
            if (q.isEmpty()) stores
            else stores.filter {
                it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(filtered) { s ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                        .then(if (s.isActive) Modifier.clickable { onStoreSelected(s.code) } else Modifier),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${s.code} — ${s.name}", style = MaterialTheme.typography.titleMedium)
                        val statusTxt = if (s.isActive) "Activa" else "Inactiva"
                        Text(statusTxt, color = if (s.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        if (!s.isActive) {
                            Text("No seleccionable (inactiva)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
