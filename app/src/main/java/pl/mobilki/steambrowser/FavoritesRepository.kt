package pl.mobilki.steambrowser

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "steam_favorites")

class FavoritesRepository(private val context: Context) {
    private val favoritesKey = stringSetPreferencesKey("favorite_app_ids")

    val favoritesFlow: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[favoritesKey]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun save(favorites: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[favoritesKey] = favorites.map { it.toString() }.toSet()
        }
    }
}
