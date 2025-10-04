package mx.checklist.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.data.api.dto.SectionTemplateDto
import mx.checklist.ui.vm.AdminViewModel
import kotlin.math.roundToInt

/**
 * Pantalla para crear/editar una sección de un template
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSectionFormScreen(
    vm: AdminViewModel,
    templateId: Long, // ID del template/checklist al que pertenece la sección
    sectionId: Long?, // null = crear nueva, not null = editar existente
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onCreateItem: (Long) -> Unit, // El parámetro es el ID de la sección (sectionId)
    onEditItem: (Long, Long) -> Unit // El primer parámetro es el ID de la sección, el segundo es el ID del ítem
) {
    // Estados locales del formulario
    var name by remember { mutableStateOf("") }
    var percentage by remember { mutableStateOf("0.0") }
    var orderIndex by remember { mutableStateOf("0") }
    var showDeleteDialog by remember { mutableStateOf<ItemTemplateDto?>(null) }
    var showDistributeDialog by remember { mutableStateOf(false) }
    
    // Estados del ViewModel
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val currentTemplate by vm.currentTemplate.collectAsStateWithLifecycle()
    val success by vm.operationSuccess.collectAsStateWithLifecycle()
    
    val currentSection = remember(currentTemplate, sectionId) {
        if (sectionId != null) {
            currentTemplate?.sections?.find { it.id == sectionId }
        } else null
    }
    
    val isEditing = sectionId != null
    val title = if (isEditing) "Editar Sección" else "Crear Sección"

    // Cargar template
    LaunchedEffect(templateId) {
        vm.loadTemplate(templateId)
    }
    
    // Actualizar campos cuando se carga la sección
    LaunchedEffect(currentSection) {
        val section = currentSection // Variable local para smart cast
        section?.let {
            name = it.name
            percentage = it.percentage?.toString() ?: "0.0"
            orderIndex = it.orderIndex.toString()
        }
    }

    // Navegar de vuelta cuando se guarda exitosamente
    LaunchedEffect(success) {
        val currentSuccess = success
        if (currentSuccess != null && !currentSuccess.contains("error", ignoreCase = true)) {
            // Solo navegar si realmente fue exitoso
            kotlinx.coroutines.delay(500) // Reducido para respuesta más rápida
            onSaved()
        }
    }

    // Limpiar mensaje de éxito después de un tiempo
    LaunchedEffect(success) {
        val currentSuccess = success
        if (currentSuccess != null) {
            kotlinx.coroutines.delay(3000) // Limpiar después de 3 segundos
            vm.clearSuccess()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con botón de volver
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Estados de carga/error
        item {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

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
        }

        // Formulario de la sección
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Información de la Sección",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la Sección *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        isError = name.isBlank()
                    )

                    OutlinedTextField(
                        value = percentage,
                        onValueChange = { 
                            // Validar que sea un número y esté entre 0-100
                            val newValue = it.replace(",", ".")
                            if (newValue.isEmpty() || newValue == ".") {
                                percentage = newValue
                            } else {
                                try {
                                    val num = newValue.toDouble()
                                    if (num >= 0 && num <= 100) {
                                        percentage = newValue
                                    }
                                } catch (e: Exception) {
                                    // No actualizar si no es un número válido
                                }
                            }
                        },
                        label = { Text("Porcentaje (0-100) *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        isError = percentage.toDoubleOrNull() == null || percentage.toDouble() < 0 || percentage.toDouble() > 100,
                        supportingText = {
                            Text("El porcentaje representa la contribución de esta sección al total del checklist.")
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                vm.clearError()
                                val percentageValue = percentage.toDoubleOrNull() ?: 0.0
                                val orderIndexValue = orderIndex.toIntOrNull() ?: 0
                                
                                if (isEditing && sectionId != null) {
                                    vm.updateSection(
                                        sectionId = sectionId,
                                        name = name,
                                        percentage = percentageValue,
                                        orderIndex = orderIndexValue
                                        // Removido onSuccess = onSaved para evitar navegación prematura
                                    )
                                } else {
                                    vm.createSection(
                                        checklistId = templateId, // Usar templateId aquí
                                        name = name,
                                        percentage = percentageValue,
                                        orderIndex = orderIndexValue
                                        // Removido onSuccess = onSaved para evitar navegación prematura
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !loading && name.isNotBlank() && 
                                     percentage.toDoubleOrNull() != null && 
                                     percentage.toDouble() >= 0 && 
                                     percentage.toDouble() <= 100
                        ) {
                            Text(if (isEditing) "Actualizar" else "Crear")
                        }
                    }
                }
            }
        }

        // Sección de ítems (solo para secciones existentes)
        if (isEditing && currentSection != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ítems de la Sección (${currentSection.items.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        showDistributeDialog = true
                                    }
                                ) {
                                    Text("Distribuir %")
                                }
                                
                                FilledTonalButton(
                                    onClick = { 
                                        // Enviamos el ID de la sección actual como parámetro
                                        // La implementación de onCreateItem decide qué hacer con él
                                        val idToPass = sectionId ?: 0L
                                        onCreateItem(idToPass)
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Agregar Ítem")
                                }
                            }
                        }

                        if (currentSection.items.isEmpty()) {
                            Text(
                                text = "No hay ítems en esta sección.\nAgrega ítems para completar tu checklist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    currentSection.items.sortedBy { it.orderIndex },
                                    key = { it.id ?: 0L }
                                ) { item ->
                                    SectionItemCard(
                                        item = item,
                                        onEdit = { 
                                            // Pasamos el ID de la sección y el ID del ítem
                                            // La implementación de onEditItem decide qué hacer con ellos
                                            val sectionIdToPass = sectionId ?: 0L
                                            onEditItem(sectionIdToPass, item.id ?: 0L)
                                        },
                                        onDelete = { showDeleteDialog = item }
                                    )
                                }
                            }
                            
                            // Información de porcentajes
                            val totalPercentage = currentSection.items.sumOf { it.percentage ?: 0.0 }
                            val formattedTotal = "%.2f".format(totalPercentage)
                            
                            Text(
                                text = "Total: $formattedTotal% ${if (formattedTotal.toDouble().roundToInt() != 100) "(Debe sumar 100%)" else "✓"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (formattedTotal.toDouble().roundToInt() != 100) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.tertiary
                            )
                        }

                        // Validación de suma de porcentajes de items
                        val items = currentSection.items
                        val totalItemPercentage = items.sumOf { it.percentage ?: 0.0 }
                        val isItemPercentageValid = totalItemPercentage.roundToInt() == 100

                        // Mostrar advertencia si la suma de porcentajes no es 100
                        if (!isItemPercentageValid && items.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    text = "La suma de porcentajes de los ítems debe ser 100%. Actual: ${"%.2f".format(totalItemPercentage)}%",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Botón para distribuir porcentajes equitativamente entre ítems
                        if (items.isNotEmpty()) {
                            Button(
                                onClick = {
                                    showDistributeDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Distribuir porcentajes equitativamente entre ítems")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog de confirmación para eliminar item
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Ítem") },
            text = { 
                Text("¿Estás seguro de que quieres eliminar el ítem '${item.title}'?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sectionId != null && item.id != null) {
                            vm.deleteSectionItem(sectionId, item.id!!) {
                                vm.loadTemplate(templateId) // Usar templateId aquí
                                showDeleteDialog = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Dialog para distribuir porcentajes
    if (showDistributeDialog && sectionId != null) {
        AlertDialog(
            onDismissRequest = { showDistributeDialog = false },
            title = { Text("Distribuir Porcentajes") },
            text = { 
                Text("¿Quieres distribuir equitativamente los porcentajes entre todos los ítems? Esto asignará automáticamente un porcentaje igual a cada ítem, sumando 100%.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sectionId != null) {
                            vm.distributeItemPercentages(sectionId) {
                                vm.loadTemplate(templateId) // Usar templateId aquí
                                showDistributeDialog = false
                            }
                        }
                    }
                ) {
                    Text("Distribuir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDistributeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionItemCard(
    item: ItemTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${item.orderIndex} - ${item.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Mostrar porcentaje
                    Text(
                        text = "Porcentaje: ${item.percentage?.let { "%.2f".format(it) } ?: "0.00"}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (item.expectedType != null) {
                        Text(
                            text = "Tipo: ${item.expectedType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}