package mx.checklist.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import mx.checklist.data.api.dto.TeamCorrectionDto
import mx.checklist.viewmodel.AuditReviewViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Correcciones del Equipo para MGR_OPS
 * Vista solo lectura de las correcciones realizadas por supervisores
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCorrectionsScreen(
    viewModel: AuditReviewViewModel,
    onBack: () -> Unit
) {
    val teamCorrections by viewModel.teamCorrections.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.teamIsLoadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.teamHasMore.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedCorrection by remember { mutableStateOf<TeamCorrectionDto?>(null) }
    val listState = rememberLazyListState()

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        viewModel.loadTeamCorrections()
    }

    // Detectar scroll al final para cargar más
    LaunchedEffect(listState) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && hasMore && !isLoadingMore && !isLoading) {
                viewModel.loadMoreTeamCorrections()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Correcciones del Equipo", 
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            "Volver", 
                            tint = Color(0xFF90CAF9)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTeamCorrections() }) {
                        Icon(
                            Icons.Default.Refresh, 
                            "Recargar", 
                            tint = Color(0xFF90CAF9)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1F2B)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && teamCorrections.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF90CAF9)
                    )
                }
                error != null && teamCorrections.isEmpty() -> {
                    TeamErrorState(
                        error = error!!,
                        onRetry = { viewModel.loadTeamCorrections() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                teamCorrections.isEmpty() -> {
                    TeamEmptyState(
                        message = "No hay correcciones del equipo",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = teamCorrections,
                            key = { it.id }
                        ) { correction ->
                            TeamCorrectionCard(
                                correction = correction,
                                onClick = { selectedCorrection = correction }
                            )
                        }

                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = Color(0xFF90CAF9)
                                    )
                                }
                            }
                        }

                        if (hasMore && !isLoadingMore) {
                            item {
                                TextButton(
                                    onClick = { viewModel.loadMoreTeamCorrections() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cargar más", color = Color(0xFF90CAF9))
                                }
                            }
                        }
                    }
                }
            }

            // Error con datos
            if (error != null && teamCorrections.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFFB71C1C),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(error!!, color = Color.White)
                }
            }
        }
    }

    // Diálogo de detalle
    selectedCorrection?.let { correction ->
        TeamCorrectionDetailDialog(
            correction = correction,
            onDismiss = { selectedCorrection = null }
        )
    }
}

/**
 * Card de corrección del equipo
 */
@Composable
private fun TeamCorrectionCard(
    correction: TeamCorrectionDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF232A36)
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(1.5.dp, Color(0xFF90CAF9).copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Título del item
            Text(
                text = correction.auditItemTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tienda
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${correction.storeName} (${correction.storeCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0BEC5)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Supervisor que corrigió
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = correction.correctedByUser.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }

            // Notas de corrección
            correction.correctionNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$notes\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Fotos
            if (correction.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${correction.attachments.size} foto${if (correction.attachments.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90CAF9)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(correction.attachments.take(4)) { attachment ->
                        AsyncImage(
                            model = attachment.url,
                            contentDescription = "Evidencia",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (correction.attachments.size > 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1F2B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${correction.attachments.size - 4}",
                                    color = Color(0xFF90CAF9),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fecha
            Text(
                text = formatTeamDate(correction.correctedAt),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0BEC5)
            )
        }
    }
}

/**
 * Diálogo de detalle de corrección
 */
@Composable
private fun TeamCorrectionDetailDialog(
    correction: TeamCorrectionDto,
    onDismiss: () -> Unit
) {
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1F2B)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF232A36))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detalle de Corrección",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color(0xFFB0BEC5))
                    }
                }

                // Contenido
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item
                    item {
                        TeamDetailSection(title = "Item de Auditoría") {
                            Text(
                                correction.auditItemTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Tienda
                    item {
                        TeamDetailSection(title = "Tienda") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Store,
                                    null,
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${correction.storeName} (${correction.storeCode})",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Supervisor
                    item {
                        TeamDetailSection(title = "Corregido por") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    correction.correctedByUser.name,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatTeamDate(correction.correctedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    }

                    // Notas
                    correction.correctionNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item {
                            TeamDetailSection(title = "Notas de Corrección") {
                                Text(notes, color = Color.White)
                            }
                        }
                    }

                    // Fotos
                    if (correction.attachments.isNotEmpty()) {
                        item {
                            TeamDetailSection(title = "Evidencia (${correction.attachments.size} foto${if (correction.attachments.size > 1) "s" else ""})") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(correction.attachments) { attachment ->
                                        AsyncImage(
                                            model = attachment.url,
                                            contentDescription = "Evidencia",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedPhotoUrl = attachment.url },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Visor de foto
    selectedPhotoUrl?.let { url ->
        TeamPhotoViewerDialog(
            photoUrl = url,
            onDismiss = { selectedPhotoUrl = null }
        )
    }
}

/**
 * Sección de detalle
 */
@Composable
private fun TeamDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF90CAF9),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

/**
 * Visor de foto fullscreen
 */
@Composable
private fun TeamPhotoViewerDialog(
    photoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Evidencia",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Estado vacío
 */
@Composable
private fun TeamEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Groups,
            contentDescription = null,
            tint = Color(0xFF90CAF9).copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            color = Color(0xFFB0BEC5),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Estado de error
 */
@Composable
private fun TeamErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = Color(0xFFEF5350),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            error,
            color = Color(0xFFEF5350),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9))
        ) {
            Text("Reintentar", color = Color.Black)
        }
    }
}

// Helper
private fun formatTeamDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        parser.parse(isoDate)?.let { formatter.format(it) } ?: isoDate.take(10)
    } catch (e: Exception) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            parser.parse(isoDate)?.let { formatter.format(it) } ?: isoDate.take(10)
        } catch (e2: Exception) {
            isoDate.take(10)
        }
    }
}
