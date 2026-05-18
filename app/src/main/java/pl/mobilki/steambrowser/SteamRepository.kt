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
import kotlinx.serialization.json.jsonPrimitive

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
            .take(100)

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

    suspend fun searchGames(query: String): Result<List<GameSummary>> = runCatching {
        val response = api.searchGames(query)
        val items = (response["items"] as? JsonArray)
            ?.mapNotNull { it.asJsonObjectOrNull() }
            ?: throw IllegalStateException("Brak wyników dla zapytania.")

        val games = items.mapNotNull { item ->
            val appId = (item["id"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val name = (item["name"] as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            GameSummary(appId = appId, name = name, currentPlayers = null, isFavorite = false)
        }

        if (games.isEmpty()) throw IllegalStateException("Nie znaleziono gier dla \"$query\".")

        coroutineScope {
            games.map { game ->
                async { game.copy(currentPlayers = getCurrentPlayers(game.appId).getOrNull()) }
            }.awaitAll()
        }
    }

    suspend fun getFeaturedDeals(): Result<List<DealItem>> = runCatching {
        val response = api.getFeaturedCategories()

        val sectionKeys = listOf("specials", "top_sellers", "new_releases", "coming_soon")
        val rawItems = sectionKeys.flatMap { key ->
            (response[key] as? JsonObject)
                ?.let { (it["items"] as? JsonArray)?.mapNotNull { e -> e.asJsonObjectOrNull() } }
                .orEmpty()
        }

        val deals = rawItems.mapNotNull { item ->
            val appId = (item["id"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val name = (item["name"] as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val discount = (item["discount_percent"] as? JsonPrimitive)?.intOrNull ?: 0
            if (discount <= 0) return@mapNotNull null
            val originalCents = (item["original_price"] as? JsonPrimitive)?.intOrNull ?: 0
            val finalCents = (item["final_price"] as? JsonPrimitive)?.intOrNull ?: 0
            val currency = (item["currency"] as? JsonPrimitive)?.contentOrNull ?: "PLN"
            val price = GamePrice(
                currency = currency,
                initialCents = originalCents,
                finalCents = finalCents,
                discountPercent = discount,
                initialFormatted = formatPricePln(originalCents),
                finalFormatted = formatPricePln(finalCents)
            )
            DealItem(appId = appId, name = name, price = price)
        }.distinctBy { it.appId }

        if (deals.isEmpty()) throw IllegalStateException("Brak aktualnych promocji.")

        coroutineScope {
            deals.map { deal ->
                async {
                    val players = getCurrentPlayers(deal.appId).getOrNull()
                    val score = (deal.price?.discountPercent ?: 0) *
                        kotlin.math.log10((players ?: 0).coerceAtLeast(100).toDouble() + 1)
                    deal.copy(currentPlayers = players, dealScore = score)
                }
            }.awaitAll()
        }.sortedByDescending { it.dealScore }
    }

    suspend fun getGamePrice(appId: Int): Result<GamePrice?> = runCatching {
        val response = api.getAppDetails(appId)
        val gameData = (response[appId.toString()] as? JsonObject) ?: return@runCatching null
        val success = (gameData["success"] as? JsonPrimitive)?.content == "true"
        if (!success) return@runCatching null
        val data = (gameData["data"] as? JsonObject) ?: return@runCatching null
        val priceOverview = (data["price_overview"] as? JsonObject) ?: return@runCatching null
        GamePrice(
            currency = (priceOverview["currency"] as? JsonPrimitive)?.contentOrNull ?: "PLN",
            initialCents = (priceOverview["initial"] as? JsonPrimitive)?.intOrNull ?: 0,
            finalCents = (priceOverview["final"] as? JsonPrimitive)?.intOrNull ?: 0,
            discountPercent = (priceOverview["discount_percent"] as? JsonPrimitive)?.intOrNull ?: 0,
            initialFormatted = (priceOverview["initial_formatted"] as? JsonPrimitive)?.contentOrNull ?: "",
            finalFormatted = (priceOverview["final_formatted"] as? JsonPrimitive)?.contentOrNull ?: ""
        )
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

private fun formatPricePln(cents: Int): String {
    val zloty = cents / 100
    val grosze = cents % 100
    return "$zloty,${"%02d".format(grosze)} zł"
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
