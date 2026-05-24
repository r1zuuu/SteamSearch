package pl.mobilki.steambrowser

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsPopularGamesFromNestedSteamResponse() = runBlocking {
        val repository = SteamRepository(
            FakeSteamApiService(
                mostPlayedJson = """
                    {
                      "response": {
                        "ranks": [
                          {
                            "appid": 730,
                            "concurrent_in_game": 912345,
                            "item": {"name": "Counter-Strike 2"}
                          }
                        ]
                      }
                    }
                """.trimIndent(),
                currentPlayersJson = """{"response":{"player_count":912345}}"""
            )
        )

        val result = repository.getPopularGames("test-key")

        assertTrue(result.isSuccess)
        assertEquals(
            GameSummary(
                appId = 730,
                name = "Counter-Strike 2",
                currentPlayers = 912345,
                isFavorite = false
            ),
            result.getOrThrow().single()
        )
    }

    @Test
    fun reportsMissingApiKey() = runBlocking {
        val repository = SteamRepository(FakeSteamApiService())

        val result = repository.getPopularGames("")

        assertTrue(result.isFailure)
        assertEquals(
            "Brak klucza Steam API. Dodaj STEAM_API_KEY w pliku local.properties.",
            result.exceptionOrNull()?.toPolishMessage()
        )
    }

    private inner class FakeSteamApiService(
        private val mostPlayedJson: String = """{"response":{"ranks":[]}}""",
        private val currentPlayersJson: String = """{"response":{"player_count":0}}"""
    ) : SteamApiService {
        override suspend fun getMostPlayedGames(apiKey: String): JsonObject =
            json.parseToJsonElement(mostPlayedJson).jsonObject

        override suspend fun getCurrentPlayers(appId: Int): JsonObject =
            json.parseToJsonElement(currentPlayersJson).jsonObject

        override suspend fun searchGames(query: String): JsonObject = JsonObject(emptyMap())
        override suspend fun getAppDetails(appId: Int): JsonObject = JsonObject(emptyMap())
        override suspend fun getFeaturedCategories(): JsonObject = JsonObject(emptyMap())
        override suspend fun getSteamReviews(appId: Int, filter: String, num: Int): JsonObject = JsonObject(emptyMap())
        override suspend fun getGameFullDetails(appId: Int): JsonObject = JsonObject(emptyMap())
    }
}
