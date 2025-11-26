@file:OptIn(ExperimentalMaterial3Api::class)

package mx.checklist.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.checklist.data.api.dto.FieldType
import java.text.NumberFormat

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
                        trailingIcon = { Text(if (expanded) "▲" else "▼") },
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

private fun Double.toPercentageString(): String {
    return NumberFormat.getInstance().apply {
        maximumFractionDigits = 2
    }.format(this)
}
