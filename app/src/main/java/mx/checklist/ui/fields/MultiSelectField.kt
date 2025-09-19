package mx.checklist.ui.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MultiSelectField(
    options: List<String>,
    selected: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        options.forEach { option ->
            Row(Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = selected.contains(option),
                    onCheckedChange = { checked ->
                        val newSelected = if (checked) selected + option else selected - option
                        onSelectionChange(newSelected.distinct())
                    }
                )
                Text(option)
            }
        }
    }
}

