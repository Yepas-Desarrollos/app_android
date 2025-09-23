package mx.checklist.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.api.dto.TemplateDto
import mx.checklist.ui.vm.RunsViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun TemplatesScreen(
    storeCode: String,
    vm: RunsViewModel,
    onRunCreated: (Long, String) -> Unit
) {
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val allTemplates = vm.getTemplates().collectAsStateWithLifecycle().value

    // Log de diagnóstico para ver tamaño y nombres
    LaunchedEffect(allTemplates) {
        Log.d("TEMPLATES", "count=${allTemplates.size} names=${allTemplates.joinToString { it.name }}")
    }

    // NO filtrar por scope - el backend ya filtra por rol
    // Pero SÍ filtrar por isActive - solo mostrar templates activos a usuarios
    val templates = allTemplates
        .filter { it.isActive }
        .sortedBy { it.name.lowercase(Locale.getDefault()) }

    // Sugerencias por día (Calendar = minSdk 24 OK)
    val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val suggestedGroup = if (day == Calendar.MONDAY || day == Calendar.TUESDAY)
        "Lunes y Martes" else "Miércoles a Domingo"

    val groupLM = templates.filter { it.name.contains("Lunes y Martes", ignoreCase = true) }
    val groupMD = templates.filter { it.name.contains("Miércoles a Domingo", ignoreCase = true) }

    // Recomendadas (subset del tramo del día)
    val recommended = templates.filter { it.name.contains(suggestedGroup, ignoreCase = true) }

    //  Evitar DUPLICADOS entre secciones
    val idsLM = groupLM.map { it.id }.toSet()
    val idsMD = groupMD.map { it.id }.toSet()

    val recommendedUnique = recommended.filterNot { idsLM.contains(it.id) || idsMD.contains(it.id) }
    val others = templates.filterNot { idsLM.contains(it.id) || idsMD.contains(it.id) || recommendedUnique.any { r -> r.id == it.id } }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Plantillas — Tienda $storeCode", style = MaterialTheme.typography.headlineSmall)
            if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        }

        // Mostrar mensaje si no hay templates
        if (templates.isEmpty() && !loading) {
            item {
                Text(
                    "No hay plantillas disponibles para tu rol",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sugeridas
        if (recommendedUnique.isNotEmpty()) {
            item { Divider(Modifier.padding(vertical = 8.dp)) }
            item { Text("Sugeridas para hoy ($suggestedGroup)", style = MaterialTheme.typography.titleMedium) }
            items(
                items = recommendedUnique,
                key = { t -> "rec-${t.id}" }
            ) { t ->
                TemplateCard(
                    t = t,
                    loading = loading,
                    onCreate = { vm.createRun(storeCode, t.id) { id -> onRunCreated(id, storeCode) } }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Lunes y Martes
        if (groupLM.isNotEmpty()) {
            item { Divider(Modifier.padding(vertical = 8.dp)) }
            item { Text("Lunes y Martes", style = MaterialTheme.typography.titleMedium) }
            items(
                items = groupLM,
                key = { t -> "lm-${t.id}" }
            ) { t ->
                TemplateCard(
                    t = t,
                    loading = loading,
                    onCreate = { vm.createRun(storeCode, t.id) { id -> onRunCreated(id, storeCode) } }
                )
            }
        }

        // Miércoles a Domingo
        if (groupMD.isNotEmpty()) {
            item { Divider(Modifier.padding(vertical = 8.dp)) }
            item { Text("Miércoles a Domingo", style = MaterialTheme.typography.titleMedium) }
            items(
                items = groupMD,
                key = { t -> "md-${t.id}" }
            ) { t ->
                TemplateCard(
                    t = t,
                    loading = loading,
                    onCreate = { vm.createRun(storeCode, t.id) { id -> onRunCreated(id, storeCode) } }
                )
            }
        }

        // Otros (por si agregas más en el futuro)
        if (others.isNotEmpty()) {
            item { Divider(Modifier.padding(vertical = 8.dp)) }
            item { Text("Otros", style = MaterialTheme.typography.titleMedium) }
            items(
                items = others,
                key = { t -> "oth-${t.id}" }
            ) { t ->
                TemplateCard(
                    t = t,
                    loading = loading,
                    onCreate = { vm.createRun(storeCode, t.id) { id -> onRunCreated(id, storeCode) } }
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    t: TemplateDto,
    loading: Boolean,
    onCreate: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium)
            val meta = listOfNotNull(
                t.scope?.takeIf { it.isNotBlank() }?.let { "Ámbito: $it" },
                t.frequency?.takeIf { it.isNotBlank() }?.let { "Frecuencia: $it" },
                t.version?.let { "Versión: $it" }
            ).joinToString("  •  ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)

            Button(
                enabled = !loading,
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Iniciar checklist") }
        }
    }
}
