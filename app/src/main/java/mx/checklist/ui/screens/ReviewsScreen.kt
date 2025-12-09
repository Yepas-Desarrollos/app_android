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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import mx.checklist.data.api.dto.CorrectedReviewDto
import mx.checklist.data.api.dto.ReviewDetailDto
import mx.checklist.viewmodel.AuditReviewViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Revisiones para AUDITORES
 * Muestra tabs: Por Validar | Historial
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    viewModel: AuditReviewViewModel,
    onBack: () -> Unit
) {
    // Tab seleccionado
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Por Validar", "Historial")
    
    // Por Validar (CORRECTED)
    val correctedReviews by viewModel.correctedReviews.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.correctedIsLoadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.correctedHasMore.collectAsStateWithLifecycle()
    
    // Historial (VALIDATED + REJECTED)
    val validatedReviews by viewModel.validatedReviews.collectAsStateWithLifecycle()
    val validatedIsLoadingMore by viewModel.validatedIsLoadingMore.collectAsStateWithLifecycle()
    val validatedHasMore by viewModel.validatedHasMore.collectAsStateWithLifecycle()
    
    // Estados compartidos
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val reviewDetail by viewModel.reviewDetail.collectAsStateWithLifecycle()

    var selectedReviewId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val historyListState = rememberLazyListState()

    // Cargar datos iniciales según tab
    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> viewModel.loadCorrectedReviews()
            1 -> viewModel.loadValidatedHistory()
        }
    }

    // Scroll infinito para tab activo
    LaunchedEffect(listState, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            snapshotFlow { 
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 3
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && hasMore && !isLoadingMore && !isLoading) {
                    viewModel.loadMoreCorrectedReviews()
                }
            }
        }
    }
    
    LaunchedEffect(historyListState, selectedTabIndex) {
        if (selectedTabIndex == 1) {
            snapshotFlow { 
                val layoutInfo = historyListState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 3
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && validatedHasMore && !validatedIsLoadingMore && !isLoading) {
                    viewModel.loadMoreValidatedHistory()
                }
            }
        }
    }

    // Cargar detalle
    LaunchedEffect(selectedReviewId) {
        selectedReviewId?.let { id -> viewModel.loadReviewDetail(id) }
    }

    // Limpiar mensajes
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            "Validar Correcciones", 
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
                        IconButton(onClick = { 
                            if (selectedTabIndex == 0) viewModel.loadCorrectedReviews()
                            else viewModel.loadValidatedHistory()
                        }) {
                            Icon(Icons.Default.Refresh, "Recargar", tint = Color(0xFF90CAF9))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1F2B)
                    )
                )
                
                // TabRow
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1A1F2B),
                    contentColor = Color(0xFF90CAF9),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 3.dp,
                            color = Color(0xFF90CAF9)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            0 -> correctedReviews.size
                            1 -> validatedReviews.size
                            else -> 0
                        }
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = if (count > 0) "$title ($count)" else title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Contenido según tab seleccionado
            when (selectedTabIndex) {
                0 -> {
                    // Tab: Por Validar
                    when {
                        isLoading && correctedReviews.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFF90CAF9)
                            )
                        }
                        error != null && correctedReviews.isEmpty() -> {
                            ReviewErrorState(
                                error = error!!,
                                onRetry = { viewModel.loadCorrectedReviews() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        correctedReviews.isEmpty() -> {
                            ReviewEmptyState(
                                message = "¡Excelente! No hay correcciones pendientes",
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
                                    items = correctedReviews,
                                    key = { it.id }
                                ) { review ->
                                    CorrectedReviewCard(
                                        review = review,
                                        onClick = { selectedReviewId = review.id },
                                        showStatusBadge = false
                                    )
                                }

                                if (isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = Color(0xFF90CAF9)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Tab: Historial
                    when {
                        isLoading && validatedReviews.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFF90CAF9)
                            )
                        }
                        error != null && validatedReviews.isEmpty() -> {
                            ReviewErrorState(
                                error = error!!,
                                onRetry = { viewModel.loadValidatedHistory() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        validatedReviews.isEmpty() -> {
                            ReviewEmptyState(
                                message = "Aún no has validado correcciones",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            LazyColumn(
                                state = historyListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = validatedReviews,
                                    key = { it.id }
                                ) { review ->
                                    CorrectedReviewCard(
                                        review = review,
                                        onClick = { },
                                        showStatusBadge = true,
                                        isHistoryItem = true
                                    )
                                }

                                if (validatedIsLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = Color(0xFF90CAF9)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar de éxito
            successMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFF2E7D32)
                ) {
                    Text(msg, color = Color.White)
                }
            }

            // Snackbar de error
            if (error != null && (correctedReviews.isNotEmpty() || validatedReviews.isNotEmpty())) {
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
    if (selectedReviewId != null && reviewDetail != null) {
        ReviewDetailDialog(
            detail = reviewDetail!!,
            viewModel = viewModel,
            onDismiss = {
                selectedReviewId = null
                viewModel.clearReviewDetail()
            }
        )
    }
}

/**
 * Card de revisión corregida
 */
@Composable
private fun CorrectedReviewCard(
    review: CorrectedReviewDto,
    onClick: () -> Unit,
    showStatusBadge: Boolean = true,
    isHistoryItem: Boolean = false
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
            // Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Título
                Text(
                    text = review.auditItemTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90CAF9),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Badge de estado (dinámico según status)
                if (showStatusBadge) {
                    val (badgeText, badgeColor) = when (review.status) {
                        "VALIDATED", "APPROVED" -> "APROBADO" to Color(0xFF4CAF50)
                        "REJECTED" -> "RECHAZADO" to Color(0xFFEF5350)
                        else -> "CORREGIDO" to Color(0xFFFFB74D)
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = badgeColor.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tienda con código
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (review.storeCode != null) "${review.storeName} (${review.storeCode})" else review.storeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0BEC5)
                )
            }

            // Nombre del checklist
            review.checklistTemplateName?.let { templateName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = templateName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Corregido por
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Corregido por: ${review.correctedByUserName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }

            // Notas de corrección
            review.correctionNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$notes\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Fotos preview
            if (review.correctionPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${review.correctionPhotos.size} foto${if (review.correctionPhotos.size > 1) "s" else ""} de evidencia",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90CAF9)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thumbnails de fotos
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(review.correctionPhotos.take(4)) { photoUrl ->
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de evidencia",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (review.correctionPhotos.size > 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1F2B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${review.correctionPhotos.size - 4}",
                                    color = Color(0xFF90CAF9),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fecha de corrección
            Text(
                text = "Corregido: ${formatReviewDate(review.correctedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0BEC5)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón validar (solo si no es item de historial)
            if (!isHistoryItem) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF90CAF9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Revisar y Validar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Diálogo de detalle de revisión
 */
@Composable
private fun ReviewDetailDialog(
    detail: ReviewDetailDto,
    viewModel: AuditReviewViewModel,
    onDismiss: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showApproveConfirm by remember { mutableStateOf(false) }
    var showRejectConfirm by remember { mutableStateOf(false) }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1F2B)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                        "Detalle de Revisión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color(0xFFB0BEC5))
                    }
                }

                // Contenido scrollable
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Info del item
                    item {
                        DetailSection(title = "Item de Auditoría") {
                            Text(
                                detail.auditItem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            detail.auditItem.category?.let { cat ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Categoría: $cat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }

                    // Tienda
                    item {
                        DetailSection(title = "Tienda") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Store,
                                    null,
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${detail.store.name} (${detail.store.code})",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Notas originales del auditor
                    detail.auditNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item {
                            DetailSection(
                                title = "Notas del Auditor",
                                titleColor = Color(0xFFFFB74D)
                            ) {
                                Text(notes, color = Color(0xFFFFB74D))
                            }
                        }
                    }

                    // Correcciones
                    if (detail.corrections.isNotEmpty()) {
                        item {
                            Text(
                                "Corrección Realizada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        detail.corrections.forEach { correction ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF232A36)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Corregido por
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Person,
                                                null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                correction.correctedByUser.name,
                                                color = Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            formatReviewDate(correction.correctedAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB0BEC5)
                                        )

                                        // Notas de corrección
                                        correction.correctionNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Notas:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB0BEC5)
                                            )
                                            Text(notes, color = Color.White)
                                        }

                                        // Fotos de evidencia
                                        if (correction.attachments.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Evidencia (${correction.attachments.size} foto${if (correction.attachments.size > 1) "s" else ""}):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB0BEC5)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(correction.attachments) { attachment ->
                                                    AsyncImage(
                                                        model = attachment.url,
                                                        contentDescription = "Evidencia",
                                                        modifier = Modifier
                                                            .size(100.dp)
                                                            .clip(RoundedCornerShape(8.dp))
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

                // Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF232A36))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Rechazar
                    OutlinedButton(
                        onClick = { showRejectConfirm = true },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF5350)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFEF5350))
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rechazar", fontWeight = FontWeight.Bold)
                    }

                    // Botón Aprobar
                    Button(
                        onClick = { showApproveConfirm = true },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aprobar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Diálogo de confirmación aprobar
    if (showApproveConfirm) {
        AlertDialog(
            onDismissRequest = { showApproveConfirm = false },
            containerColor = Color(0xFF1A1F2B),
            title = { Text("Confirmar Aprobación", color = Color(0xFF90CAF9)) },
            text = { Text("¿Estás seguro de aprobar esta corrección?", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveConfirm = false
                        viewModel.validateCorrection(detail.id, approved = true) {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Aprobar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveConfirm = false }) {
                    Text("Cancelar", color = Color(0xFFB0BEC5))
                }
            }
        )
    }

    // Diálogo de confirmación rechazar
    if (showRejectConfirm) {
        AlertDialog(
            onDismissRequest = { showRejectConfirm = false },
            containerColor = Color(0xFF1A1F2B),
            title = { Text("Confirmar Rechazo", color = Color(0xFFEF5350)) },
            text = { 
                Text(
                    "¿Estás seguro de rechazar esta corrección? El supervisor deberá corregirla nuevamente.",
                    color = Color.White
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectConfirm = false
                        viewModel.validateCorrection(detail.id, approved = false) {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Rechazar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirm = false }) {
                    Text("Cancelar", color = Color(0xFFB0BEC5))
                }
            }
        )
    }

    // Visor de foto fullscreen
    selectedPhotoUrl?.let { url ->
        PhotoViewerDialog(
            photoUrl = url,
            onDismiss = { selectedPhotoUrl = null }
        )
    }
}

/**
 * Sección de detalle
 */
@Composable
private fun DetailSection(
    title: String,
    titleColor: Color = Color(0xFF90CAF9),
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = titleColor,
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
private fun PhotoViewerDialog(
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
                contentDescription = "Foto de evidencia",
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
private fun ReviewEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50).copy(alpha = 0.5f),
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
private fun ReviewErrorState(
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
private fun formatReviewDate(isoDate: String): String {
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
