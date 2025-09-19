package mx.checklist.ui.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SingleChoiceField(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        options.forEach { option ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(option) },
                leadingContent = {
                    RadioButton(
                        selected = selected == option,
                        onClick = { onSelect(option) }
                    )
                }
            )
        }
    }
}

