package mx.checklist.utils

import android.util.Log
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Contexto de la operación para interpretar errores correctamente
 */
enum class ErrorContext {
    LOGIN,           // Durante login
    AUTHENTICATED    // Durante operaciones autenticadas
}

/**
 * Mapea excepciones a errores tipificados con mensajes claros
 */
object ErrorMapper {
    
    fun fromThrowable(
        throwable: Throwable,
        context: ErrorContext = ErrorContext.AUTHENTICATED
    ): AppError {
        Log.e("ErrorMapper", "Mapping error: ${throwable.javaClass.simpleName}", throwable)
        
        return when (throwable) {
            is HttpException -> mapHttpException(throwable, context)
            is UnknownHostException -> AppError.NoInternet()
            is SocketTimeoutException -> AppError.Timeout()
            is IOException -> AppError.NoInternet("Problema de conexión. Verifica tu internet")
            else -> AppError.Unknown(throwable.message ?: "Error desconocido")
        }
    }
    
    private fun mapHttpException(
        exception: HttpException,
        context: ErrorContext
    ): AppError {
        val errorBody = try {
            exception.response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        
        val serverMessage = parseServerMessage(errorBody)
        
        return when (exception.code()) {
            400 -> {
                // Bad Request - puede ser credenciales inválidas o validación
                when {
                    serverMessage?.contains("credentials", ignoreCase = true) == true ||
                    serverMessage?.contains("password", ignoreCase = true) == true ||
                    serverMessage?.contains("contraseña", ignoreCase = true) == true ||
                    serverMessage?.contains("usuario", ignoreCase = true) == true ||
                    serverMessage?.contains("email", ignoreCase = true) == true ->
                        AppError.InvalidCredentials()
                    
                    serverMessage?.contains("locked", ignoreCase = true) == true ||
                    serverMessage?.contains("bloqueada", ignoreCase = true) == true ->
                        AppError.AccountLocked()
                    
                    else -> AppError.ValidationError("", serverMessage ?: "Datos inválidos")
                }
            }
            
            401 -> {
                // IMPORTANTE: 401 tiene diferente significado según el contexto
                when (context) {
                    ErrorContext.LOGIN -> 
                        // Durante login: credenciales incorrectas
                        AppError.InvalidCredentials()
                    
                    ErrorContext.AUTHENTICATED -> 
                        // Durante operaciones: sesión expirada
                        AppError.SessionExpired()
                }
            }
            
            403 -> AppError.Unauthorized()
            
            404 -> AppError.NotFound(serverMessage ?: "Recurso no encontrado")
            
            409 -> AppError.Conflict(serverMessage ?: "El recurso ya existe")
            
            413 -> AppError.FileTooLarge()
            
            422 -> AppError.ValidationError("", serverMessage ?: "Datos inválidos")
            
            in 500..599 -> AppError.ServerError(
                code = exception.code(),
                message = "Error del servidor. Intenta más tarde"
            )
            
            else -> AppError.Unknown("Error HTTP ${exception.code()}: ${serverMessage ?: exception.message()}")
        }
    }
    
    /**
     * Intenta extraer el mensaje del servidor desde el JSON de error
     * Formatos soportados:
     * - {"message": "..."}
     * - {"error": "..."}
     * - {"msg": "..."}
     */
    private fun parseServerMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        
        return try {
            // Intentar parsear diferentes formatos de JSON
            val patterns = listOf(
                """"message"\s*:\s*"([^"]+)"""",
                """"error"\s*:\s*"([^"]+)"""",
                """"msg"\s*:\s*"([^"]+)""""
            )
            
            for (pattern in patterns) {
                val match = pattern.toRegex().find(errorBody)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("ErrorMapper", "Error parsing server message", e)
            null
        }
    }
}
