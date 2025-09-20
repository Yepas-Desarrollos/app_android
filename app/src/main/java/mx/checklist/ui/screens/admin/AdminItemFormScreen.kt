package mx.checklist.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.FieldType
import mx.checklist.data.api.dto.FieldTypeDefaults
import mx.checklist.ui.vm.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminItemFormScreen(
    vm: AdminViewModel,
    templateId: Long,
    itemId: Long?, // null = crear nuevo, not null = editar existente
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    // Estados locales del formulario
    var orderIndex by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf("") }
    var selectedFieldType by remember { mutableStateOf(FieldType.BOOLEAN) }
    var config by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    // Estados específicos para configuración de tipos
    var options by remember { mutableStateOf(listOf("")) }
    var minValue by remember { mutableStateOf("") }
    var maxValue by remember { mutableStateOf("") }
    var stepValue by remember { mutableStateOf("") }
    var maxLength by remember { mutableStateOf("") }
    var evidenceRequired by remember { mutableStateOf(false) }
    var minPhotoCount by remember { mutableStateOf("") }

    // Estados del ViewModel
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val currentTemplate by vm.currentTemplate.collectAsStateWithLifecycle()
    val operationSuccess by vm.operationSuccess.collectAsStateWithLifecycle()

    val isEditing = itemId != null
    val title_screen = if (isEditing) "Editar Item" else "Crear Item"

    // Cargar template y item si estamos editando
    LaunchedEffect(templateId, itemId) {
        vm.loadTemplate(templateId)
    }

    // Actualizar campos cuando se carga el item existente
    LaunchedEffect(currentTemplate, itemId) {
        if (isEditing && itemId != null && currentTemplate != null) {
            val item = currentTemplate!!.items.find { it.id == itemId }
            item?.let {
                orderIndex = it.orderIndex.toString()
                title = it.title ?: ""
                category = it.category ?: ""
                subcategory = it.subcategory ?: ""
                selectedFieldType = FieldType.fromValue(it.expectedType ?: "") ?: FieldType.BOOLEAN
                config = it.config ?: emptyMap()
                
                // Cargar configuraciones específicas
                when (selectedFieldType) {
                    FieldType.SINGLE_CHOICE, FieldType.MULTISELECT -> {
                        val configOptions = (config["options"] as? List<*>)?.map { opt -> opt.toString() } ?: listOf("")
                        options = if (configOptions.isEmpty()) listOf("") else configOptions
                    }
                    FieldType.SCALE -> {
                        minValue = (config["min"] as? Number)?.toString() ?: "1"
                        maxValue = (config["max"] as? Number)?.toString() ?: "10"
                        stepValue = (config["step"] as? Number)?.toString() ?: "1"
                    }
                    FieldType.NUMBER -> {
                        minValue = (config["min"] as? Number)?.toString() ?: ""
                        maxValue = (config["max"] as? Number)?.toString() ?: ""
                    }
                    FieldType.TEXT -> {
                        maxLength = (config["maxLength"] as? Number)?.toString() ?: "500"
                    }
                    FieldType.PHOTO -> {
                        val evidenceConfig = config["evidence"] as? Map<*, *>
                        evidenceRequired = evidenceConfig?.get("required") as? Boolean ?: true
                        minPhotoCount = (evidenceConfig?.get("minCount") as? Number)?.toString() ?: "1"
                    }
                    else -> {}
                }
            }
        } else if (!isEditing && currentTemplate != null) {
            // Para nuevo item, sugerir el siguiente orderIndex
            val nextIndex = (currentTemplate!!.items.maxOfOrNull { it.orderIndex } ?: 0) + 1
            orderIndex = nextIndex.toString()
        }
    }

    // Navegar de vuelta cuando se guarda exitosamente
    LaunchedEffect(operationSuccess) {
        if (operationSuccess != null) {
            onSaved()
        }
    }

    // Actualizar configuración cuando cambia el tipo de campo
    LaunchedEffect(selectedFieldType) {
        if (!isEditing) {
            // Solo cargar defaults para items nuevos
            val defaults = FieldTypeDefaults.getDefaultConfig(selectedFieldType)
            when (selectedFieldType) {
                FieldType.SINGLE_CHOICE, FieldType.MULTISELECT -> {
                    val configOptions = (defaults["options"] as? List<*>)?.map { it.toString() } ?: listOf("")
                    options = if (configOptions.isEmpty()) listOf("") else configOptions
                }
                FieldType.SCALE -> {
                    minValue = (defaults["min"] as? Number)?.toString() ?: "1"
                    maxValue = (defaults["max"] as? Number)?.toString() ?: "10"
                    stepValue = (defaults["step"] as? Number)?.toString() ?: "1"
                }
                FieldType.NUMBER -> {
                    minValue = (defaults["min"] as? Number)?.toString() ?: ""
                    maxValue = (defaults["max"] as? Number)?.toString() ?: ""
                }
                FieldType.TEXT -> {
                    maxLength = (defaults["maxLength"] as? Number)?.toString() ?: "500"
                }
                FieldType.PHOTO -> {
                    val evidenceConfig = defaults["evidence"] as? Map<*, *>
                    evidenceRequired = evidenceConfig?.get("required") as? Boolean ?: true
                    minPhotoCount = (evidenceConfig?.get("minCount") as? Number)?.toString() ?: "1"
                }
                else -> {}
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                
                Text(
                    text = title_screen,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
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

        item {
            // Información básica
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Información Básica",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = orderIndex,
                            onValueChange = { orderIndex = it },
                            label = { Text("Orden *") },
                            modifier = Modifier.weight(1f),
                            enabled = !loading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = orderIndex.toIntOrNull() == null
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título del Item *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        isError = title.isBlank()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Categoría") },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        )

                        OutlinedTextField(
                            value = subcategory,
                            onValueChange = { subcategory = it },
                            label = { Text("Subcategoría") },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        )
                    }
                }
            }
        }

        item {
            // Tipo de campo
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Tipo de Campo",
                        style = MaterialTheme.typography.titleMedium
                    )

                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFieldType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Campo *") },
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
                            FieldType.values().forEach { fieldType ->
                                DropdownMenuItem(
                                    text = { Text(fieldType.displayName) },
                                    onClick = {
                                        selectedFieldType = fieldType
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Configuración específica del tipo de campo
        item {
            FieldTypeConfigSection(
                fieldType = selectedFieldType,
                options = options,
                onOptionsChange = { options = it },
                minValue = minValue,
                onMinValueChange = { minValue = it },
                maxValue = maxValue,
                onMaxValueChange = { maxValue = it },
                stepValue = stepValue,
                onStepValueChange = { stepValue = it },
                maxLength = maxLength,
                onMaxLengthChange = { maxLength = it },
                evidenceRequired = evidenceRequired,
                onEvidenceRequiredChange = { evidenceRequired = it },
                minPhotoCount = minPhotoCount,
                onMinPhotoCountChange = { minPhotoCount = it },
                enabled = !loading
            )
        }

        item {
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
                        val finalConfig = buildFieldConfig(
                            fieldType = selectedFieldType,
                            options = options,
                            minValue = minValue,
                            maxValue = maxValue,
                            stepValue = stepValue,
                            maxLength = maxLength,
                            evidenceRequired = evidenceRequired,
                            minPhotoCount = minPhotoCount
                        )
                        
                        if (isEditing && itemId != null) {
                            vm.updateItem(
                                templateId = templateId,
                                itemId = itemId,
                                orderIndex = orderIndex.toIntOrNull(),
                                title = title.takeIf { it.isNotBlank() },
                                category = category.takeIf { it.isNotBlank() },
                                subcategory = subcategory.takeIf { it.isNotBlank() },
                                expectedType = selectedFieldType.value,
                                config = finalConfig.takeIf { it.isNotEmpty() }
                            ) { /* handled by LaunchedEffect */ }
                        } else {
                            vm.createItem(
                                templateId = templateId,
                                orderIndex = orderIndex.toIntOrNull() ?: 1,
                                title = title,
                                category = category.takeIf { it.isNotBlank() },
                                subcategory = subcategory.takeIf { it.isNotBlank() },
                                expectedType = selectedFieldType.value,
                                config = finalConfig.takeIf { it.isNotEmpty() }
                            ) { /* handled by LaunchedEffect */ }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !loading && title.isNotBlank() && orderIndex.toIntOrNull() != null
                ) {
                    Text(if (isEditing) "Actualizar" else "Crear")
                }
            }
        }
    }
}

@Composable
private fun FieldTypeConfigSection(
    fieldType: FieldType,
    options: List<String>,
    onOptionsChange: (List<String>) -> Unit,
    minValue: String,
    onMinValueChange: (String) -> Unit,
    maxValue: String,
    onMaxValueChange: (String) -> Unit,
    stepValue: String,
    onStepValueChange: (String) -> Unit,
    maxLength: String,
    onMaxLengthChange: (String) -> Unit,
    evidenceRequired: Boolean,
    onEvidenceRequiredChange: (Boolean) -> Unit,
    minPhotoCount: String,
    onMinPhotoCountChange: (String) -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configuración de ${fieldType.displayName}",
                style = MaterialTheme.typography.titleMedium
            )

            when (fieldType) {
                FieldType.BOOLEAN -> {
                    Text(
                        text = "Este tipo de campo no requiere configuración adicional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FieldType.SINGLE_CHOICE, FieldType.MULTISELECT -> {
                    Text("Opciones disponibles:")
                    
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = option,
                                onValueChange = { newValue ->
                                    val newOptions = options.toMutableList()
                                    newOptions[index] = newValue
                                    onOptionsChange(newOptions)
                                },
                                label = { Text("Opción ${index + 1}") },
                                modifier = Modifier.weight(1f),
                                enabled = enabled
                            )
                            
                            if (options.size > 1) {
                                IconButton(
                                    onClick = {
                                        val newOptions = options.toMutableList()
                                        newOptions.removeAt(index)
                                        onOptionsChange(newOptions)
                                    },
                                    enabled = enabled
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar opción",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            onOptionsChange(options + "")
                        },
                        enabled = enabled
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar Opción")
                    }
                }

                FieldType.SCALE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minValue,
                            onValueChange = onMinValueChange,
                            label = { Text("Mínimo") },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        OutlinedTextField(
                            value = maxValue,
                            onValueChange = onMaxValueChange,
                            label = { Text("Máximo") },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        OutlinedTextField(
                            value = stepValue,
                            onValueChange = onStepValueChange,
                            label = { Text("Paso") },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                FieldType.NUMBER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minValue,
                            onValueChange = onMinValueChange,
                            label = { Text("Mínimo (opcional)") },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        OutlinedTextField(
                            value = maxValue,
                            onValueChange = onMaxValueChange,
                            label = { Text("Máximo (opcional)") },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                FieldType.TEXT -> {
                    OutlinedTextField(
                        value = maxLength,
                        onValueChange = onMaxLengthChange,
                        label = { Text("Máximo de caracteres") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                FieldType.PHOTO -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = evidenceRequired,
                            onCheckedChange = onEvidenceRequiredChange,
                            enabled = enabled
                        )
                        Text("Evidencia requerida")
                    }
                    
                    OutlinedTextField(
                        value = minPhotoCount,
                        onValueChange = onMinPhotoCountChange,
                        label = { Text("Mínimo de fotos") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                FieldType.BARCODE -> {
                    Text(
                        text = "Configurado para códigos CODE_128 por defecto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Función externa para cargar configuración específica del tipo de campo
private fun loadFieldSpecificConfig(
    fieldType: FieldType, 
    itemConfig: Map<String, Any?>,
    setOptions: (List<String>) -> Unit,
    setMinValue: (String) -> Unit,
    setMaxValue: (String) -> Unit,
    setStepValue: (String) -> Unit,
    setMaxLength: (String) -> Unit,
    setEvidenceRequired: (Boolean) -> Unit,
    setMinPhotoCount: (String) -> Unit
) {
    when (fieldType) {
        FieldType.SINGLE_CHOICE, FieldType.MULTISELECT -> {
            val configOptions = (itemConfig["options"] as? List<*>)?.map { it.toString() } ?: listOf("")
            setOptions(if (configOptions.isEmpty()) listOf("") else configOptions)
        }
        FieldType.SCALE -> {
            setMinValue((itemConfig["min"] as? Number)?.toString() ?: "1")
            setMaxValue((itemConfig["max"] as? Number)?.toString() ?: "10")
            setStepValue((itemConfig["step"] as? Number)?.toString() ?: "1")
        }
        FieldType.NUMBER -> {
            setMinValue((itemConfig["min"] as? Number)?.toString() ?: "")
            setMaxValue((itemConfig["max"] as? Number)?.toString() ?: "")
        }
        FieldType.TEXT -> {
            setMaxLength((itemConfig["maxLength"] as? Number)?.toString() ?: "500")
        }
        FieldType.PHOTO -> {
            val evidenceConfig = itemConfig["evidence"] as? Map<*, *>
            setEvidenceRequired(evidenceConfig?.get("required") as? Boolean ?: true)
            setMinPhotoCount((evidenceConfig?.get("minCount") as? Number)?.toString() ?: "1")
        }
        else -> {}
    }
}

// Función para construir configuración según el tipo de campo
private fun buildFieldConfig(
    fieldType: FieldType,
    options: List<String>,
    minValue: String,
    maxValue: String,
    stepValue: String,
    maxLength: String,
    evidenceRequired: Boolean,
    minPhotoCount: String
): Map<String, Any?> {
    return when (fieldType) {
        FieldType.BOOLEAN -> emptyMap()
        
        FieldType.SINGLE_CHOICE, FieldType.MULTISELECT -> {
            val filteredOptions = options.filter { it.isNotBlank() }
            if (filteredOptions.isNotEmpty()) {
                mapOf("options" to filteredOptions)
            } else emptyMap()
        }
        
        FieldType.SCALE -> {
            mapOf(
                "min" to (minValue.toIntOrNull() ?: 1),
                "max" to (maxValue.toIntOrNull() ?: 10),
                "step" to (stepValue.toIntOrNull() ?: 1)
            )
        }
        
        FieldType.NUMBER -> {
            val numberConfig = mutableMapOf<String, Any?>()
            minValue.toDoubleOrNull()?.let { numberConfig["min"] = it }
            maxValue.toDoubleOrNull()?.let { numberConfig["max"] = it }
            numberConfig
        }
        
        FieldType.TEXT -> {
            mapOf("maxLength" to (maxLength.toIntOrNull() ?: 500))
        }
        
        FieldType.PHOTO -> {
            mapOf(
                "evidence" to mapOf(
                    "type" to "PHOTO",
                    "required" to evidenceRequired,
                    "minCount" to (minPhotoCount.toIntOrNull() ?: 1)
                )
            )
        }
        
        FieldType.BARCODE -> {
            mapOf("format" to "CODE_128")
        }
    }
}