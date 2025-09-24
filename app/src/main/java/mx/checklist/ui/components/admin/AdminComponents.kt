package mx.checklist.ui.components.admin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.checklist.data.auth.AuthState

@Composable
fun AdminAccessButton(
    onAdminAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Verificar si el usuario tiene permisos de administración (ADMIN o MANAGER)
    val isAdmin = AuthState.roleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")
    
    Log.d("AdminComponents", "🎯 AdminAccessButton - AuthState.roleCode: '${AuthState.roleCode}', isAdmin: $isAdmin")
    
    if (isAdmin) {
        val buttonText = when (AuthState.roleCode) {
            "ADMIN" -> "Panel Administrador"
            "MGR_PREV" -> "Panel Prevención"
            "MGR_OPS" -> "Panel Operaciones"
            else -> "Panel Admin"
        }
        
        FilledTonalButton(
            onClick = onAdminAccess,
            modifier = modifier,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = buttonText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
    }
}

@Composable
fun AdminOnlyContent(
    content: @Composable () -> Unit
) {
    val hasAdminPermissions = AuthState.roleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")
    
    if (hasAdminPermissions) {
        content()
    }
}

@Composable
fun AdminBadge() {
    val hasAdminPermissions = AuthState.roleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")
    
    if (hasAdminPermissions) {
        val badgeText = when (AuthState.roleCode) {
            "ADMIN" -> "ADMIN"
            "MGR_PREV" -> "PREV"
            "MGR_OPS" -> "OPS"
            else -> "ADM"
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}