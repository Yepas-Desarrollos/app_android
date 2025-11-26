@file:OptIn(ExperimentalMaterial3Api::class)

package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.checklist.data.api.dto.ItemPercentage
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.data.api.dto.SectionTemplateDto
import mx.checklist.ui.components.EmptyItemsState
import mx.checklist.ui.components.ErrorState
import mx.checklist.ui.components.ItemCard
import mx.checklist.ui.components.LoadingState
import mx.checklist.ui.components.PercentageSummary
import mx.checklist.ui.dialogs.ItemDialog
import mx.checklist.ui.vm.ChecklistStructureUiState
import mx.checklist.ui.vm.ChecklistStructureViewModel
import mx.checklist.ui.vm.ValidationState
import kotlin.math.abs

private data class ItemDialogState(
    val sectionId: Long,
    val item: ItemTemplateDto?
)

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
                        onMoveUp = { onReorderItem(index, index + 1) },
                        onMoveDown = { onReorderItem(index, index - 1) },
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
