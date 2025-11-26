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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.checklist.data.api.dto.ItemPercentage
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.data.api.dto.SectionPercentage
import mx.checklist.data.api.dto.SectionTemplateDto
import mx.checklist.ui.components.EmptySectionsState
import mx.checklist.ui.components.ErrorState
import mx.checklist.ui.components.LoadingState
import mx.checklist.ui.components.PercentageSummary
import mx.checklist.ui.components.SectionCard
import mx.checklist.ui.dialogs.SectionDialog
import mx.checklist.ui.screens.readonly.ChecklistStructureReadOnlyContent
import mx.checklist.ui.vm.ChecklistStructureUiState
import mx.checklist.ui.vm.ChecklistStructureViewModel
import mx.checklist.ui.vm.ValidationState
import java.util.Locale
import kotlin.math.abs

private data class SectionDialogState(
    val checklistId: Long,
    val section: SectionTemplateDto?
)

internal fun Double.toPercentageString(): String =
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
                    ChecklistStructureReadOnlyContent(
                        padding = padding,
                        state = state,
                        onOpenSection = onOpenSectionItems
                    )
                }
            }
        }
    }

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
