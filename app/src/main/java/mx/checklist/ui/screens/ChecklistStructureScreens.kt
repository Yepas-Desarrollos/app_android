@file:OptIn(ExperimentalMaterial3Api::class)

package mx.checklist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.checklist.data.api.dto.FieldType
import mx.checklist.data.api.dto.ItemPercentage
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.data.api.dto.SectionPercentage
import mx.checklist.data.api.dto.SectionTemplateDto
import mx.checklist.data.auth.AuthState
import mx.checklist.ui.vm.ChecklistStructureUiState
import mx.checklist.ui.vm.ChecklistStructureViewModel
import mx.checklist.ui.vm.ValidationState
import java.util.Locale
import kotlin.math.abs

private data class SectionDialogState(
    val checklistId: Long,
    val section: SectionTemplateDto?
)

private data class ItemDialogState(
    val sectionId: Long,
    val item: ItemTemplateDto?
)

private fun Double.toPercentageString(): String =
    if (this.rem(1.0) == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }

@Composable
fun ChecklistStructureScreen(
    checklistId: Long,
    viewModel: ChecklistStructureViewModel,
    navigateBack: () -> Unit,
    onOpenSectionItems: (Long) -> Unit = {}
) {
    LaunchedEffect(checklistId) { viewModel.loadChecklistStructure(checklistId) }

    val uiState by viewModel.uiState.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Detectar si el usuario es administrador
    val isAdmin = remember {
        mx.checklist.data.auth.AuthState.roleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")
    }

    LaunchedEffect(error) {
        error?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    var sectionDialogState by remember { mutableStateOf<SectionDialogState?>(null) }
    var sectionToDelete by remember { mutableStateOf<SectionTemplateDto?>(null) }

    val title = when (val state = uiState) {
        is ChecklistStructureUiState.Success -> state.checklistName
        ChecklistStructureUiState.Loading -> "Cargando checklist..."
        is ChecklistStructureUiState.Error -> "Error"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            ChecklistStructureUiState.Loading -> LoadingState(padding)
            is ChecklistStructureUiState.Error -> ErrorState(
                padding = padding,
                message = state.message,
                onRetry = { viewModel.loadChecklistStructure(checklistId) }
            )
            is ChecklistStructureUiState.Success -> {
                if (isAdmin) {
                    // Vista de ADMINISTRADOR: con opciones de edición
                    ChecklistStructureSuccessContent(
                        padding = padding,
                        state = state,
                        validationState = validationState,
                        loading = loading,
                        onAddSection = { sectionDialogState = SectionDialogState(state.checklistId, null) },
                        onDistributeSections = { viewModel.distributeSectionPercentages(state.checklistId) },
                        onPersistPercentages = {
                            val payload = state.sections.mapNotNull { section ->
                                section.id?.let { SectionPercentage(it, section.percentage ?: 0.0) }
                            }
                            if (payload.isNotEmpty()) {
                                viewModel.updateSectionPercentages(state.checklistId, payload)
                            }
                        },
                        onEditSection = { section -> sectionDialogState = SectionDialogState(state.checklistId, section) },
                        onDeleteSection = { section -> sectionToDelete = section },
                        onReorderSection = { from, to -> viewModel.reorderSections(state.checklistId, from, to) },
                        onOpenItems = onOpenSectionItems,
                        onPercentageChange = { sectionId, value -> viewModel.editSectionPercentageLocally(sectionId, value) }
                    )
                } else {
                    // Vista de USUARIO NORMAL (AUDITOR/SUPERVISOR): solo lectura
                    ChecklistStructureReadOnlyContent(
                        padding = padding,
                        state = state,
                        onOpenSection = onOpenSectionItems
                    )
                }
            }
        }
    }

    // Dialogs solo para administradores
    if (isAdmin) {
        sectionDialogState?.let { dialogState ->
            SectionDialog(
                isEditing = dialogState.section != null,
                initialName = dialogState.section?.name.orEmpty(),
                initialPercentage = dialogState.section?.percentage ?: 0.0,
                onDismiss = { sectionDialogState = null },
                onConfirm = { name, percentage ->
                    if (dialogState.section == null) {
                        viewModel.createSection(dialogState.checklistId, name, percentage)
                    } else {
                        dialogState.section.id?.let { viewModel.updateSection(it, name, percentage) }
                    }
                    sectionDialogState = null
                }
            )
        }

        sectionToDelete?.let { section ->
            AlertDialog(
                onDismissRequest = { sectionToDelete = null },
                title = { Text("Eliminar sección") },
                text = { Text("¿Eliminar la sección '${section.name}' y todos sus items?") },
                confirmButton = {
                    Button(
                        onClick = {
                            section.id?.let { viewModel.deleteSection(it) }
                            sectionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sectionToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// NUEVA: Vista de solo lectura para usuarios normales (AUDITOR/SUPERVISOR)
@Composable
private fun ChecklistStructureReadOnlyContent(
    padding: PaddingValues,
    state: ChecklistStructureUiState.Success,
    onOpenSection: (Long) -> Unit
) {
    val sections = remember(state.sections) { state.sections.sortedBy { it.orderIndex } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Secciones del Checklist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (sections.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Este checklist no tiene secciones configuradas",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Contacta al administrador para configurar el checklist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Selecciona una sección para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(sections, key = { _, section -> section.id ?: 0L }) { index, section ->
                    SectionCardReadOnly(
                        section = section,
                        index = index + 1,
                        onClick = { section.id?.let(onOpenSection) }
                    )
                }
            }
        }
    }
}

// NUEVA: Card de sección en modo lectura (para AUDITOR/SUPERVISOR)
@Composable
private fun SectionCardReadOnly(
    section: SectionTemplateDto,
    index: Int,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$index. ${section.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${section.items.size} items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    section.percentage?.let { pct ->
                        Text(
                            text = "Peso: ${pct.toPercentageString()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Abrir sección",
                modifier = Modifier.rotate(180f),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ChecklistStructureSuccessContent(
    padding: PaddingValues,
    state: ChecklistStructureUiState.Success,
    validationState: ValidationState,
    loading: Boolean,
    onAddSection: () -> Unit,
    onDistributeSections: () -> Unit,
    onPersistPercentages: () -> Unit,
    onEditSection: (SectionTemplateDto) -> Unit,
    onDeleteSection: (SectionTemplateDto) -> Unit,
    onReorderSection: (Int, Int) -> Unit,
    onOpenItems: (Long) -> Unit,
    onPercentageChange: (Long, Double) -> Unit
) {
    val sections = remember(state.sections) { state.sections.sortedBy { it.orderIndex } }
    val sectionsSum = validationState.sectionsPercentageSum
    val sectionsValid = abs(sectionsSum - 100.0) <= 0.01 && validationState.invalidSectionIds.isEmpty()
    val canPersist = sections.isNotEmpty() && sections.all { it.id != null } && sectionsValid && !loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = state.checklistName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        PercentageSummary(
            label = "Porcentaje total de secciones",
            percentage = sectionsSum,
            isValid = sectionsValid
        )

        // LOG VISUAL: Mostrar las secciones recibidas
        Text(
            text = "Secciones recibidas:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        sections.forEach { s ->
            Text(
                text = "id=${s.id} name=${s.name} pct=${s.percentage} order=${s.orderIndex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(onClick = onAddSection, enabled = !loading) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar sección")
            }

            OutlinedButton(onClick = onDistributeSections, enabled = sections.isNotEmpty() && !loading) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Distribuir 100%")
            }

            OutlinedButton(onClick = onPersistPercentages, enabled = canPersist) {
                Text("Guardar porcentajes")
            }
        }

        if (sections.isEmpty()) {
            EmptySectionsState(onCreate = onAddSection)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val invalidIds = validationState.invalidSectionIds.toSet()
                // Cambia el key para asegurar unicidad y evitar problemas de recomposición
                itemsIndexed(sections, key = { idx, section -> "${section.id ?: "null"}-${section.orderIndex}" }) { index, section ->
                    SectionCard(
                        section = section,
                        index = index,
                        total = sections.size,
                        invalidIds = invalidIds,
                        loading = loading,
                        onEdit = { onEditSection(section) },
                        onDelete = { onDeleteSection(section) },
                        onMoveUp = { onReorderSection(index, index - 1) },
                        onMoveDown = { onReorderSection(index, index + 1) },
                        onOpenItems = { section.id?.let(onOpenItems) },
                        onPercentageChange = { newValue -> section.id?.let { onPercentageChange(it, newValue) } }
                    )
                }
            }
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SectionItemsScreen(
    sectionId: Long,
    viewModel: ChecklistStructureViewModel,
    navigateBack: () -> Unit
) {
    LaunchedEffect(sectionId) { viewModel.loadSectionItems(sectionId) }

    val uiState by viewModel.uiState.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } }
    }

    val section = (uiState as? ChecklistStructureUiState.Success)
        ?.sections
        ?.firstOrNull { it.id == sectionId }

    var itemDialogState by remember { mutableStateOf<ItemDialogState?>(null) }
    var itemToDelete by remember { mutableStateOf<ItemTemplateDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.name ?: "Items") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            section == null && loading -> LoadingState(padding)
            section == null -> ErrorState(
                padding = padding,
                message = "No fue posible cargar la sección.",
                onRetry = { viewModel.loadSectionItems(sectionId) }
            )
            else -> SectionItemsContent(
                padding = padding,
                section = section,
                validationState = validationState,
                loading = loading,
                onAddItem = { itemDialogState = ItemDialogState(sectionId, null) },
                onDistributeItems = { viewModel.distributeItemPercentages(sectionId) },
                onPersistPercentages = {
                    val payload = section.items.mapNotNull { item ->
                        item.id?.let { ItemPercentage(it, item.percentage ?: 0.0) }
                    }
                    if (payload.isNotEmpty()) {
                        viewModel.updateItemPercentages(sectionId, payload)
                    }
                },
                onEditItem = { item -> itemDialogState = ItemDialogState(sectionId, item) },
                onDeleteItem = { item -> itemToDelete = item },
                onReorderItem = { from, to -> viewModel.reorderItems(sectionId, from, to) },
                onPercentageChange = { itemId, value -> viewModel.editItemPercentageLocally(itemId, value) }
            )
        }
    }

    // Diálogos FUERA del Scaffold
    itemDialogState?.let { dialogState ->
        ItemDialog(
            isEditing = dialogState.item != null,
            initialTitle = dialogState.item?.title.orEmpty(),
            initialPercentage = dialogState.item?.percentage ?: 0.0,
            initialFieldType = dialogState.item?.expectedType,
            onDismiss = { itemDialogState = null },
            onConfirm = { title, percentage, fieldType ->
                val templateId = (viewModel.uiState.value as? ChecklistStructureUiState.Success)?.checklistId
                if (dialogState.item == null && templateId != null) {
                    viewModel.createItem(templateId, dialogState.sectionId, title, percentage, fieldType.value)
                } else if (dialogState.item?.id != null) {
                    viewModel.updateItem(dialogState.item.id!!, title, percentage)
                }
                itemDialogState = null
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar item") },
            text = { Text("¿Eliminar el item '${item.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        item.id?.let { viewModel.deleteItem(it) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionItemsContent(
    padding: PaddingValues,
    section: SectionTemplateDto,
    validationState: ValidationState,
    loading: Boolean,
    onAddItem: () -> Unit,
    onDistributeItems: () -> Unit,
    onPersistPercentages: () -> Unit,
    onEditItem: (ItemTemplateDto) -> Unit,
    onDeleteItem: (ItemTemplateDto) -> Unit,
    onReorderItem: (Int, Int) -> Unit,
    onPercentageChange: (Long, Double) -> Unit
) {
    val items = remember(section.items) { section.items.sortedBy { it.orderIndex } }
    val itemsSum = items.sumOf { it.percentage ?: 0.0 }
    val invalidIds = validationState.invalidItemIds.toSet()
    val itemsValid = abs(itemsSum - 100.0) <= 0.01 && invalidIds.isEmpty()
    val canPersist = items.isNotEmpty() && items.all { it.id != null } && itemsValid && !loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PercentageSummary(
            label = "Porcentaje total de items",
            percentage = itemsSum,
            isValid = itemsValid
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(onClick = onAddItem, enabled = !loading) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar item")
            }

            OutlinedButton(onClick = onDistributeItems, enabled = items.isNotEmpty() && !loading) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Distribuir 100%")
            }

            OutlinedButton(onClick = onPersistPercentages, enabled = canPersist) {
                Text("Guardar porcentajes")
            }
        }

        if (items.isEmpty()) {
            EmptyItemsState(onCreate = onAddItem)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id ?: item.orderIndex.toLong() }) { index, item ->
                    ItemCard(
                        item = item,
                        index = index,
                        total = items.size,
                        invalidIds = invalidIds,
                        loading = loading,
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) },
                        onMoveUp = { onReorderItem(index, index - 1) },
                        onMoveDown = { onReorderItem(index, index + 1) },
                        onPercentageChange = { newValue -> item.id?.let { onPercentageChange(it, newValue) } }
                    )
                }
            }
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SectionCard(
    section: SectionTemplateDto,
    index: Int,
    total: Int,
    invalidIds: Set<Long>,
    loading: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpenItems: () -> Unit,
    onPercentageChange: (Double) -> Unit
) {
    var percentageText by remember(section.id, section.percentage) {
        mutableStateOf((section.percentage ?: 0.0).toPercentageString())
    }
    val isInvalid = (section.id != null && section.id in invalidIds) || (section.percentage ?: 0.0) <= 0.0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LOG VISUAL: Mostrar datos de la sección en el card
            Text(
                text = "[SectionCard] id=${section.id} name=${section.name} pct=${section.percentage} order=${section.orderIndex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${index + 1} · ${section.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${section.items.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, enabled = section.id != null && !loading) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar sección")
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = section.id != null && !loading,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar sección")
                    }
                }
            }

            OutlinedTextField(
                value = percentageText,
                onValueChange = { text ->
                    val sanitized = text.replace(',', '.')
                    percentageText = sanitized
                    sanitized.toDoubleOrNull()?.let(onPercentageChange)
                },
                label = { Text("Porcentaje") },
                trailingIcon = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isInvalid,
                enabled = section.id != null && !loading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isInvalid) {
                Text(
                    text = "Debe ser un porcentaje mayor a 0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onOpenItems, enabled = section.id != null) {
                    Text("Ver items")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMoveUp, enabled = index > 0 && !loading) {
                        Text("↑")
                    }
                    IconButton(onClick = onMoveDown, enabled = index < total - 1 && !loading) {
                        Text("↓")
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: ItemTemplateDto,
    index: Int,
    total: Int,
    invalidIds: Set<Long>,
    loading: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPercentageChange: (Double) -> Unit
) {
    var percentageText by remember(item.id, item.percentage) {
        mutableStateOf((item.percentage ?: 0.0).toPercentageString())
    }
    val isInvalid = (item.id != null && item.id in invalidIds) || (item.percentage ?: 0.0) <= 0.0
    val fieldDisplay = remember(item.expectedType) {
        FieldType.fromValue(item.expectedType ?: "")?.displayName ?: (item.expectedType ?: "Sin tipo")
    }

    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${index + 1} · ${item.title.orEmpty()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = fieldDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, enabled = item.id != null && !loading) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar item")
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = item.id != null && !loading,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar item")
                    }
                }
            }

            OutlinedTextField(
                value = percentageText,
                onValueChange = { text ->
                    val sanitized = text.replace(',', '.')
                    percentageText = sanitized
                    sanitized.toDoubleOrNull()?.let(onPercentageChange)
                },
                label = { Text("Porcentaje") },
                trailingIcon = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isInvalid,
                enabled = item.id != null && !loading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isInvalid) {
                Text(
                    text = "Debe ser un porcentaje mayor a 0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    IconButton(onClick = onMoveUp, enabled = index > 0 && !loading) {
                        Text("↑")
                    }
                    IconButton(onClick = onMoveDown, enabled = index < total - 1 && !loading) {
                        Text("↓")
                    }
            }
        }
    }
}

