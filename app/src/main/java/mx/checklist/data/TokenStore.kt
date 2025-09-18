package mx.checklist.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import mx.checklist.data.auth.Authenticated

private val Context.dataStore by preferencesDataStore(name = "auth")

class TokenStore(ctx: Context) {
  private val store = ctx.dataStore
  private val TOKEN_KEY = stringPreferencesKey("jwt")
  private val ROLE_KEY = stringPreferencesKey("roleCode")

  val tokenFlow = store.data.map { it[TOKEN_KEY] }
  val roleCodeFlow = store.data.map { it[ROLE_KEY] }

  suspend fun saveToken(token: String) {
    store.edit { it[TOKEN_KEY] = token }
  }

  suspend fun saveRoleCode(roleCode: String?) {
    store.edit {
      if (roleCode != null) {
        it[ROLE_KEY] = roleCode
      } else {
        it.remove(ROLE_KEY)
      }
    }
  }

  suspend fun save(auth: Authenticated) {
    store.edit {
      it[TOKEN_KEY] = auth.token
      if (auth.roleCode != null) {
        it[ROLE_KEY] = auth.roleCode
      } else {
        it.remove(ROLE_KEY)
      }
    }
  }

  suspend fun clear() {
    store.edit {
      it.remove(TOKEN_KEY)
      it.remove(ROLE_KEY)
    }
  }
}
