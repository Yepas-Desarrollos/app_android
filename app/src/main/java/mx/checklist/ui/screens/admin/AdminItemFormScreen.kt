package mx.checklist.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.ui.vm.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminItemFormScreen(
    vm: AdminViewModel,
    templateId: Long,
    itemId: Long? = null,
    initialTitle: String = "",
    initialExpectedType: String = "TEXT",
    initialCategory: String = "",
    initialSubcategory: String = "",
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    // Estados locales del formulario
    var title by remember { mutableStateOf(initialTitle) }
    var expectedType by remember { mutableStateOf(initialExpectedType) } // ✅ Usar valor inicial pasado
    var category by remember { mutableStateOf(initialCategory) }
    var subcategory by remember { mutableStateOf(initialSubcategory) }
    var percentage by remember { mutableStateOf("") } // ✅ NUEVO: Campo de porcentaje
    var expanded by remember { mutableStateOf(false) }
    var isDataLoaded by remember { mutableStateOf(false) } // ✅ NUEVO: Flag para controlar carga única

    // Estados del ViewModel
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val success by vm.operationSuccess.collectAsStateWithLifecycle()
    val currentTemplate by vm.currentTemplate.collectAsStateWithLifecycle()

    val isEditing = itemId != null
    val screenTitle = if (isEditing) "Editar Item" else "Crear Item"

    // ✅ CAMBIADO: Solo tipo BOOLEAN (Sí/No) por ahora
    val fieldTypes = listOf(
        "BOOLEAN" to "Sí / No"
    )

    // Limpiar mensajes de éxito después de 3 segundos
    LaunchedEffect(success) {
        val currentSuccess = success
        if (currentSuccess != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearSuccess()
        }
    }

    // ✅ CORREGIDO: Cargar template y datos del item
    LaunchedEffect(itemId, currentTemplate) {
        if (itemId != null) {
            // ✅ Capturar el valor en una variable local para evitar smart cast error
            val template = currentTemplate

            // Paso 1: Cargar template si no está disponible
            if (template == null || template.id != templateId) {
                println("[AdminItemFormScreen] 🔄 Cargando template $templateId para editar item $itemId")
                vm.loadTemplate(templateId)
                return@LaunchedEffect
            }

            // Paso 2: Cargar datos del item solo una vez
            if (!isDataLoaded) {
                println("[AdminItemFormScreen] 🔄 Cargando datos del item $itemId")

                val allItemsFromRoot = template.items ?: emptyList()
                val allItems = if (allItemsFromRoot.isNotEmpty()) {
                    allItemsFromRoot
                } else {
                    template.sections?.flatMap { it.items } ?: emptyList()
                }

                println("[AdminItemFormScreen] 📋 Total items en template: ${allItems.size}")

                val item = allItems.find { it.id == itemId }

                if (item != null) {
                    println("[AdminItemFormScreen] ✅ Item encontrado, cargando datos:")

                    title = item.title ?: ""
                    expectedType = item.expectedType ?: "BOOLEAN"
                    category = item.category ?: ""
                    subcategory = item.subcategory ?: ""
                    percentage = item.percentage?.toString() ?: ""
                    isDataLoaded = true

                    println("  - title: '${item.title}' -> cargado: '$title'")
                    println("  - expectedType: '${item.expectedType}' -> cargado: '$expectedType'")
                    println("  - category: '${item.category}' -> cargado: '$category'")
                    println("  - subcategory: '${item.subcategory}' -> cargado: '$subcategory'")
                    println("  - percentage: '${item.percentage}' -> cargado: '$percentage'")
                } else {
                    println("[AdminItemFormScreen] ❌ Item $itemId NO encontrado en template")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con botón de volver
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            
            Text(
                text = screenTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

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

        success?.let { successMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = successMsg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Formulario
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Información del Item",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    isError = title.isBlank()
                )

                // ✅ Campo de categoría OBLIGATORIO
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría * (ej: Limpieza, Inventario)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    isError = category.isBlank(),
                    supportingText = {
                        Text(
                            text = if (category.isBlank()) "La categoría es obligatoria para agrupar los items"
                                  else "Los items se agruparán por esta categoría",
                            color = if (category.isBlank()) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategoría (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                )

                // ✅ NUEVO: Campo para porcentaje (0-100)
                OutlinedTextField(
                    value = percentage,
                    onValueChange = {
                        // Asegurarse de que solo se ingresen números y el carácter de porcentaje
                        if (it.all { char -> char.isDigit() || char == '%' }) {
                            percentage = it
                        }
                    },
                    label = { Text("Porcentaje (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    supportingText = {
                        Text(
                            text = "Indica un porcentaje si es necesario (ej: 50%)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Selector de tipo de campo
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = fieldTypes[0].second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de campo *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !loading
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sí / No") },
                            onClick = {
                                expectedType = "BOOLEAN"
                                expanded = false
                            }
                        )
                    }
                }

                // Botones de acción
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
                            
                            // Validar campos obligatorios
                            if (title.isBlank()) {
                                // Mostrar error localmente - el ViewModel maneja errores internamente
                                return@Button
                            }
                            
                            if (category.isBlank()) {
                                // Mostrar error localmente - el ViewModel maneja errores internamente
                                return@Button
                            }

                            // Obtener el orderIndex más alto actual
                            val currentTemplate = vm.currentTemplate.value
                            val maxOrderIndex = currentTemplate?.sections
                                ?.flatMap { it.items }
                                ?.maxOfOrNull { it.orderIndex } ?: 0

                            if (isEditing && itemId != null) {
                                vm.updateItem(
                                    templateId = templateId,
                                    itemId = itemId,
                                    orderIndex = null,
                                    title = title.takeIf { it.isNotBlank() },
                                    category = category.takeIf { it.isNotBlank() },
                                    subcategory = subcategory.takeIf { it.isNotBlank() },
                                    percentage = percentage.replace("%", "").toDoubleOrNull(), // ✅ AGREGADO: Parsear porcentaje
                                    expectedType = expectedType,
                                    config = null,
                                    onSuccess = { }
                                )
                            } else {
                                vm.createItem(
                                    templateId = templateId,
                                    orderIndex = maxOrderIndex + 1,
                                    title = title,
                                    category = category,
                                    subcategory = subcategory.takeIf { it.isNotBlank() },
                                    percentage = percentage.replace("%", "").toDoubleOrNull(), // ✅ AGREGADO: Enviar porcentaje
                                    expectedType = expectedType,
                                    config = null,
                                    onSuccess = { }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading && title.isNotBlank() && category.isNotBlank()
                    ) {
                        Text(if (isEditing) "Actualizar" else "Crear")
                    }
                }
            }
        }
    }
}