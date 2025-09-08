package mx.checklist.ui.screens.fields

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * Campo para evidencias fotográficas.
 *
 * - "Cámara": abre la cámara y guarda en cache (FileProvider).
 * - "Galería": permite seleccionar múltiples imágenes.
 * - Muestra miniaturas de las URLs existentes (ya subidas).
 *
 * Requiere:
 * - Permiso CAMERA en el Manifest.
 * - FileProvider configurado (ver manifest y res/xml/provider_paths.xml).
 */
@Composable
fun PhotoField(
    itemId: Long,
    existing: List<String>,              // URLs ya existentes
    onPickFiles: (List<File>) -> Unit,   // se llama con los archivos listos para subir
    readOnly: Boolean
) {
    val ctx = LocalContext.current
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Cámara (TakePicture)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            lastPhotoUri?.let { uri ->
                val file = uri.toFileCompat(ctx)
                if (file != null) onPickFiles(listOf(file))
            }
        }
    }

    // Galería (múltiples)
    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val files = uris.mapNotNull { it.toFileCompat(ctx) }
        if (files.isNotEmpty()) onPickFiles(files)
    }

    // Asegurar que si cambia el itemId, limpiamos el último uri
    LaunchedEffect(itemId) { lastPhotoUri = null }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled = !readOnly,
                onClick = {
                    val (uri, _) = createTempImageUri(ctx)
                    lastPhotoUri = uri
                    takePictureLauncher.launch(uri)
                }
            ) { Text("Cámara") }

            Button(
                enabled = !readOnly,
                onClick = { pickMultipleLauncher.launch("image/*") }
            ) { Text("Galería") }
        }

        if (existing.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(existing) { url ->
                    Box(Modifier.height(90.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(90.dp)
                                .fillMaxWidth(0.45f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun createTempImageUri(ctx: Context): Pair<Uri, File> {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(ctx.cacheDir, "evidence_$time.jpg")
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    return uri to file
}

/**
 * Convierte un Uri a File en cache (copiando cuando es "content://").
 */
private fun Uri.toFileCompat(ctx: Context): File? {
    return when (scheme) {
        "file" -> File(path ?: return null)
        "content" -> {
            val input = ctx.contentResolver.openInputStream(this) ?: return null
            val out = File(ctx.cacheDir, "pick_${System.currentTimeMillis()}.jpg")
            input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            out
        }
        else -> null
    }
}
