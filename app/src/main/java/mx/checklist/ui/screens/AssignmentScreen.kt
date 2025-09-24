package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.AssignableUserDto
import kotlinx.coroutines.flow.StateFlow
import mx.checklist.data.api.dto.AssignmentSummaryDto
import mx.checklist.data.api.dto.AssignedStoreDto
import mx.checklist.data.api.dto.UserAssignmentDto
import mx.checklist.ui.vm.AssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    assignmentVM: AssignmentViewModel,
    onBack: () -> Unit = { }
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Usuarios", "Resumen")
    
    // Estados del ViewModel
    val users: List<AssignableUserDto> by assignmentVM.users.collectAsStateWithLifecycle()
    val summary: List<AssignmentSummaryDto> by assignmentVM.summary.collectAsStateWithLifecycle()
    val loading: Boolean by assignmentVM.loading.collectAsStateWithLifecycle()
    val error: String? by assignmentVM.error.collectAsStateWithLifecycle()
    val isAssigning by assignmentVM.isAssigning.collectAsStateWithLifecycle()
    val snackbarMessage by assignmentVM.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            assignmentVM.consumeSnackbarMessage()
        }
    }
    
    // Cargar datos al inicio
    LaunchedEffect(Unit) {
        assignmentVM.loadAssignableUsers()
        assignmentVM.loadAssignmentSummary()
        assignmentVM.loadSectors()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SnackbarHost(hostState = snackbarHostState)
        // TopAppBar
        TopAppBar(
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Panel de Asignaciones")
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }
        )
        
        // TabRow
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Contenido según tab seleccionado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> UsersContent(
                    users = users,
                    loading = loading,
                    error = error,
                    sectorsFlow = assignmentVM.sectors,
                    isAssigning = isAssigning,
                    onAssignUser = assignmentVM::assignUserToSectors
                )
                1 -> SummaryContent(
                    summary = summary,
                    loading = loading,
                    error = error
                )
            }
        }
    }
}

@Composable
fun UsersContent(
    users: List<AssignableUserDto>,
    loading: Boolean,
    error: String?,
    sectorsFlow: StateFlow<List<Int>>,
    isAssigning: Boolean,
    onAssignUser: (Long, List<Int>) -> Unit
) {
    val sectors by sectorsFlow.collectAsStateWithLifecycle()
    var selectedUser by remember { mutableStateOf<AssignableUserDto?>(null) }
    var selectedSectors by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var originalSectors by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showAssignDialog by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Usuarios Disponibles para Asignación",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Estado de carga
        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error
        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $errorMsg",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Lista de usuarios
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    isAssigning = isAssigning,
                    onAssign = { 
                        selectedUser = user
                        val current = user.stores.flatMap { s -> s.sectors }.mapNotNull { it.toIntOrNull() }.toSet()
                        originalSectors = current
                        selectedSectors = current
                        showAssignDialog = true
                    }
                )
            }
            
            if (users.isEmpty() && !loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay usuarios disponibles para asignación",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog de asignación
    if (showAssignDialog && selectedUser != null) {
        AssignmentDialog(
            user = selectedUser!!,
            availableSectors = sectors,
            selectedSectors = selectedSectors,
            originalSectors = originalSectors,
            onSectorsChanged = { selectedSectors = it },
            isAssigning = isAssigning,
            onConfirm = {
                onAssignUser(selectedUser!!.id, selectedSectors.toList())
                showAssignDialog = false
                selectedUser = null
                selectedSectors = emptySet()
                originalSectors = emptySet()
            },
            onDismiss = {
                showAssignDialog = false
                selectedUser = null
                selectedSectors = emptySet()
                originalSectors = emptySet()
            }
        )
    }
}

@Composable
fun UserCard(
    user: AssignableUserDto,
    isAssigning: Boolean = false,
    onAssign: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val distinctSectors = remember(user.stores) {
                user.stores.flatMap { it.sectors }.mapNotNull { it.toIntOrNull() }.toSet().sorted()
            }
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Email: ${user.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rol: ${user.roleCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (distinctSectors.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sectores actuales: ${distinctSectors.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Button(
                    onClick = onAssign,
                    enabled = !isAssigning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (distinctSectors.isEmpty()) "Asignar" else "Editar")
                }
            }
            
            if (user.stores.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Ocultar tiendas" else "Ver tiendas (${user.stores.size})")
                }
                if (expanded) {
                    user.stores.forEach { store: AssignedStoreDto ->
                        Text(
                            text = "• ${store.name} (${store.code}) - Sectores: ${store.sectors.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentDialog(
    user: AssignableUserDto,
    availableSectors: List<Int>,
    selectedSectors: Set<Int>,
    originalSectors: Set<Int>,
    onSectorsChanged: (Set<Int>) -> Unit,
    isAssigning: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Asignar sectores a ${user.name}")
        },
        text = {
            Column {
                val added = selectedSectors - originalSectors
                val removed = originalSectors - selectedSectors
                Text(
                    text = "Selecciona los sectores que quieres asignar/desasignar:",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (originalSectors.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Originales: ${originalSectors.sorted().joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (added.isNotEmpty() || removed.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            if (added.isNotEmpty()) append("+ ${added.sorted().joinToString()}  ")
                            if (removed.isNotEmpty()) append("- ${removed.sorted().joinToString()}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (removed.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSectorsChanged(availableSectors.toSet()) }) { Text("Todos") }
                    TextButton(onClick = { onSectorsChanged(originalSectors) }) { Text("Revertir") }
                }
                Spacer(Modifier.height(4.dp))
                availableSectors.sorted().forEach { sector ->
                    val isSelected = sector in selectedSectors
                    val wasOriginal = sector in originalSectors
                    val labelSuffix = when {
                        isSelected && !wasOriginal -> " (nuevo)"
                        !isSelected && wasOriginal -> " (remover)"
                        else -> ""
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    val newSelection = if (isSelected) selectedSectors - sector else selectedSectors + sector
                                    onSectorsChanged(newSelection)
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sector $sector$labelSuffix",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                labelSuffix.contains("nuevo") -> MaterialTheme.colorScheme.primary
                                labelSuffix.contains("remover") -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isAssigning && (selectedSectors != originalSectors)
            ) {
                if (isAssigning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isAssigning) "Asignando..." else "Asignar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SummaryContent(
    summary: List<AssignmentSummaryDto>,
    loading: Boolean,
    error: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Resumen de Asignaciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Estado de carga
        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error
        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $errorMsg",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Lista de resumen
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summary, key = { "${it.storeCode}_${it.sector}" }) { item ->
                SummaryCard(summary = item)
            }
            
            if (summary.isEmpty() && !loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay asignaciones registradas",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    summary: AssignmentSummaryDto
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${summary.storeName} (${summary.storeCode})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sector: ${summary.sector}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${summary.assignedUsers}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Usuarios asignados
            if (summary.users.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Usuarios asignados:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                summary.users.forEach { user: UserAssignmentDto ->
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${user.name} (${user.roleCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}