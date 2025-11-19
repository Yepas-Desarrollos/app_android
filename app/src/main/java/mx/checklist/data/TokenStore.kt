package mx.checklist.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mx.checklist.data.auth.Authenticated

class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow

    private val _roleCodeFlow = MutableStateFlow<String?>(null)
    val roleCodeFlow: StateFlow<String?> = _roleCodeFlow

    init {
        // Cargar datos guardados
        val savedToken = encryptedPrefs.getString("jwt", null)
        _tokenFlow.value = savedToken

        val savedRoleCode = encryptedPrefs.getString("roleCode", null)
        _roleCodeFlow.value = savedRoleCode
    }

    suspend fun saveToken(token: String) {
        encryptedPrefs.edit().putString("jwt", token).apply()
        _tokenFlow.value = token
    }

    suspend fun saveRoleCode(roleCode: String?) {
        val editor = encryptedPrefs.edit()
        if (roleCode != null) {
            editor.putString("roleCode", roleCode)
        } else {
            editor.remove("roleCode")
        }
        editor.apply()
        _roleCodeFlow.value = roleCode
    }

    suspend fun save(auth: Authenticated) {
        encryptedPrefs.edit().apply {
            putString("jwt", auth.token)
            if (auth.roleCode != null) {
                putString("roleCode", auth.roleCode)
            } else {
                remove("roleCode")
            }
        }.apply()

        _tokenFlow.value = auth.token
        _roleCodeFlow.value = auth.roleCode
    }

    suspend fun clear() {
        encryptedPrefs.edit().clear().apply()
        _tokenFlow.value = null
        _roleCodeFlow.value = null
    }
}
