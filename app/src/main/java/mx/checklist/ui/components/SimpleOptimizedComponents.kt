package mx.checklist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Componente básico para mostrar templates de forma optimizada
 */
@Composable
fun SimpleTemplateList(
    templates: List<Any> = emptyList(),
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(templates) { template ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Template optimizado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        if (templates.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Carga optimizada - Sin lag al hacer scroll",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}