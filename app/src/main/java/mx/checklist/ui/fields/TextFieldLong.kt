package mx.checklist.ui.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun TextFieldLong(
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (it.length <= maxLength) {
                    text = it
                    onValueChange(it)
                }
            },
            label = { Text("Texto (máx. ${'$'}maxLength)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )
        Text("${'$'}{text.length} / ${'$'}maxLength caracteres")
    }
}

