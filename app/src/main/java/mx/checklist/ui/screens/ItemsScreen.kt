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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// NUEVOS imports para Snackbar/Scaffold
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

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

    //  CORREGIDO: Agrupar items por categoría sin depender de sectionId
    val itemsBySection = remember(items) {
        items
            .groupBy { it.itemTemplate?.category ?: "Sin categoría" }
            .toSortedMap() // Ordenar alfabéticamente por nombre de categoría
    }

    val answered = items.count { !it.responseStatus.isNullOrEmpty() }
    val total = items.size.coerceAtLeast(1)
    val allAnswered = items.isNotEmpty() && answered == items.size

    //  CORRECCIÓN: Validar también que se cumplan los requisitos de fotos
    val allRequirementsMet = remember(items) {
        if (items.isEmpty()) return@remember false

        items.all { item ->
            // Verificar que el item esté respondido
            if (item.responseStatus.isNullOrEmpty()) return@all false

            // Verificar requisitos de fotos
            val tpl = item.itemTemplate
            val cfg = tpl?.config ?: emptyMap<String, Any>()
            val attachments = item.attachments ?: emptyList()
            val evidenceConfig = cfg["evidence"] as? Map<String, Any?>

            if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
                val required = (evidenceConfig["required"] as? Boolean) ?: false
                val minCount = (evidenceConfig["minCount"] as? Number)?.toInt() ?: 0
                val requiredOnFail = (evidenceConfig["requiredOnFail"] as? Boolean) ?: false

                var photosNeeded = 0
                if (requiredOnFail && item.responseStatus.equals("FAIL", ignoreCase = true)) {
                    photosNeeded = if (minCount > 0) minCount else 1
                } else if (required) {
                    photosNeeded = minCount
                }

                if (attachments.size < photosNeeded) return@all false
            } else if (tpl?.expectedType.equals("PHOTO", ignoreCase = true) ||
                       tpl?.expectedType.equals("MULTIPHOTO", ignoreCase = true)) {
                if (attachments.isEmpty()) return@all false
            }

            true
        }
    }

    var showSubmittedDialog by remember { mutableStateOf(false) }

    // Snackbar para mensajes (incluye folio)
    val snackbarHostState = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Encabezado mejorado con diseño más atractivo
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = shownTemplateName ?: "Checklist",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // Color azul como "Checklist Disponibles"
                    )
                    Text(
                        text = "Tienda: $storeCode",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Run ID: #$runId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$answered / ${items.size} items respondidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isReadOnly) Text("Checklist enviado (solo lectura)", color = MaterialTheme.colorScheme.primary)
            if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

            LinearProgressIndicator(progress = { answered / total.toFloat() }, modifier = Modifier.fillMaxWidth())

            if (loading && items.isEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                //  CORREGIDO: Iterar correctamente sobre todas las secciones
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
                    enabled = allAnswered && allRequirementsMet && !loading,
                    onClick = {
                        val (canSubmit, message) = vm.canSubmitAll()
                        if (canSubmit) {
                            vm.submit(runId) {
                                showSubmittedDialog = true
                                // Mostrar snackbar con folio
                                screenScope.launch {
                                    snackbarHostState.showSnackbar("Checklist enviado. Folio: #$runId")
                                }
                            }
                        } else {
                            vm.updateError(message ?: "Hay ítems pendientes o con errores de evidencia.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text(if (loading) "Enviando…" else "Enviar checklist") }
            }
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
            text = {
                Text("Tu checklist se envió correctamente.\n\nCon numero de Folio: #$runId")
            }
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

    // NUEVO: consumir borradores del VM
    val drafts by vm.drafts.collectAsStateWithLifecycle()
    val draftForItem = drafts[item.id]

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

    //  BUENA PRÁCTICA: Estado local inicial desde BORRADOR (si existe) o servidor
    val initialStatus = remember(item.id, draftForItem?.status, item.responseStatus) { draftForItem?.status ?: item.responseStatus ?: "" }
    val initialText = remember(item.id, draftForItem?.text, item.responseText) { draftForItem?.text ?: item.responseText ?: "" }
    val initialNumberStr = remember(item.id, draftForItem?.number, item.responseNumber) { (draftForItem?.number ?: item.responseNumber)?.toString() ?: "" }

    var localStatus by remember(item.id) { mutableStateOf(initialStatus) }
    var localRespText by remember(item.id) { mutableStateOf(initialText) }
    var localNumberStr by remember(item.id) { mutableStateOf(initialNumberStr) }

    //  NUEVO: guardar borrador en cada cambio local
    LaunchedEffect(localStatus, localRespText, localNumberStr) {
        vm.setDraft(item.id, localStatus.ifBlank { null }, localRespText.ifBlank { null }, localNumberStr.toDoubleOrNull())
    }

    //  NUEVO: Job de guardado para cancelar si es necesario
    var saveJob by remember(item.id) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // Estado derivado: si ya tiene respuesta del servidor
    val isServerSaved = !item.responseStatus.isNullOrEmpty() &&
                        item.responseStatus == localStatus

    val evidenceConfig = remember(item.itemTemplate?.config) {
        (item.itemTemplate?.config?.get("evidence") as? Map<*, *>)?.mapKeys { it.key.toString() }
    }

    //  BUENA PRÁCTICA: Calcular requisitos de fotos con useMemo (remember)
    val photoRequirements = remember(item.id, evidenceConfig, localStatus, attachmentsForThisItem.size) {
        calculatePhotoRequirements(
            evidenceConfig = evidenceConfig,
            expectedType = item.itemTemplate?.expectedType,
            currentStatus = localStatus,
            currentPhotoCount = attachmentsForThisItem.size
        )
    }

    //  CRÍTICO: Función de guardado INMEDIATO con validación del backend
    val saveResponse: () -> Unit = save@ {
        if (localStatus.isNotBlank() && !loading && !isUploadingThisItem) {
            // Evitar POST si ya coincide con servidor
            if (isServerSaved) return@save
            val canSaveToServer = if (photoRequirements.required > 0) {
                attachmentsForThisItem.size >= photoRequirements.required
            } else {
                true
            }

            if (canSaveToServer) {
                // Cancelar guardado anterior si existe
                saveJob?.cancel()

                // Guardar inmediatamente sin delay
                saveJob = scope.launch {
                    val parsed = localNumberStr.toDoubleOrNull()
                    vm.clearEvidenceError()
                    onSave(localStatus.trim(), localRespText.trim().ifBlank { null }, parsed)
                }
            }
        }
    }

    // Guardar cuando cambia el estado (con pequeño debounce)
    LaunchedEffect(localStatus) {
        if (localStatus.isNotBlank() && !isServerSaved) {
            delay(100)
            saveResponse()
        }
    }

    // Guardar al desmontar si es posible
    DisposableEffect(item.id) {
        onDispose {
            if (localStatus.isNotBlank() && !loading && !isUploadingThisItem && !isServerSaved) {
                val canSave = if (photoRequirements.required > 0) {
                    attachmentsForThisItem.size >= photoRequirements.required
                } else {
                    true
                }
                if (canSave) {
                    val parsed = localNumberStr.toDoubleOrNull()
                    scope.launch {
                        vm.clearEvidenceError()
                        onSave(localStatus.trim(), localRespText.trim().ifBlank { null }, parsed)
                    }
                }
            }
        }
    }

    // Guardar automáticamente cuando se sube una foto
    LaunchedEffect(attachmentsForThisItem.size) {
        if (localStatus.isNotBlank() && !isServerSaved && attachmentsForThisItem.size >= photoRequirements.required) {
            delay(200)
            saveResponse()
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Encabezado con título y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(initialTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                // Chip de estado mejorado
                val needsPhotoToSave = localStatus.isNotBlank() &&
                                      photoRequirements.required > 0 &&
                                      attachmentsForThisItem.size < photoRequirements.required

                FilterChip(
                    selected = isServerSaved,
                    onClick = { /* Solo visual */ },
                    enabled = false,
                    label = { 
                        Text(
                            when {
                                needsPhotoToSave -> "📷 Falta foto"
                                isServerSaved -> "✓ Guardado"
                                localStatus.isNotBlank() && !isServerSaved -> "⏳ Guardando..."
                                else -> "⚪ Pendiente"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50),
                        selectedLabelColor = Color.White,
                        disabledSelectedContainerColor = Color(0xFF4CAF50),
                        disabledContainerColor = when {
                            needsPhotoToSave -> Color(0xFFFF9800) // Naranja para "falta foto"
                            localStatus.isNotBlank() && !isServerSaved -> Color(0xFFFFA726) // Naranja claro para "guardando"
                            else -> Color.Gray.copy(alpha = 0.3f)
                        },
                        disabledLabelColor = if (localStatus.isNotBlank() || needsPhotoToSave) Color.White else Color.Gray
                    )
                )
            }

            val meta = listOfNotNull(
                initialCategory.takeIf { it.isNotBlank() },
                initialSubcategory.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall)
            }

            // Mostrar info de fotos
            if (photoRequirements.message != null) {
                val photosAreMissing = photoRequirements.required > attachmentsForThisItem.size
                Text(
                    photoRequirements.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (photosAreMissing && photoRequirements.required > 0)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            evidenceError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
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
                                    val imageModel = remember(att.id, att.localUri, att.url) {
                                        ImageRequest.Builder(context)
                                            .data(att.localUri ?: att.url)
                                            .crossfade(300)
                                            .diskCacheKey(att.url)
                                            .memoryCacheKey(att.url)
                                            .build()
                                    }

                                    val painter = rememberAsyncImagePainter(imageModel)
                                    val painterState = painter.state

                                    Image(
                                        painter = painter,
                                        contentDescription = "Foto adjunta ${att.id}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (painterState is AsyncImagePainter.State.Loading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White
                                            )
                                        }
                                    }

                                    if (!readOnly) {
                                        IconButton(
                                            onClick = {
                                                vm.deleteAttachment(item.id, att.id)
                                                // NO marcar como dirty al eliminar foto
                                            },
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
                        val canTakeMorePhotos = attachmentsForThisItem.size < photoRequirements.max

                        Button(
                            onClick = {
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
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
                            enabled = !isUploadingThisItem && canTakeMorePhotos,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when {
                                isUploadingThisItem -> Text("Subiendo imagen...")
                                !canTakeMorePhotos -> Text("Máximo de fotos alcanzado (${photoRequirements.max})")
                                else -> Text("Tomar Foto")
                            }
                        }

                        if (isUploadingThisItem) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Renderizado específico por tipo de campo
            val expectedType = item.itemTemplate?.expectedType?.uppercase()
            val config = item.itemTemplate?.config ?: emptyMap()

            when (expectedType) {
                "BOOLEAN" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusOptions = mapOf("OK" to "SÍ", "FAIL" to "NO")
                        val statusColors = mapOf("OK" to Color(0xFF4CAF50), "FAIL" to Color(0xFFF44336))

                        statusOptions.forEach { (value, label) ->
                            val isSelected = localStatus.equals(value, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!readOnly) {
                                        localStatus = value
                                        vm.clearEvidenceError()
                                        // Guardar borrador inmediatamente
                                        vm.setDraft(item.id, localStatus, localRespText.ifBlank { null }, localNumberStr.toDoubleOrNull())
                                        // El guardado al servidor se dispara por LaunchedEffect/localStatus
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
                }
                
                "SINGLE_CHOICE" -> {
                    val options = (config["options"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    if (options.isNotEmpty() && !readOnly) {
                        Text("Selecciona una opción:")
                        SingleChoiceField(
                            options = options,
                            selected = localRespText.takeIf { it.isNotBlank() },
                            onSelect = { selectedOption ->
                                localRespText = selectedOption
                                localStatus = "OK"
                                vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                                // El guardado se dispara automáticamente
                            }
                        )
                    } else if (options.isNotEmpty() && readOnly) {
                        Text("Opción seleccionada: ${localRespText.ifBlank { "Ninguna" }}")
                    }
                    if (!readOnly) {
                        val hasResponse = localRespText.isNotBlank()
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
                        val selectedOptions = localRespText.split(",").filter { it.isNotBlank() }
                        MultiSelectField(
                            options = options,
                            selected = selectedOptions,
                            onSelectionChange = { newSelection ->
                                localRespText = newSelection.joinToString(",")
                                localStatus = "OK"
                                vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                                // El guardado se dispara automáticamente
                            }
                        )
                    } else if (options.isNotEmpty() && readOnly) {
                        Text("Opciones seleccionadas: ${localRespText.ifBlank { "Ninguna" }}")
                    }
                    if (!readOnly) {
                        val hasResponse = localRespText.isNotBlank()
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
                            value = localNumberStr.toIntOrNull(),
                            onValueChange = { value ->
                                localNumberStr = value.toString()
                                localStatus = "OK"
                                vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                                // El guardado se dispara automáticamente
                            }
                        )
                        val hasResponse = localNumberStr.isNotBlank() && localNumberStr.toIntOrNull() != null
                        Text(
                            text = if (hasResponse) "✓ Calificado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Calificación: ${localNumberStr.ifBlank { "No calificado" }}")
                    }
                }
                
                "NUMBER" -> {
                    if (!readOnly) {
                        OutlinedTextField(
                            value = localNumberStr,
                            onValueChange = { input ->
                                localNumberStr = input.filter { ch: Char -> ch.isDigit() || ch == '.' }
                                if (input.isNotBlank() && input.toDoubleOrNull() != null) {
                                    localStatus = "OK"
                                    vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                                    // El guardado se dispara automáticamente
                                }
                            },
                            label = { Text("Valor numérico") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val hasResponse = localNumberStr.isNotBlank() && localNumberStr.toDoubleOrNull() != null
                        Text(
                            text = if (hasResponse) "✓ Valor ingresado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Valor: ${localNumberStr.ifBlank { "No especificado" }}")
                    }
                }
                
                "TEXT" -> {
                    val maxLength = (config["maxLength"] as? Number)?.toInt() ?: 500
                    if (!readOnly) {
                        TextFieldLong(
                            value = localRespText,
                            maxLength = maxLength,
                            onValueChange = { newText ->
                                localRespText = newText
                                if (newText.isNotBlank()) {
                                    localStatus = "OK"
                                    vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                                    // El guardado se dispara automáticamente
                                }
                            }
                        )
                        val hasResponse = localRespText.isNotBlank()
                        Text(
                            text = if (hasResponse) "✓ Texto ingresado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else {
                        Text("Texto: ${localRespText.ifBlank { "No especificado" }}")
                    }
                }
                
                "BARCODE" -> {
                    if (!readOnly) {
                        BarcodeField(
                            value = item.scannedBarcode,
                            onScan = { scannedCode ->
                                // El guardado se dispara automáticamente
                            }
                        )
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
                    if (!readOnly) {
                        val hasPhotos = item.attachments?.isNotEmpty() == true
                        Text(
                            text = if (hasPhotos) "✓ Foto(s) tomada(s)" else "⚪ Sin fotos",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasPhotos) Color(0xFF4CAF50) else Color.Gray
                        )
                        if (hasPhotos && localStatus.isBlank()) {
                            localStatus = "OK"
                            vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                            // El guardado se dispara automáticamente
                        }
                    }
                }
                
                else -> {
                    if (!readOnly) {
                        OutlinedTextField(
                            value = localRespText,
                            onValueChange = {
                                localRespText = it
                                if (it.isNotBlank()) {
                                    localStatus = "OK"
                                    vm.setDraft(item.id, localStatus, localRespText, localNumberStr.toDoubleOrNull())
                                    // El guardado se dispara automáticamente
                                }
                            },
                            label = { Text("Comentarios adicionales (opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val hasResponse = localRespText.isNotBlank()
                        Text(
                            text = if (hasResponse) "✓ Completado" else "⚪ Pendiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResponse) Color(0xFF4CAF50) else Color.Gray
                        )
                    } else if (localRespText.isNotBlank()) {
                        Text("Comentarios: $localRespText")
                    }
                }
            }
        }
    }
}

// ✅ BUENA PRÁCTICA: Data class para requisitos de fotos (inmutable y claro)
private data class PhotoRequirements(
    val required: Int,
    val max: Int,
    val message: String?
)

// ✅ BUENA PRÁCTICA: Función pura para calcular requisitos (sin efectos secundarios)
private fun calculatePhotoRequirements(
    evidenceConfig: Map<String, Any?>?,
    expectedType: String?,
    currentStatus: String,
    currentPhotoCount: Int
): PhotoRequirements {
    var required = 0
    var minCount = 0
    var maxCount = Int.MAX_VALUE
    var requiredOnFail = false
    var isPhotoEvidenceSource = false

    val itemExpectedType = expectedType?.uppercase()

    if (evidenceConfig != null && evidenceConfig["type"] == "PHOTO") {
        isPhotoEvidenceSource = true
        val isRequired = evidenceConfig["required"] as? Boolean ?: false
        minCount = (evidenceConfig["minCount"] as? Number)?.toInt() ?: 0
        maxCount = (evidenceConfig["maxCount"] as? Number)?.toInt() ?: Int.MAX_VALUE
        requiredOnFail = evidenceConfig["requiredOnFail"] as? Boolean ?: false

        if (requiredOnFail && currentStatus.equals("FAIL", ignoreCase = true)) {
            required = if (minCount > 0) minCount else 1
        } else if (isRequired) {
            required = minCount
        }
    } else if (itemExpectedType == "PHOTO" || itemExpectedType == "MULTIPHOTO") {
        isPhotoEvidenceSource = true
        minCount = (evidenceConfig?.get("minCount") as? Number)?.toInt() ?: 1
        maxCount = (evidenceConfig?.get("maxCount") as? Number)?.toInt() ?:
                   if (itemExpectedType == "PHOTO") 1 else Int.MAX_VALUE
        required = minCount
    }

    val message = if (isPhotoEvidenceSource && required > 0) {
        val remaining = (required - currentPhotoCount).coerceAtLeast(0)
        if (remaining > 0) {
            "Faltan $remaining fotos. ($currentPhotoCount / $required)"
        } else {
            "Fotos: $currentPhotoCount / $required ✓"
        }
    } else if (isPhotoEvidenceSource) {
        "Fotos: $currentPhotoCount (opcional)"
    } else {
        null
    }

    return PhotoRequirements(
        required = required,
        max = maxCount,
        message = message
    )
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
