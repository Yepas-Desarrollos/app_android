package mx.checklist.ui.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun BarcodeField(
    value: String?,
    onScan: (String) -> Unit
) {
    var barcode by remember { mutableStateOf(value.orEmpty()) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("Código de barras") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onScan(barcode) }, enabled = barcode.isNotBlank()) {
            Text("Guardar código")
        }
    }
}

