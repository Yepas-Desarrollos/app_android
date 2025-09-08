package mx.checklist.ui.screens.fields

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import java.io.File

/**
 * Lienzo de firma en Compose.
 * Guarda un PNG en cache y devuelve el File vía [onSaved].
 */
@Composable
fun SignatureField(
    enabled: Boolean = true,
    onSaved: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // Lista de trazos; cada trazo es una lista de puntos
    val strokes = remember { mutableStateListOf<MutableList<Offset>>() }
    var current by remember { mutableStateOf<MutableList<Offset>?>(null) }

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .onSizeChanged { boxSize = it }
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { start ->
                            current = mutableListOf<Offset>().also { it.add(start) }
                            strokes.add(current!!)
                        },
                        onDrag = { change, _ ->
                            current?.add(change.position)
                        },
                        onDragEnd = { current = null },
                        onDragCancel = { current = null }
                    )
                }
                .padding(4.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                strokes.forEach { points ->
                    if (points.size >= 2) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(path, color = Color.Black, style = stroke)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                enabled = enabled && strokes.isNotEmpty(),
                onClick = { strokes.clear() }
            ) { Text("Borrar") }

            Button(
                enabled = enabled && strokes.any { it.size > 1 },
                onClick = {
                    // Renderizar a Bitmap y guardar PNG en cache
                    val w = boxSize.width.coerceAtLeast(1)
                    val h = boxSize.height.coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(Color.White.toArgb())

                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 6f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }

                    strokes.forEach { pts ->
                        for (i in 1 until pts.size) {
                            val p0 = pts[i - 1]
                            val p1 = pts[i]
                            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
                        }
                    }

                    val file = File(
                        context.cacheDir,
                        "signature_${System.currentTimeMillis()}.png"
                    )
                    file.outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    onSaved(file)
                }
            ) { Text("Guardar firma") }
        }
    }
}
