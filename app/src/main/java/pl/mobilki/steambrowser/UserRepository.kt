package pl.mobilki.steambrowser

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "steam_user")

class UserRepository(private val context: Context) {
    private val steamIdKey = stringPreferencesKey("steam_id_64")

    val steamIdFlow: Flow<String?> = context.userDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[steamIdKey] }

    suspend fun saveSteamId(steamId: String) {
        runCatching {
            context.userDataStore.edit { prefs ->
                prefs[steamIdKey] = steamId
            }
        }
    }

    suspend fun clearSteamId() {
        runCatching {
            context.userDataStore.edit { prefs ->
                prefs.remove(steamIdKey)
            }
        }
    }
}
