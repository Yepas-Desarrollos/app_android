package mx.checklist.data.repo

import mx.checklist.data.api.Api
import mx.checklist.data.api.dto.LoginReq // Asegúrate de importar el DTO correcto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Los repositorios suelen ser Singletons
class AuthRepository @Inject constructor(private val api: Api) {

    suspend fun login(email: String, password: String) {
        // Llama al método login de la interfaz Api.
        // La interfaz Api debería manejar la llamada de red y devolver una respuesta o lanzar una excepción.
        // El ViewModel ya maneja onSuccess/onError basado en esto.
        api.login(LoginReq(email = email, password = password))
    }

    // ... otros métodos del repositorio que puedas tener ...
}