@Composable
fun SectionDialog(
    isEditing: Boolean,
    initialName: String,
    initialPercentage: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var percentageText by remember { mutableStateOf(initialPercentage.takeIf { it > 0 }?.toPercentageString().orEmpty()) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar sección" else "Nueva sección") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = percentageText,
                    onValueChange = {
                        percentageText = it.replace(',', '.')
                        showError = false
                    },
                    label = { Text("Porcentaje") },
                    trailingIcon = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                text = "Ingresa un valor numérico mayor a 0",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val percentage = percentageText.toDoubleOrNull()
                    if (name.isBlank() || percentage == null || percentage <= 0) {
                        showError = true
                    } else {
                        onConfirm(name.trim(), percentage)
                    }
                },
                enabled = name.isNotBlank() && percentageText.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text(if (isEditing) "Actualizar" else "Crear")
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
fun ItemDialog(
    isEditing: Boolean,
    initialTitle: String,
    initialPercentage: Double,
    initialFieldType: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, FieldType) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var percentageText by remember { mutableStateOf(initialPercentage.takeIf { it > 0 }?.toPercentageString().orEmpty()) }
    var showError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedFieldType by remember {
        mutableStateOf(FieldType.fromValue(initialFieldType ?: FieldType.TEXT.value) ?: FieldType.TEXT)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar item" else "Nuevo item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = percentageText,
                    onValueChange = {
                        percentageText = it.replace(',', '.')
                        showError = false
                    },
                    label = { Text("Porcentaje") },
                    trailingIcon = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                text = "Ingresa un valor numérico mayor a 0",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    val fieldTypes = listOf(FieldType.BOOLEAN)
                    OutlinedTextField(
                        value = fieldTypes[0].displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de campo") },
                        trailingIcon = { Text(if (expanded) "\u25b2" else "\u25bc") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showError,
                        enabled = !isEditing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isEditing) { expanded = !expanded },
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(FieldType.BOOLEAN.displayName) },
                            onClick = {
                                selectedFieldType = FieldType.BOOLEAN
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val percentage = percentageText.toDoubleOrNull()
                    if (title.isBlank() || percentage == null || percentage <= 0) {
                        showError = true
                    } else {
                        onConfirm(title.trim(), percentage, selectedFieldType)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditing) "Actualizar" else "Crear")
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
fun PercentageSummary(label: String, percentage: Double, isValid: Boolean) {
    val color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Surface(
        tonalElevation = 4.dp,
        shape = CardDefaults.elevatedShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${percentage.toPercentageString()}%",
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            if (!isValid) {
                Text(
                    text = "La suma debe ser 100%",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
fun EmptySectionsState(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = CardDefaults.elevatedShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Este checklist aún no tiene secciones.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Crea la primera sección para empezar a configurar la estructura.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear primera sección")
            }
        }
    }
}

@Composable
fun EmptyItemsState(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = CardDefaults.elevatedShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Esta sección no tiene items",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Agrega items para completar la evaluación de esta sección.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar primer item")
            }
        }
    }
}

@Composable
fun ErrorState(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
    }
}