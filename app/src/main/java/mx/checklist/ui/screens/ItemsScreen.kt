package mx.checklist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import java.io.File

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
            Text("Corrida #$runId — Tienda $storeCode", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Items de la Corrida $runId ($storeCode)", style = MaterialTheme.typography.headlineSmall)
        }

        if (isReadOnly) Text("Corrida enviada (solo lectura)", color = MaterialTheme.colorScheme.primary)
        if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

        Text("$answered / ${items.size} respondidos")
        LinearProgressIndicator(progress = answered / total.toFloat(), modifier = Modifier.fillMaxWidth())

        if (loading && items.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    readOnly = isReadOnly,
                    vm = vm, // Pass the ViewModel
                    onSave = { currentStatus, respText, number ->
                        vm.respond(item.id, currentStatus, respText, number)
                    }
                )
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

    val context = LocalContext.current
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val files = uris.mapNotNull { uri ->
                    uriToFile(context, uri)
                }
                if (files.isNotEmpty()) {
                    vm.uploadAttachments(item.id, files)
                }
            }
        }
    )

    // Eliminar LaunchedEffect que llamaba a vm.loadAttachments
    // Considerar dónde/cómo clearEvidenceError debe ser llamado si es necesario por ítem.
    // Por ahora, se mantiene la llamada explícita a vm.clearEvidenceError() antes de onSave y al cambiar de estatus.
    LaunchedEffect(item.id) {
        vm.clearEvidenceError() // Limpiar error de evidencia específico del ítem al cargar o cambiar el ítem
    }

    var status by remember(item.id) { mutableStateOf(item.responseStatus.orEmpty()) }
    var respText by remember(item.id) { mutableStateOf(item.responseText.orEmpty()) }
    var numberStr by remember(item.id) { mutableStateOf(item.responseNumber?.toString().orEmpty()) }
    var justSaved by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.id, item.responseStatus, item.responseText, item.responseNumber) {
        status = item.responseStatus.orEmpty()
        respText = item.responseText.orEmpty()
        numberStr = item.responseNumber?.toString().orEmpty()
    }

    val evidenceConfig = remember(item.itemTemplate?.config) {
        item.itemTemplate?.config?.get("evidence") as? Map<String, Any?>
    }
    // Actualizar photoEvidenceRequiredText para usar attachmentsForThisItem.size
    var photoEvidenceRequiredText by remember(item.id, evidenceConfig, status, attachmentsForThisItem.size) { mutableStateOf<String?>(null) }

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
            }
            else if (minCount > 0 && photosActuallyNeeded == 0) {
                photosActuallyNeeded = minCount
            }

            if (photosActuallyNeeded > 0) {
                // Usar attachmentsForThisItem.size
                photoEvidenceRequiredText = "Fotos: ${attachmentsForThisItem.size} / $photosActuallyNeeded requeridas."
            } else {
                // Usar attachmentsForThisItem.size
                photoEvidenceRequiredText = if (itemExpectedType == "PHOTO" || itemExpectedType == "MULTIPHOTO") "Fotos: ${attachmentsForThisItem.size} (opcional)" else null
            }
        } else {
            photoEvidenceRequiredText = null
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(initialTitle, style = MaterialTheme.typography.titleMedium)
            val meta = listOfNotNull(
                initialCategory.takeIf { it.isNotBlank() },
                initialSubcategory.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)

            photoEvidenceRequiredText?.let {
                val currentMinCount = extractMinCount(evidenceConfig, status, item.itemTemplate?.expectedType)
                // Usar attachmentsForThisItem.size
                val isError = evidenceError != null || (currentMinCount > attachmentsForThisItem.size && currentMinCount > 0)
                Text(it, style = MaterialTheme.typography.bodySmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            evidenceError?.let {
                Text("$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            val shouldShowEvidenceSection = evidenceConfig != null || item.itemTemplate?.expectedType.equals("PHOTO", true) || item.itemTemplate?.expectedType.equals("MULTIPHOTO", true)
            if (shouldShowEvidenceSection) {
                Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Evidencia Fotográfica", style = MaterialTheme.typography.titleSmall)
                    // Usar attachmentsForThisItem
                    attachmentsForThisItem.forEach { att ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("- Foto ID: ${att.id} (${att.url})", style = MaterialTheme.typography.bodySmall)
                            if (!readOnly) {
                                TextButton(onClick = { vm.deleteAttachment(item.id, att.id) }) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                    // Usar attachmentsForThisItem
                    if (attachmentsForThisItem.isEmpty()) {
                        Text("(No hay fotos adjuntas)", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!readOnly) {
                        Button(onClick = {
                            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text("Agregar Foto(s)")
                        }
                    }
                }
            }

            Text("Estatus (requerido)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("OK", "FAIL", "NA").forEach { opt ->
                    FilterChip(
                        selected = status.equals(opt, ignoreCase = true),
                        onClick = {
                            if (!readOnly) {
                                status = opt
                                justSaved = false
                                vm.clearEvidenceError()
                            }
                        },
                        enabled = !readOnly,
                        label = { Text(opt) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            OutlinedTextField(
                value = respText,
                onValueChange = { if (!readOnly) respText = it },
                label = { Text("Texto (opcional)") },
                enabled = !readOnly,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = numberStr,
                onValueChange = { input ->
                    if (!readOnly) numberStr = input.filter { ch: Char -> ch.isDigit() || ch == '.' }
                },
                label = { Text("Número (opcional)") },
                enabled = !readOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (!readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val parsed = numberStr.toDoubleOrNull()
                    Button(
                        enabled = status.isNotBlank() && !loading,
                        onClick = {
                            vm.clearEvidenceError()
                            onSave(status.trim(), respText.trim().ifBlank { null }, parsed)
                        }
                    ) { Text(if (justSaved && evidenceError == null && !loading) "Guardado ✓" else "Guardar respuesta") }
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
        }
        else if (minCount > 0 && photosNeeded == 0) {
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
