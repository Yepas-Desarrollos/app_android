package mx.checklist.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.checklist.data.api.RespondReq
import mx.checklist.ui.ItemsVM


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(runId:Long, storeCode:String, vm: ItemsVM, onSubmit:()->Unit){
    val items by vm.items.collectAsState()
    LaunchedEffect(runId){ vm.load(runId) }
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        items(items, key={it.id}) { it ->
            val tmpl = it.itemTemplate
            Card(Modifier.fillMaxWidth().padding(4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${tmpl.category ?: ""} ${tmpl.subcategory ?: ""}".trim(), style=MaterialTheme.typography.labelSmall)
                    Text("${it.orderIndex}. ${tmpl.title}", style=MaterialTheme.typography.titleMedium)
                    when (tmpl.expectedType) {
                        "OK_NA_FAIL" -> {
                            Row {
                                listOf("OK","FAIL","NA").forEach { opt ->
                                    FilterChip(selected = it.responseStatus == opt,
                                        onClick = { vm.respond(it.id, RespondReq(responseStatus = opt)) { vm.load(runId) } },
                                        label = { Text(opt) }, modifier = Modifier.padding(end=8.dp))
                                }
                            }
                        }
                        "TEXT" -> {
                            var txt by remember(it.id){ mutableStateOf(it.responseText ?: "") }
                            OutlinedTextField(value=txt, onValueChange={txt=it}, label={Text("Respuesta")})
                            Button(onClick={ vm.respond(it.id, RespondReq(responseText = txt)) { vm.load(runId) } }){ Text("Guardar") }
                        }
                        "NUMERIC" -> {
                            var num by remember(it.id){ mutableStateOf(it.responseNumber?.toString() ?: "") }
                            OutlinedTextField(value=num, onValueChange={num=it.filter{c-> c.isDigit() || c=='.'}},
                                label={Text("Número")}, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Button(onClick={ vm.respond(it.id, RespondReq(responseNumber = num.toDoubleOrNull())) { vm.load(runId) } }){ Text("Guardar") }
                        }
                        else -> Text("Tipo ${tmpl.expectedType} (pendiente)")
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.submit(runId) { onSubmit() } },
                modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("Enviar checklist") }
        }
    }
}
