package mx.checklist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel

@Composable
fun HomeScreen(
    vm: RunsViewModel,
    authVM: AuthViewModel? = null,
    onNuevaCorrida: () -> Unit,
    onOpenHistory: () -> Unit,
    onAdminAccess: (() -> Unit)? = null,
    onLogout: () -> Unit = {}
) {
    // Obtener el estado de autenticación para mostrar el mensaje de bienvenida
    val authState = authVM?.state?.collectAsStateWithLifecycle()?.value

    // Limpiar el mensaje de bienvenida después de 3 segundos
    LaunchedEffect(authState?.welcomeMessage) {
        if (authState?.welcomeMessage != null) {
            kotlinx.coroutines.delay(3000)
            authVM?.clearWelcomeMessage()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black // Fondo negro sólido
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "CHECKLIST YEPAS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sistema de Gestión",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeCardPremium(
                    title = "Ejecutar\nChecklist",
                    icon = Icons.Default.PlayArrow,
                    onClick = onNuevaCorrida,
                    modifier = Modifier.weight(1f)
                )
                HomeCardPremium(
                    title = "Historial",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            // Divisor visual
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.8f),
                thickness = 1.5.dp,
                color = Color(0xFF90CAF9).copy(alpha = 0.22f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Botón Administración premium
            onAdminAccess?.let { adminCallback ->
                Card(
                    onClick = adminCallback,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF263040)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 16.dp,
                        pressedElevation = 22.dp
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFF90CAF9).copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Administración",
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Administración",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            // Botón de logout premium
            authVM?.let {
                Button(
                    onClick = {
                        vm.clearCache()
                        authVM.logout(onLogout)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF181C22),
                        contentColor = Color(0xFF90CAF9)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        "Cerrar sesión",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun HomeCardPremium(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF232A36),
            contentColor = Color(0xFF90CAF9)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 18.dp,
            pressedElevation = 24.dp,
            hoveredElevation = 20.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF90CAF9)
            )
        }
    }
}
