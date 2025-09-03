package mx.checklist.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("auth_prefs")

class TokenStore(private val ctx: Context) {
  private val KEY_TOKEN = stringPreferencesKey("token")

  val tokenFlow = ctx.dataStore.data.map { it[KEY_TOKEN] }

  suspend fun saveToken(token: String) {
    ctx.dataStore.edit { it[KEY_TOKEN] = token }
  }

  suspend fun clear() {
    ctx.dataStore.edit { it.remove(KEY_TOKEN) }
  }
}
