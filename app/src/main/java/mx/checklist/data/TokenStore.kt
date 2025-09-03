package mx.checklist.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

class TokenStore(ctx: Context) {
  private val store = ctx.dataStore
  private val KEY = stringPreferencesKey("jwt")

  val tokenFlow = store.data.map { it[KEY] }

  suspend fun saveToken(token: String) {
    store.edit { it[KEY] = token }
  }

  suspend fun clear() {
    store.edit { it.remove(KEY) }
  }
}
