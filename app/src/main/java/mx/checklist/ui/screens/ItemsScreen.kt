package mx.checklist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.RunItemDto
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.fields.BarcodeField
import mx.checklist.ui.fields.MultiSelectField
import mx.checklist.ui.fields.ScaleField
import mx.checklist.ui.fields.SingleChoiceField
import mx.checklist.ui.fields.TextFieldLong
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@Composable
fun ItemsScreen(
    runId: Long,
    storeCode: String,
    vm: RunsViewModel,
    onSubmit: () -> Unit,
    readOnly: Boolean = false,
    templateName: String? = null
) {
    // Cargar ítems + info del run (para status y nombre de plantilla)
    LaunchedEffect(runId) {
        vm.loadRunItems(runId)
        vm.loadRunInfo(runId)
    }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val items by vm.runItemsFlow().collectAsStateWithLifecycle()
    val runInfo by vm.runInfoFlow().collectAsStateWithLifecycle()

    val readOnlyAuto = runInfo?.status == "SUBMITTED"
    val isReadOnly = readOnly || readOnlyAuto

    val shownTemplateName = templateName ?: runInfo?.templateName

    // ✅ CORREGIDO: Agrupar items por categoría sin depender de sectionId
    val itemsBySection = remember(items) {
        items
            .groupBy { it.itemTemplate?.category ?: "Sin categoría" }
            .toSortedMap() // Ordenar alfabéticamente por nombre de categoría
    }

    val answered = items.count { !it.responseStatus.isNullOrEmpty() }
    val total = items.size.coerceAtLeast(1)
    val allAnswered = items.isNotEmpty() && answered == items.size

    var showSubmittedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!shownTemplateName.isNullOrBlank()) {
            Text("Checklist: $shownTemplateName", style = MaterialTheme.typography.titleLarge)
            Text("ID #$runId — Tienda $storeCode", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Checklist #$runId ($storeCode)", style = MaterialTheme.typography.headlineSmall)
        }

        if (isReadOnly) Text("Checklist enviado (solo lectura)", color = MaterialTheme.colorScheme.primary)
        if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

        Text("$answered / ${items.size} items respondidos")
        LinearProgressIndicator(progress = { answered / total.toFloat() }, modifier = Modifier.fillMaxWidth())

        if (loading && items.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ✅ CORREGIDO: Iterar correctamente sobre todas las secciones
            itemsBySection.forEach { (sectionName, sectionItems) ->
                // Calcular estadísticas de la sección
                val sectionAnswered = sectionItems.count { !it.responseStatus.isNullOrEmpty() }
                val sectionTotal = sectionItems.size

                // Encabezado de sección
                item(key = "header_$sectionName") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = sectionName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$sectionAnswered de $sectionTotal completados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Items de la sección ordenados por orderIndex
                items(
                    items = sectionItems.sortedBy { it.orderIndex },
                    key = { it.id }
                ) { item ->
                    ItemCard(
                        item = item,
                        readOnly = isReadOnly,
                        vm = vm,
                        onSave = { currentStatus, respText, number ->
                            vm.respond(item.id, currentStatus, respText, number)
                        }
                    )
                }
            }
        }

        if (!isReadOnly) {
            Button(
                enabled = allAnswered && !loading,
                onClick = {
                    val (canSubmit, message) = vm.canSubmitAll()
                    if (canSubmit) {
                        vm.submit(runId) {
                            showSubmittedDialog = true
                        }
                    } else {
                        vm.updateError(message ?: "Hay ítems pendientes o con errores de evidencia.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Enviando…" else "Enviar checklist") }
        }
    }

    if (showSubmittedDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmittedDialog = false
                onSubmit()
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubmittedDialog = false
                    onSubmit()
                }) { Text("OK") }
            },
            title = { Text("Checklist enviado") },
            text = { Text("Tu checklist se envió correctamente.") }
        )
    }
}

