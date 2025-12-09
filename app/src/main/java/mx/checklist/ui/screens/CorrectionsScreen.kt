package mx.checklist.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import mx.checklist.data.api.dto.PendingReviewDto
import mx.checklist.data.api.dto.MyCorrectionDto
import mx.checklist.viewmodel.AuditReviewViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import mx.checklist.util.formatRelativeTime

/**
 * Pantalla de Correcciones para SUPERVISORES
 * Muestra lista de items pendientes de corrección con paginación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrectionsScreen(
    viewModel: AuditReviewViewModel,
    onBack: () -> Unit
) {
    // Tab seleccionado
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pendientes", "Mis Correcciones")

    // Pendientes
    val pendingReviews by viewModel.pendingReviews.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.pendingIsLoadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.pendingHasMore.collectAsStateWithLifecycle()

    // Mis Correcciones (historial)
    val myCorrections by viewModel.myCorrections.collectAsStateWithLifecycle()
    val myCorrectionsIsLoadingMore by viewModel.myCorrectionsIsLoadingMore.collectAsStateWithLifecycle()
    val myCorrectionsHasMore by viewModel.myCorrectionsHasMore.collectAsStateWithLifecycle()

    // Estados compartidos
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var selectedReview by remember { mutableStateOf<PendingReviewDto?>(null) }
    val listState = rememberLazyListState()
    val historyListState = rememberLazyListState()

    // Cargar datos según tab seleccionado
    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> viewModel.loadPendingReviews()
            1 -> viewModel.loadMyCorrections()
        }
    }

    // Scroll infinito para tab Pendientes
    LaunchedEffect(listState, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            snapshotFlow { 
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 3
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && hasMore && !isLoadingMore && !isLoading) {
                    viewModel.loadMorePendingReviews()
                }
            }
        }
    }

    // Scroll infinito para tab Mis Correcciones
    LaunchedEffect(historyListState, selectedTabIndex) {
        if (selectedTabIndex == 1) {
            snapshotFlow { 
                val layoutInfo = historyListState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 3
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && myCorrectionsHasMore && !myCorrectionsIsLoadingMore && !isLoading) {
                    viewModel.loadMoreMyCorrections()
                }
            }
        }
    }

    // Mostrar mensajes de éxito
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
                            "Correcciones", 
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
                            if (selectedTabIndex == 0) viewModel.loadPendingReviews()
                            else viewModel.loadMyCorrections()
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
                            0 -> pendingReviews.size
                            1 -> myCorrections.size
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
                    // Tab: Pendientes
                    when {
                        isLoading && pendingReviews.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFF90CAF9)
                            )
                        }
                        error != null && pendingReviews.isEmpty() -> {
                            ErrorState(
                                error = error!!,
                                onRetry = { viewModel.loadPendingReviews() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        pendingReviews.isEmpty() -> {
                            EmptyState(
                                message = "¡Excelente! No hay correcciones pendientes",
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            // Lista con datos
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = pendingReviews,
                                    key = { it.id }
                                ) { review ->
                                    PendingReviewCard(
                                        review = review,
                                        onClick = { selectedReview = review }
                                    )
                                }

                                // Indicador de carga al final
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

                                // Botón cargar más
                                if (hasMore && !isLoadingMore) {
                                    item {
                                        TextButton(
                                            onClick = { viewModel.loadMorePendingReviews() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Cargar más",
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
                    // Tab: Mis Correcciones
                    when {
                        isLoading && myCorrections.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFF90CAF9)
                            )
                        }
                        error != null && myCorrections.isEmpty() -> {
                            ErrorState(
                                error = error!!,
                                onRetry = { viewModel.loadMyCorrections() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        myCorrections.isEmpty() -> {
                            EmptyState(
                                message = "Aún no has enviado correcciones",
                                icon = Icons.Default.History,
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
                                    items = myCorrections,
                                    key = { it.id }
                                ) { correction ->
                                    MyCorrectionCard(correction = correction)
                                }

                                if (myCorrectionsIsLoadingMore) {
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

            // Mensaje de éxito
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

            // Mensaje de error
            if (error != null && pendingReviews.isNotEmpty()) {
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

    // Diálogo de corrección
    selectedReview?.let { review ->
        CorrectionFormDialog(
            review = review,
            viewModel = viewModel,
            onDismiss = { 
                selectedReview = null
                viewModel.clearPhotos()
            }
        )
    }
}

/**
 * Card de revisión pendiente
 */
