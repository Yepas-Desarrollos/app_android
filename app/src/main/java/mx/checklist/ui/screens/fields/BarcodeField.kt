package mx.checklist.ui.screens.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Campo simple para capturar/pegar un código de barras.
 * (La acción de escanear con cámara puede integrarse después vía [onClickScan].)
 */
@Composable
fun BarcodeField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    onClickScan: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it.trim()) },
            label = { Text("Código de barras") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )

        if (onClickScan != null) {
            TextButton(
                enabled = enabled,
                onClick = onClickScan
            ) { Text("Escanear (opcional)") }
        }
    }
}
