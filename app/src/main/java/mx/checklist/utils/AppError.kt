package mx.checklist.utils

import android.content.Context
import mx.checklist.R

/**
 * Tipos de errores de la aplicación con mensajes claros para el usuario
 */
sealed class AppError {
    abstract fun toUserMessage(context: Context): String
    
    // === Errores de Autenticación ===
    data class InvalidCredentials(
        val debugMessage: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_invalid_credentials)
    }
    
    data class AccountLocked(
        val debugMessage: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_account_locked)
    }
    
    // === Errores de Red ===
    data class NoInternet(
        val debugMessage: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_no_internet)
    }
    
    data class Timeout(
        val debugMessage: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_timeout)
    }
    
    data class ServerError(
        val code: Int,
        val message: String? = null
    ) : AppError() {
        override fun toUserMessage(context: Context): String {
            return if (!message.isNullOrBlank()) {
                context.getString(R.string.error_server_code, message, code)
            } else {
                context.getString(R.string.error_server)
            }
        }
    }
    
    // === Errores de Sesión ===
    data class SessionExpired(
        val debugMessage: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_session_expired)
    }
    
    data class Unauthorized(
        val debugMessage: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_unauthorized)
    }
    
    // === Errores de Validación ===
    data class ValidationError(
        val field: String,
        val message: String
    ) : AppError() {
        override fun toUserMessage(context: Context) = if (field.isNotEmpty()) {
            "$field: $message"
        } else {
            message
        }
    }
    
    data class MissingEvidence(
        val required: Int,
        val current: Int
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_missing_evidence, required, current)
    }
    
    data class FileTooLarge(
        val maxSizeMB: Int = 10
    ) : AppError() {
        override fun toUserMessage(context: Context) = context.getString(R.string.error_file_too_large, maxSizeMB)
    }
    
    // === Errores de Datos ===
    data class NotFound(
        val resource: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = if (resource.isNotEmpty()) resource else context.getString(R.string.error_not_found)
    }
    
    data class Conflict(
        val message: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = if (message.isNotEmpty()) message else context.getString(R.string.error_conflict)
    }
    
    // === Errores Genéricos ===
    data class Unknown(
        val message: String = ""
    ) : AppError() {
        override fun toUserMessage(context: Context) = if (message.isNotEmpty()) message else context.getString(R.string.error_unknown)
    }
}
