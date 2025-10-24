package mx.checklist.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * Miniatura de imagen clickeable que abre el visor en pantalla completa.
 *
 * Mejores prácticas:
 * - Uso de Coil para carga eficiente de imágenes
 * - Caché de disco y memoria automático
 * - Estados de carga visuales
 * - Interacción táctil responsiva
 */
@Composable
fun ClickableImageThumbnail(
    url: String?,
    localUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    readOnly: Boolean = false,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        // Modelo de imagen con optimizaciones
        val imageModel = remember(url, localUri) {
            ImageRequest.Builder(context)
                .data(localUri ?: url)
                .crossfade(300)
                .diskCacheKey(url)
                .memoryCacheKey(url)
                .build()
        }

        val painter = rememberAsyncImagePainter(imageModel)
        val painterState = painter.state

        // Imagen principal clickeable
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Sin efecto ripple para UX más limpia
                ) {
                    onImageClick()
                },
            contentScale = ContentScale.Crop
        )

        // Indicador de carga
        if (painterState is AsyncImagePainter.State.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            }
        }

        // Botón de eliminar (solo si no es readOnly y se proporciona onDelete)
        if (!readOnly && onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar foto",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Visor de imágenes en pantalla completa con gestos táctiles avanzados.
 *
 * Características:
 * - Pinch to zoom (1x a 5x)
 * - Pan (arrastrar) cuando hay zoom aplicado
 * - Doble tap para zoom progresivo (1x → 2x → 3x → 5x → 1x)
 * - Cierre con botón X o tap fuera de la imagen
 *
 * Mejores prácticas según Context7:
 * - Dialog con propiedades optimizadas para UX móvil
 * - Gestos nativos de Android
 * - Estados reactivos con remember
 * - Animaciones suaves
 */
@Composable
fun FullscreenImageViewer(
    url: String?,
    localUri: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val context = LocalContext.current

        // Estados para transformaciones
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        // Modelo de imagen en alta calidad
        val imageModel = remember(url, localUri) {
            ImageRequest.Builder(context)
                .data(localUri ?: url)
                .crossfade(300)
                .diskCacheKey(url)
                .memoryCacheKey(url)
                .build()
        }

        val painter = rememberAsyncImagePainter(imageModel)
        val painterState = painter.state

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Imagen con transformaciones y gestos combinados
            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                scale = newScale

                // Permitir pan cuando hay zoom aplicado
                if (scale > 1f) {
                    // Limitar el pan para evitar que la imagen se salga demasiado
                    val maxOffset = 1000f * scale
                    offsetX = (offsetX + panChange.x).coerceIn(-maxOffset, maxOffset)
                    offsetY = (offsetY + panChange.y).coerceIn(-maxOffset, maxOffset)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Solo cerrar si no hay zoom aplicado
                                if (scale <= 1.1f) {
                                    onDismiss()
                                }
                            },
                            onDoubleTap = {
                                // Doble tap para zoom progresivo: 1x → 2x → 3x → 5x → 1x
                                scale = when {
                                    scale < 1.5f -> 2f  // De 1x a 2x
                                    scale < 2.5f -> 3f  // De 2x a 3x
                                    scale < 4f -> 5f    // De 3x a 5x
                                    else -> 1f          // De 5x vuelve a 1x
                                }

                                // Resetear offset al volver a 1x
                                if (scale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        )
                    }
                    .transformable(state = transformableState),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Imagen en pantalla completa",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            // Indicador de carga
            if (painterState is AsyncImagePainter.State.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White
                    )
                }
            }

            // Botón de cierre siempre visible
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Indicador de zoom (opcional, se muestra solo cuando hay zoom)
            AnimatedVisibility(
                visible = scale > 1.1f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    androidx.compose.material3.Text(
                        text = "${(scale * 100).toInt()}%",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

