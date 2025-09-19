package mx.checklist.ui.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ScaleField(
    min: Int,
    max: Int,
    step: Int,
    value: Int?,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(value?.toFloat() ?: min.toFloat()) }
    Column(Modifier.fillMaxWidth()) {
        Text("Valor: ${'$'}{sliderValue.toInt()}")
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.toInt())
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = ((max - min) / step) - 1
        )
    }
}

