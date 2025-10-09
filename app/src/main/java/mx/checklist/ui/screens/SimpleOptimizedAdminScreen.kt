package mx.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.ui.vm.AdminViewModel

/**
 * AdminTemplateListScreen optimizada con datos reales
 */
@Composable
fun SimpleOptimizedAdminScreen(
    adminVM: AdminViewModel,
    onAssignments: () -> Unit = { },
    onTemplatesAdmin: () -> Unit = { }
) {
    // Estados del ViewModel
    val error by adminVM.error.collectAsStateWithLifecycle()
    val operationSuccess by adminVM.operationSuccess.collectAsStateWithLifecycle()
    
    // Cargar templates al inicio y limpiar estados
    LaunchedEffect(Unit) {
        adminVM.clearError()
        adminVM.clearSuccess()
        adminVM.loadTemplates()
    }
    
    // Limpiar mensaje de éxito después de un tiempo
    LaunchedEffect(operationSuccess) {
        if (operationSuccess != null) {
            kotlinx.coroutines.delay(3000)
            adminVM.clearSuccess()
        }
    }
    
    // Auto-dismiss de errores después de 5 segundos
    LaunchedEffect(error) {
        if (error != null) {
            kotlinx.coroutines.delay(5000)
            adminVM.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header principal
        Text(
            text = "Panel de Administración",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Botones principales de administración
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón Panel de Asignaciones
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Button(
                    onClick = onAssignments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Asignaciones",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Panel de\nAsignaciones", 
                             style = MaterialTheme.typography.labelMedium,
                             textAlign = TextAlign.Center)
                    }
                }
            }
            
            // Botón Checklists Admin
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Button(
                    onClick = onTemplatesAdmin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Checklists",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Checklists\nAdmin",
                             style = MaterialTheme.typography.labelMedium,
                             textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        // (Se removió la tarjeta "Estado del Sistema" a solicitud del usuario)
    }
}