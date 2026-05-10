package pl.mobilki.steambrowser.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import pl.mobilki.steambrowser.data.model.GameDetails
import pl.mobilki.steambrowser.data.model.GameSummary
import pl.mobilki.steambrowser.data.remote.SteamApiService
import pl.mobilki.steambrowser.data.remote.SteamApiException

class SteamRepository(
    private val api: SteamApiService
) {
    private var allAppsCache: List<GameSummary>? = null

    suspend fun getPopularGames(apiKey: String): Result<List<GameSummary>> = runCatching {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Brak klucza Steam API. Dodaj STEAM_API_KEY w pliku local.properties.")
        }

        val response = api.getMostPlayedGames(apiKey)
        val parsedGames = response.extractGameObjects()
            .mapNotNull { it.toGameSummary() }
            .distinctBy { it.appId }
            .take(25)

        coroutineScope {
            parsedGames.mapIndexed { index, game ->
                async {
                    if (game.currentPlayers != null && game.currentPlayers > 0) {
                        game
                    } else {
                        delay(index * 200L)
                        val players = getCurrentPlayers(game.appId).getOrNull()
                        if (players != null && players > 0) game.copy(currentPlayers = players) else game
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getCurrentPlayers(appId: Int): Result<Int> = runCatching {
        val response = api.getCurrentPlayers(appId)
        val data = response["response"]?.asJsonObjectOrNull() ?: response
        data.findInt("player_count")
            ?: data.findInt("current_players") ?: 0
    }

    suspend fun getAllGames(): Result<List<GameSummary>> = runCatching {
        allAppsCache?.let { return@runCatching it }
        
        val response = api.getAppList()
        val apps = response.extractGameObjects()
            .mapNotNull { it.toGameSummary() }
            .filter { it.name.isNotBlank() && !it.name.contains("SteamDB Unknown App", ignoreCase = true) }
            .distinctBy { it.appId }
        
        if (apps.isEmpty()) throw IllegalStateException("Pobrana lista gier jest pusta. Spróbuj odświeżyć później.")
        
        allAppsCache = apps
        apps
    }

    suspend fun searchStore(query: String): Result<List<GameSummary>> = runCatching {
        val response = api.searchStore(query)
        val items = response["items"]?.asJsonArrayOrNull() ?: return@runCatching emptyList()
        
        items.mapNotNull { it.asJsonObjectOrNull()?.toGameSummary() }
    }

    suspend fun getGameDetails(appId: Int): Result<GameDetails> = runCatching {
        val response = api.getAppDetails(appId)
        val appData = response[appId.toString()]?.asJsonObjectOrNull() 
            ?: throw IllegalStateException("Nie znaleziono danych dla gry o ID $appId")
        
        if (appData["success"]?.jsonPrimitive?.booleanOrNull != true) {
            throw IllegalStateException("Steam API nie zwróciło danych dla ID $appId")
        }

        val data = appData["data"]?.asJsonObjectOrNull() ?: throw IllegalStateException("Błędny format danych gry")
        
        GameDetails(
            appId = appId,
            name = data["name"]?.jsonPrimitive?.content ?: "Nieznana gra",
            currentPlayers = null,
            isFavorite = false,
            description = data["short_description"]?.jsonPrimitive?.content ?: "",
            imageUrl = data["header_image"]?.jsonPrimitive?.content ?: "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/${appId}/header.jpg",
            developers = data["developers"]?.asJsonArrayOrNull()?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
            genres = data["genres"]?.asJsonArrayOrNull()?.mapNotNull { it.asJsonObjectOrNull()?.get("description")?.jsonPrimitive?.contentOrNull } ?: emptyList()
        )
    }

    private fun JsonObject.toGameSummary(): GameSummary? {
        val appId = (this["id"] ?: this["appid"] ?: this["app_id"] ?: this["steam_appid"])?.asIntOrNull() ?: return null
        val nameValue = this["name"] ?: this["title"] ?: this["app_name"] ?: 
                        this["item"]?.asJsonObjectOrNull()?.get("name")
        val name = (nameValue as? JsonPrimitive)?.contentOrNull ?: "Gra #$appId"
        
        val players = this["peak_in_game"]?.asIntOrNull() 
            ?: this["rollup_current_players"]?.asIntOrNull()
            ?: this["player_count"]?.asIntOrNull()
            ?: this["current_players"]?.asIntOrNull()
        
        val imageUrl = this["tiny_image"]?.jsonPrimitive?.contentOrNull
            ?: "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/${appId}/header.jpg"

        return GameSummary(
            appId = appId,
            name = name,
            currentPlayers = if (players != null && players > 0) players else null,
            isFavorite = false,
            imageUrl = imageUrl
        )
    }
}

fun Throwable.toPolishMessage(): String = when (this) {
    is IllegalStateException -> message ?: "Błąd konfiguracji."
    is SteamApiException -> message ?: "Błąd API Steam."
    else -> "Błąd połączenia: ${message ?: "nieznany błąd"}"
}

private fun JsonObject.extractGameObjects(): List<JsonObject> {
    this["applist"]?.asJsonObjectOrNull()?.let { appList ->
        val apps = appList["apps"]
        apps?.asJsonArrayOrNull()?.let { return it.mapNotNull { it.asJsonObjectOrNull() } }
        apps?.asJsonObjectOrNull()?.get("app")?.asJsonArrayOrNull()?.let { return it.mapNotNull { it.asJsonObjectOrNull() } }
    }
    
    val resp = this["response"]?.asJsonObjectOrNull() ?: this
    listOf("ranks", "items", "apps", "games", "app").forEach { key ->
        resp[key]?.asJsonArrayOrNull()?.let { return it.mapNotNull { it.asJsonObjectOrNull() } }
    }

    return emptyList()
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()
private fun JsonElement.asJsonArrayOrNull(): JsonArray? = runCatching { jsonArray }.getOrNull()
private fun JsonObject.findInt(key: String): Int? = this[key]?.asIntOrNull()
private fun JsonElement.asIntOrNull(): Int? = when (this) {
    is JsonPrimitive -> intOrNull ?: contentOrNull?.toIntOrNull()
    else -> null
}
