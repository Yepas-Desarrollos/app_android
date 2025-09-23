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
    // Verificar si el usuario es admin
    val isAdmin = AuthState.roleCode == "ADMIN"
    
    Log.d("AdminComponents", "🎯 AdminAccessButton - AuthState.roleCode: '${AuthState.roleCode}', isAdmin: $isAdmin")
    
    if (isAdmin) {
        FilledTonalButton(
            onClick = onAdminAccess,
            modifier = modifier,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Panel Admin"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Panel Admin")
        }
    }
}

@Composable
fun AdminOnlyContent(
    content: @Composable () -> Unit
) {
    val isAdmin = AuthState.roleCode == "ADMIN"
    
    if (isAdmin) {
        content()
    }
}

@Composable
fun AdminBadge() {
    val isAdmin = AuthState.roleCode == "ADMIN"
    
    if (isAdmin) {
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
                    text = "ADMIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}