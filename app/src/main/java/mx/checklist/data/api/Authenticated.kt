package mx.checklist.data.api

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Anotación para marcar que una petición de Retrofit requiere autenticación.
 * Un interceptor de OkHttp puede buscar esta anotación para añadir
 * el token de autorización necesario.
 */
@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Authenticated
