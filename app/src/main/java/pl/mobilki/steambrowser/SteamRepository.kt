package pl.mobilki.steambrowser

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

class SteamRepository(
    private val api: SteamApiService
) {
    suspend fun getPopularGames(apiKey: String): Result<List<GameSummary>> = runCatching {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Brak klucza Steam API. Dodaj STEAM_API_KEY w pliku local.properties.")
        }

        val response = api.getMostPlayedGames(apiKey)
        val parsedGames = response.extractGameObjects()
            .mapNotNull { it.toGameSummary() }
            .distinctBy { it.appId }
            .take(25)

        if (parsedGames.isEmpty()) {
            throw IllegalStateException("Nie udało się odczytać listy popularnych gier ze Steam.")
        }

        coroutineScope {
            parsedGames.map { game ->
                async {
                    if (game.currentPlayers != null) {
                        game
                    } else {
                        game.copy(currentPlayers = getCurrentPlayers(game.appId).getOrNull())
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getCurrentPlayers(appId: Int): Result<Int> = runCatching {
        val response = api.getCurrentPlayers(appId)
        response.findInt("player_count")
            ?: response.findInt("current_players")
            ?: response.findInt("concurrent_in_game")
            ?: throw IllegalStateException("Brak danych o aktualnej liczbie graczy.")
    }

    private fun JsonObject.toGameSummary(): GameSummary? {
        val appId = findInt("appid")
            ?: findInt("app_id")
            ?: findInt("steam_appid")
            ?: return null

        val name = findString("name")
            ?: findString("title")
            ?: findString("app_name")
            ?: "Gra #$appId"

        val players = findInt("concurrent_in_game")
            ?: findInt("current_players")
            ?: findInt("player_count")
            ?: findInt("players")

        return GameSummary(
            appId = appId,
            name = name,
            currentPlayers = players,
            isFavorite = false
        )
    }
}

fun Throwable.toPolishMessage(): String = when (this) {
    is IllegalStateException -> message ?: "Wystąpił błąd konfiguracji."
    is SteamApiException -> message ?: "Steam API zwróciło błąd."
    else -> message ?: "Nie udało się pobrać danych ze Steam. Sprawdź internet i spróbuj ponownie."
}

private fun JsonObject.extractGameObjects(): List<JsonObject> {
    val directCandidates = listOf("ranks", "items", "apps", "results", "games")
        .flatMap { key ->
            (this[key] as? JsonArray).orEmpty().mapNotNull { it.asJsonObjectOrNull() }
        }

    if (directCandidates.isNotEmpty()) {
        return directCandidates
    }

    val collected = mutableListOf<JsonObject>()
    fun visit(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                if (element.findInt("appid") != null || element.findInt("app_id") != null) {
                    collected += element
                }
                element.values.forEach(::visit)
            }
            is JsonArray -> element.forEach(::visit)
            else -> Unit
        }
    }
    visit(this)
    return collected
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

private fun JsonObject.findInt(key: String): Int? {
    this[key]?.asIntOrNull()?.let { return it }
    values.forEach { child ->
        when (child) {
            is JsonObject -> child.findInt(key)?.let { return it }
            is JsonArray -> child.forEach { item ->
                item.asJsonObjectOrNull()?.findInt(key)?.let { return it }
            }
            else -> Unit
        }
    }
    return null
}

private fun JsonObject.findString(key: String): String? {
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
    values.forEach { child ->
        when (child) {
            is JsonObject -> child.findString(key)?.let { return it }
            is JsonArray -> child.forEach { item ->
                item.asJsonObjectOrNull()?.findString(key)?.let { return it }
            }
            else -> Unit
        }
    }
    return null
}

private fun JsonElement.asIntOrNull(): Int? = when (this) {
    is JsonPrimitive -> intOrNull ?: contentOrNull?.toIntOrNull()
    else -> null
}