@Composable
private fun ItemCard(
    item: RunItemDto,
    readOnly: Boolean,
    vm: RunsViewModel,
    onSave: (status: String?, text: String?, number: Double?) -> Unit
) {
    val initialTitle = remember(item.id) {
        item.itemTemplate?.title?.takeIf { it.isNotBlank() }
            ?: "Ítem #${item.orderIndex} (id ${item.id})"
    }
    val initialCategory = remember(item.id) { item.itemTemplate?.category.orEmpty() }
    val initialSubcategory = remember(item.id) { item.itemTemplate?.subcategory.orEmpty() }

    // Usar los adjuntos directamente del item DTO
    val attachmentsForThisItem = item.attachments ?: emptyList()
    val evidenceError by vm.evidenceError.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val uploadingImages by vm.uploadingImages.collectAsStateWithLifecycle()
    val isUploadingThisItem = uploadingImages.contains(item.id)

    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    uriToFile(context, uri)?.let { file ->
                        vm.uploadAttachment(item.id, file, uri.toString())
                    }
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val newImageFile = createImageFile(context)
            val newImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                newImageFile
            )
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        }
    }

    LaunchedEffect(item.id) {
        vm.clearEvidenceError()
    }

    var status by remember(item.id) { mutableStateOf(item.responseStatus.orEmpty()) }
    var respText by remember(item.id) { mutableStateOf(item.responseText.orEmpty()) }
    var numberStr by remember(item.id) { mutableStateOf(item.responseNumber?.toString().orEmpty()) }
    // ✅ CORREGIDO: Usar responseStatus en lugar de justSaved local
    val isAlreadySaved = !item.responseStatus.isNullOrEmpty()

    LaunchedEffect(item.id, item.responseStatus, item.responseText, item.responseNumber) {
        status = item.responseStatus.orEmpty()
        respText = item.responseText.orEmpty()
        numberStr = item.responseNumber?.toString().orEmpty()
    }

    val evidenceConfig = remember(item.itemTemplate?.config) {
        (item.itemTemplate?.config?.get("evidence") as? Map<*, *>)?.mapKeys { it.key.toString() }
    }

    var photoEvidenceRequiredText by remember(item.id, evidenceConfig, status, attachmentsForThisItem.size) {
        mutableStateOf<String?>(null)
    }
    var photosNeeded by remember { mutableStateOf(0) }

    LaunchedEffect(item.id, evidenceConfig, status, attachmentsForThisItem.size) {
        var required = false
        var minCount = 0
        var requiredOnFail = false
        var isPhotoEvidenceSource = false

        val itemExpectedType = item.itemTemplate?.expectedType?.uppercase()

        if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
            isPhotoEvidenceSource = true
            required = evidenceConfig["required"] as? Boolean ?: false
            minCount = (evidenceConfig["minCount"] as? Number)?.toInt() ?: 0
            requiredOnFail = evidenceConfig["requiredOnFail"] as? Boolean ?: false
        } else if (itemExpectedType == "PHOTO" || itemExpectedType == "MULTIPHOTO"){
            isPhotoEvidenceSource = true
            minCount = (evidenceConfig?.get("minCount") as? Number)?.toInt() ?: 1
            required = evidenceConfig?.get("required") as? Boolean ?: (minCount > 0)
        }

        if (isPhotoEvidenceSource) {
            var photosActuallyNeeded = 0
            if (requiredOnFail && status.equals("FAIL", ignoreCase = true)) {
                photosActuallyNeeded = if (minCount > 0) minCount else 1
            } else if (required) {
                photosActuallyNeeded = minCount
            } else if (minCount > 0 && photosActuallyNeeded == 0) {
                photosActuallyNeeded = minCount
            }

            photosNeeded = photosActuallyNeeded

            if (photosActuallyNeeded > 0) {
                val remaining = (photosActuallyNeeded - attachmentsForThisItem.size).coerceAtLeast(0)
                if (remaining > 0) {
                    photoEvidenceRequiredText = "Faltan $remaining fotos. (${attachmentsForThisItem.size} / $photosActuallyNeeded)"
                } else {
                    photoEvidenceRequiredText = "Fotos: ${attachmentsForThisItem.size} / $photosActuallyNeeded requeridas."
                }
            } else {
                photoEvidenceRequiredText = if (itemExpectedType == "PHOTO" || itemExpectedType == "MULTIPHOTO")
                    "Fotos: ${attachmentsForThisItem.size} (opcional)" else null
            }
        } else {
            photoEvidenceRequiredText = null
            photosNeeded = 0
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ✅ NUEVO: Chip de estado visual prominente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(initialTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                // Chip de estado
                FilterChip(
                    selected = isAlreadySaved,
                    onClick = { /* No hace nada, solo visual */ },
                    enabled = false,
                    label = { 
                        Text(
                            if (isAlreadySaved) "✓ Guardado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50),
                        selectedLabelColor = Color.White,
                        disabledSelectedContainerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        disabledLabelColor = Color.Gray
                    )
                )
            }

            val meta = listOfNotNull(
                initialCategory.takeIf { it.isNotBlank() },
                initialSubcategory.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)

            photoEvidenceRequiredText?.let {
                val currentMinCount = extractMinCount(evidenceConfig, status, item.itemTemplate?.expectedType)
                val photosAreMissing = currentMinCount > attachmentsForThisItem.size
                val isError = evidenceError != null || (photosAreMissing && currentMinCount > 0)
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            evidenceError?.let {
                Text("$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            val shouldShowEvidenceSection = evidenceConfig != null ||
                item.itemTemplate?.expectedType.equals("PHOTO", true) ||
                item.itemTemplate?.expectedType.equals("MULTIPHOTO", true)
            if (shouldShowEvidenceSection) {
                Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Evidencia Fotográfica", style = MaterialTheme.typography.titleSmall)

                    if (attachmentsForThisItem.isEmpty()) {
                        Text("(No hay fotos adjuntas)", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachmentsForThisItem, key = { it.id }) { att ->
                                Box(modifier = Modifier.size(100.dp)) {
                                    val imageModel = att.localUri ?: "http://172.16.16.22:3000${att.url}"
                                    Image(
                                        painter = rememberAsyncImagePainter(imageModel),
                                        contentDescription = "Foto adjunta ${att.id}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (!readOnly) {
                                        IconButton(
                                            onClick = { vm.deleteAttachment(item.id, att.id) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar foto", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!readOnly) {
                        Button(
                            onClick = {
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) -> {
                                        val newImageFile = createImageFile(context)
                                        val newImageUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            newImageFile
                                        )
                                        tempImageUri = newImageUri
                                        cameraLauncher.launch(newImageUri)
                                    }
                                    else -> {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            enabled = !isUploadingThisItem,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUploadingThisItem) {
                                Text("Subiendo imagen...")
                            } else {
                                Text("Tomar Foto")
                            }
                        }

                        if (isUploadingThisItem) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Renderizado específico por tipo de campo
            val expectedType = item.itemTemplate?.expectedType?.uppercase()
            val config = item.itemTemplate?.config ?: emptyMap()

            when (expectedType) {
                "BOOLEAN" -> {
                    // Solo para campos BOOLEAN mostramos los chips SÍ/NO
                    Text("Estatus (requerido)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusOptions = mapOf(
                            "OK" to "SÍ",
                            "FAIL" to "NO"
                        )
                        val statusColors = mapOf(
                            "OK" to Color(0xFF4CAF50),
                            "FAIL" to Color(0xFFF44336)
                        )

                        statusOptions.forEach { (value, label) ->
                            val isSelected = status.equals(value, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!readOnly) {
                                        status = value
                                        vm.clearEvidenceError()
                                    }
                                },
                                enabled = !readOnly,
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = statusColors[value] ?: MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    if (!readOnly) {
                        Text("Campo booleano - selecciona SÍ o NO arriba", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                "SINGLE_CHOICE" -> {
                    val options = (config["options"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    if (options.isNotEmpty() && !readOnly) {
                        Text("Selecciona una opción:")
                        SingleChoiceField(
                            options = options,
                            selected = respText.takeIf { it.isNotBlank() },
                            onSelect = { selectedOption ->
                                respText = selectedOption
                                // Auto-establecer estado a OK cuando se selecciona una opción
                                status = "OK"
                            }
                        )
                    } else if (options.isNotEmpty() && readOnly) {
                        Text("Opción seleccionada: ${respText.ifBlank { "Ninguna" }}")
                    }
                    // Mostrar estado automático para no-BOOLEAN
                    if (!readOnly) {
                        val hasResponse = respText.isNotBlank()
                        Text(
                            text = if (hasResponse) "✓ Respondido" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
                
                "MULTISELECT" -> {
                    val options = (config["options"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    if (options.isNotEmpty() && !readOnly) {
                        Text("Selecciona múltiples opciones:")
                        val selectedOptions = respText.split(",").filter { it.isNotBlank() }
                        MultiSelectField(
                            options = options,
                            selected = selectedOptions,
                            onSelectionChange = { newSelection ->
                                respText = newSelection.joinToString(",")
                                // Auto-establecer estado a OK cuando se seleccionan opciones
                                status = "OK"
                            }
                        )
                    } else if (options.isNotEmpty() && readOnly) {
                        Text("Opciones seleccionadas: ${respText.ifBlank { "Ninguna" }}")
                    }
                    // Mostrar estado automático para no-BOOLEAN
                    if (!readOnly) {
                        val hasResponse = respText.isNotBlank()
                        Text(
                            text = if (hasResponse) "✓ Respondido" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
                
                "SCALE" -> {
                    val min = (config["min"] as? Number)?.toInt() ?: 1
                    val max = (config["max"] as? Number)?.toInt() ?: 10
                    val step = (config["step"] as? Number)?.toInt() ?: 1
                    if (!readOnly) {
                        Text("Calificación (${min} a ${max}):")
                        ScaleField(
                            min = min,
                            max = max,
                            step = step,
                            value = numberStr.toIntOrNull(),
                            onValueChange = { value ->
                                numberStr = value.toString()
                                // Auto-establecer estado a OK cuando se califica
                                status = "OK"
                            }
                        )
                        // Mostrar estado automático
                        val hasResponse = numberStr.isNotBlank() && numberStr.toIntOrNull() != null
                        Text(
                            text = if (hasResponse) "✓ Calificado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Calificación: ${numberStr.ifBlank { "No calificado" }}")
                    }
                }
                
                "NUMBER" -> {
                    if (!readOnly) {
                        OutlinedTextField(
                            value = numberStr,
                            onValueChange = { input ->
                                numberStr = input.filter { ch: Char -> ch.isDigit() || ch == '.' }
                                // Auto-establecer estado a OK cuando se ingresa número válido
                                if (input.isNotBlank() && input.toDoubleOrNull() != null) {
                                    status = "OK"
                                }
                            },
                            label = { Text("Valor numérico") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Mostrar estado automático
                        val hasResponse = numberStr.isNotBlank() && numberStr.toDoubleOrNull() != null
                        Text(
                            text = if (hasResponse) "✓ Valor ingresado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Valor: ${numberStr.ifBlank { "No especificado" }}")
                    }
                }
                
                "TEXT" -> {
                    val maxLength = (config["maxLength"] as? Number)?.toInt() ?: 500
                    if (!readOnly) {
                        TextFieldLong(
                            value = respText,
                            maxLength = maxLength,
                            onValueChange = { newText ->
                                respText = newText
                                // Auto-establecer estado a OK cuando hay texto
                                if (newText.isNotBlank()) {
                                    status = "OK"
                                }
                            }
                        )
                        // Mostrar estado automático
                        val hasResponse = respText.isNotBlank()
                        Text(
                            text = if (hasResponse) "✓ Texto ingresado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Texto: ${respText.ifBlank { "No especificado" }}")
                    }
                }
                
                "BARCODE" -> {
                    if (!readOnly) {
                        BarcodeField(
                            value = item.scannedBarcode,
                            onScan = { scannedCode ->
                                // Actualizar el código escaneado
                            }
                        )
                        // Mostrar estado automático
                        val hasResponse = !item.scannedBarcode.isNullOrBlank()
                        Text(
                            text = if (hasResponse) "✓ Código escaneado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Código escaneado: ${item.scannedBarcode ?: "No escaneado"}")
                    }
                }
                
                "PHOTO", "MULTIPHOTO" -> {
                    // Las fotos ya se manejan en la sección de evidencia arriba
                    if (!readOnly) {
                        // Mostrar estado automático basado en evidencia
                        val hasPhotos = item.attachments?.isNotEmpty() == true
                        Text(
                            text = if (hasPhotos) "✓ Foto(s) tomada(s)" else "⚪ Sin fotos",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasPhotos) Color(0xFF4CAF50) else Color.Gray
                        )
                        if (hasPhotos) {
                            status = "OK" // Auto-establecer cuando hay fotos
                        }
                    }
                }
                
                else -> {
                    // Campos genéricos para tipos no reconocidos - auto-estado basado en respuesta
                    if (!readOnly) {
                        OutlinedTextField(
                            value = respText,
                            onValueChange = { 
                                respText = it
                                // Auto-establecer estado a OK cuando hay contenido
                                if (it.isNotBlank()) {
                                    status = "OK"
                                }
                            },
                            label = { Text("Comentarios adicionales (opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Mostrar estado automático
                        val hasResponse = respText.isNotBlank()
                        Text(
                            text = if (hasResponse) "✓ Completado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else if (respText.isNotBlank()) {
                        Text("Comentarios: $respText")
                    }
                }
            }

            if (!readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val parsed = numberStr.toDoubleOrNull()
                    val isSaveEnabled = status.isNotBlank() && !loading && !isUploadingThisItem &&
                        (attachmentsForThisItem.size >= photosNeeded)
                    val isSaved = isAlreadySaved && evidenceError == null && !loading && !isUploadingThisItem

                    Button(
                        enabled = isSaveEnabled && !isSaved,
                        onClick = {
                            vm.clearEvidenceError()
                            onSave(status.trim(), respText.trim().ifBlank { null }, parsed)
                        },
                        colors = if (isSaved) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        ) else ButtonDefaults.buttonColors()
                    ) {
                        when {
                            isUploadingThisItem -> Text("Subiendo imagen...")
                            isSaved -> Text("Guardado ✓")
                            else -> Text("Guardar respuesta")
                        }
                    }
                }
            }
        }
    }
}

private fun extractMinCount(evidenceConfig: Map<String, Any?>?, currentStatus: String, itemExpectedType: String?): Int {
    var photosNeeded = 0
    var minCount = 0
    var required = false
    var requiredOnFail = false
    var isPhotoEvidenceSource = false

    if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
        isPhotoEvidenceSource = true
        required = evidenceConfig["required"] as? Boolean ?: false
        minCount = (evidenceConfig["minCount"] as? Number)?.toInt() ?: 0
        requiredOnFail = evidenceConfig["requiredOnFail"] as? Boolean ?: false
    } else if (itemExpectedType.equals("PHOTO", true) || itemExpectedType.equals("MULTIPHOTO", true)) {
        isPhotoEvidenceSource = true
        minCount = (evidenceConfig?.get("minCount") as? Number)?.toInt() ?: 1
        required = evidenceConfig?.get("required") as? Boolean ?: (minCount > 0)
    }

    if (isPhotoEvidenceSource) {
        if (requiredOnFail && currentStatus.equals("FAIL", ignoreCase = true)) {
            photosNeeded = if (minCount > 0) minCount else 1
        } else if (required) {
            photosNeeded = minCount
        } else if (minCount > 0 && photosNeeded == 0) {
            photosNeeded = minCount
        }
    }
    return photosNeeded
}

private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val fileName = "upload_${System.currentTimeMillis()}_${uri.lastPathSegment ?: "temp"}"
        val tempFile = File(context.cacheDir, fileName)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.cacheDir
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}