@Composable
private fun PendingReviewCard(
    review: PendingReviewDto,
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
            // Categoría (si existe)
            review.auditItemCategory?.let { category ->
                Text(
                    text = category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF90CAF9).copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Título del item
            Text(
                text = review.auditItemTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tienda
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${review.storeName} (${review.storeCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0BEC5)
                )
            }

            // Nombre del checklist (si existe)
            review.checklistTemplateName?.let { templateName ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = templateName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Notas del auditor (si existen)
            review.auditNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1F2B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB74D),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info del auditor y fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = review.createdByUserName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5)
                    )
                }
                Text(
                    text = formatRelativeTime(review.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón corregir
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF90CAF9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Corregir",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Diálogo de formulario de corrección
 */
@Composable
private fun CorrectionFormDialog(
    review: PendingReviewDto,
    viewModel: AuditReviewViewModel,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }
    val selectedPhotos by viewModel.selectedPhotos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher para seleccionar fotos de galería
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            uriToFile(context, uri)?.let { file ->
                viewModel.addPhoto(file)
            }
        }
    }

    // URI temporal para la foto de cámara
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Launcher para tomar foto con cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            uriToFile(context, cameraPhotoUri!!)?.let { file ->
                viewModel.addPhoto(file)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = Color(0xFF1A1F2B),
        titleContentColor = Color(0xFF90CAF9),
        textContentColor = Color.White,
        title = {
            Column {
                Text(
                    "Corregir Item",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    review.auditItemTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0BEC5)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Notas originales del auditor
                review.auditNotes?.takeIf { it.isNotBlank() }?.let { originalNotes ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF232A36)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Notas del auditor:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB74D)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                originalNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Campo de notas de corrección
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas de corrección (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFF90CAF9).copy(alpha = 0.5f),
                        focusedBorderColor = Color(0xFF90CAF9),
                        unfocusedLabelColor = Color(0xFFB0BEC5),
                        focusedLabelColor = Color(0xFF90CAF9),
                        cursorColor = Color(0xFF90CAF9),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botones para agregar fotos (Galería y Cámara)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Galería
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF232A36)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9).copy(alpha = 0.5f)),
                        enabled = selectedPhotos.size < 10
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Galería",
                            color = Color(0xFF90CAF9),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Botón Cámara
                    Button(
                        onClick = {
                            val photoFile = File.createTempFile(
                                "camera_photo_${System.currentTimeMillis()}",
                                ".jpg",
                                context.cacheDir
                            )
                            cameraPhotoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                photoFile
                            )
                            cameraLauncher.launch(cameraPhotoUri!!)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF232A36)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                        enabled = selectedPhotos.size < 10
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Cámara",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contador de fotos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPhotos.size} foto${if (selectedPhotos.size != 1) "s" else ""} seleccionada${if (selectedPhotos.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedPhotos.isEmpty()) Color(0xFFEF5350) else Color(0xFF90CAF9)
                    )
                    if (selectedPhotos.size >= 10) {
                        Text(
                            "Máximo alcanzado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D)
                        )
                    }
                }

                // Mensaje de foto requerida
                if (selectedPhotos.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Mínimo 1 foto requerida",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF5350)
                        )
                    }
                }

                // Preview de fotos seleccionadas
                if (selectedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedPhotos) { file ->
                            PhotoThumbnail(
                                file = file,
                                onRemove = { viewModel.removePhoto(file) }
                            )
                        }
                    }
                }

                // Error
                error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitCorrection(
                        reviewId = review.id,
                        notes = notes.trim().takeIf { it.isNotEmpty() },
                        onSuccess = onDismiss
                    )
                },
                enabled = selectedPhotos.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isLoading) "Enviando..." else "Enviar Corrección",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar", color = Color(0xFFB0BEC5))
            }
        }
    )
}

/**
 * Thumbnail de foto con botón eliminar
 */
@Composable
private fun PhotoThumbnail(
    file: File,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.size(80.dp)
    ) {
        AsyncImage(
            model = file,
            contentDescription = "Foto seleccionada",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        // Botón eliminar
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color(0xFFEF5350), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Eliminar",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Estado vacío
 */
@Composable
private fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
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
private fun ErrorState(
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
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF90CAF9)
            )
        ) {
            Text("Reintentar", color = Color.Black)
        }
    }
}

// ============================================
// HELPERS
// ============================================

private fun formatDate(isoDate: String): String {
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

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "correction_photo_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        android.util.Log.e("CorrectionsScreen", "Error converting URI to File", e)
        null
    }
}

/**
 * Card para mostrar una corrección del supervisor (historial)
 */
@Composable
private fun MyCorrectionCard(
    correction: MyCorrectionDto
) {
    // Determinar color y texto del badge según status
    val (badgeText, badgeColor) = when (correction.status) {
        "VALIDATED", "APPROVED" -> "APROBADO" to Color(0xFF4CAF50)
        "REJECTED" -> "RECHAZADO" to Color(0xFFEF5350)
        else -> "EN REVISIÓN" to Color(0xFFFFB74D) // CORRECTED = pendiente de validar
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF232A36)
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(1.5.dp, Color(0xFF90CAF9).copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Título y Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = correction.auditItemTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90CAF9),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Badge de estado
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

            // Checklist template
            correction.checklistTemplateName?.let { templateName ->
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

            // Notas de corrección
            correction.correctionNotes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fecha y fotos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fecha
                Text(
                    text = formatRelativeTime(correction.correctedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5)
                )

                // Contador de fotos
                if (correction.attachmentsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${correction.attachmentsCount} foto(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formatea fecha de corrección
 */
private fun formatCorrectionDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString.take(16).replace("T", " ")
    }
}
