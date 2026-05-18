package pl.mobilki.steambrowser

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val cacheJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class CachedGame(val appId: Int, val name: String, val currentPlayers: Int?)

class SearchCacheRepository(context: Context) {
    private val prefs = context.getSharedPreferences("search_cache", Context.MODE_PRIVATE)
    private val key = "recent_games"
    private val maxCached = 50

    fun save(games: List<GameSummary>) {
        val existing = load().associateBy { it.appId }
        val merged = (games.map { CachedGame(it.appId, it.name, it.currentPlayers) } +
            existing.values.map { CachedGame(it.appId, it.name, it.currentPlayers) })
            .distinctBy { it.appId }
            .take(maxCached)
        prefs.edit().putString(key, cacheJson.encodeToString(merged)).apply()
    }

    fun load(): List<GameSummary> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            cacheJson.decodeFromString<List<CachedGame>>(raw)
                .map { GameSummary(it.appId, it.name, it.currentPlayers, false) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
