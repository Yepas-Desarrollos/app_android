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
    sectionId: Long,
    itemId: Long? = null,
    initialTitle: String = "",
    initialExpectedType: String = "TEXT", // Cambiado de "BOOLEAN" a "TEXT"
    initialCategory: String = "",
    initialSubcategory: String = "",
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    // Estados locales del formulario - usando campos que el backend realmente maneja
    var title by remember { mutableStateOf(initialTitle) }
    var expectedType by remember { mutableStateOf(initialExpectedType) }
    var category by remember { mutableStateOf(initialCategory) }
    var subcategory by remember { mutableStateOf(initialSubcategory) }
    var expanded by remember { mutableStateOf(false) }

    // Estados del ViewModel
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val success by vm.operationSuccess.collectAsStateWithLifecycle()
    val currentTemplate by vm.currentTemplate.collectAsStateWithLifecycle()

    val isEditing = itemId != null
    val screenTitle = if (isEditing) "Editar Item" else "Crear Item"

    // Tipos de campo disponibles según el backend
    val fieldTypes = listOf(
        "TEXT" to "Texto",
        "NUMBER" to "Número", 
        "SINGLE_CHOICE" to "Opción única", // Cambiado de "BOOLEAN"
        "MULTIPLE_CHOICE" to "Opción múltiple",
        "PHOTO" to "Foto"
    )

    // Navegar de vuelta cuando se guarda exitosamente
    LaunchedEffect(success) {
        val currentSuccess = success
        if (currentSuccess != null && !currentSuccess.contains("error", ignoreCase = true)) {
            // Solo navegar si realmente fue exitoso
            kotlinx.coroutines.delay(1000) // Reducido de 1500ms a 1000ms
            onSaved()
        }
    }

    // Limpiar error si hay éxito
    LaunchedEffect(success) {
        val currentSuccess = success
        if (currentSuccess != null) {
            kotlinx.coroutines.delay(2000) // Limpiar después de 2 segundos
            vm.clearSuccess()
        }
    }

    // Cargar datos del item actual si estamos editando
    LaunchedEffect(itemId, templateId) {
        if (itemId != null && templateId != null) {
            println("[AdminItemFormScreen] Iniciando carga de item para edición:")
            println("  - itemId: $itemId")
            println("  - templateId: $templateId")

            // Asegurar que el template esté cargado primero
            vm.loadTemplate(templateId)

            // Esperar un poco para que se cargue el template
            kotlinx.coroutines.delay(500)

            val template = vm.currentTemplate.value
            if (template != null) {
                // Buscar el item en todas las secciones del template
                val item = template.sections
                    .flatMap { it.items }
                    .find { it.id == itemId }

                if (item != null) {
                    title = item.title ?: ""
                    expectedType = item.expectedType ?: "TEXT"
                    category = item.category ?: ""
                    subcategory = item.subcategory ?: ""

                    println("[AdminItemFormScreen] ✅ Item cargado para edición:")
                    println("  - title: '${item.title}'")
                    println("  - expectedType: '${item.expectedType}'")
                    println("  - category: '${item.category}'")
                    println("  - subcategory: '${item.subcategory}'")
                } else {
                    println("[AdminItemFormScreen] ⚠️ Item no encontrado en template")
                }
            } else {
                println("[AdminItemFormScreen] ⚠️ Template no cargado después del delay")
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
                    text = "✓ $successMsg",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Formulario del item
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Información del Item",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Item *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    isError = title.isBlank()
                )

                // Dropdown para tipo de campo
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = fieldTypes.find { it.first == expectedType }?.second ?: expectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Campo *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        enabled = !loading
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        fieldTypes.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    expectedType = value
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Campos adicionales para categorías y subcategorías
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                )

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategoría") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
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
                            vm.clearSuccess()
                            if (isEditing && itemId != null) {
                                vm.updateSectionItem(
                                    itemId = itemId,
                                    title = title,
                                    expectedType = expectedType,
                                    category = category,
                                    subcategory = subcategory
                                ) { /* onSuccess handled by LaunchedEffect */ }
                            } else {
                                vm.createSectionItem(
                                    sectionId = sectionId,
                                    title = title,
                                    expectedType = expectedType,
                                    category = category,
                                    subcategory = subcategory
                                ) { /* onSuccess handled by LaunchedEffect */ }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading && title.isNotBlank()
                    ) {
                        Text(if (isEditing) "Actualizar" else "Crear")
                    }
                }
            }
        }
    }
}