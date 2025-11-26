@file:OptIn(ExperimentalMaterial3Api::class)

package mx.checklist.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.checklist.data.api.dto.FieldType
import mx.checklist.data.api.dto.ItemTemplateDto
import mx.checklist.data.api.dto.SectionTemplateDto
import mx.checklist.ui.screens.toPercentageString
import java.util.Locale

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
