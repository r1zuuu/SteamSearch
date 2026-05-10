package pl.mobilki.steambrowser

import pl.mobilki.steambrowser.data.model.GameSummary
import pl.mobilki.steambrowser.data.remote.SteamApiService
import pl.mobilki.steambrowser.data.repository.SteamRepository
import pl.mobilki.steambrowser.data.repository.toPolishMessage
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

    @Test
    fun getAllGamesReturnsListOnSuccess() = runBlocking {
        val appListJson = """
            {
                "applist": {
                    "apps": [
                        {"appid": 1, "name": "Game 1"},
                        {"appid": 2, "name": "Game 2"}
                    ]
                }
            }
        """.trimIndent()
        val repository = SteamRepository(FakeSteamApiService(appListJson = appListJson))

        val result = repository.getAllGames()

        assertTrue(result.isSuccess)
        val games = result.getOrThrow()
        assertEquals(2, games.size)
        assertEquals("Game 1", games[0].name)
        assertEquals(1, games[0].appId)
    }

    private inner class FakeSteamApiService(
        private val mostPlayedJson: String = """{"response":{"ranks":[]}}""",
        private val currentPlayersJson: String = """{"response":{"player_count":0}}""",
        private val appListJson: String = """{"applist":{"apps":[]}}"""
    ) : SteamApiService {
        override suspend fun getMostPlayedGames(apiKey: String): JsonObject =
            json.parseToJsonElement(mostPlayedJson).jsonObject

        override suspend fun getCurrentPlayers(appId: Int): JsonObject =
            json.parseToJsonElement(currentPlayersJson).jsonObject

        override suspend fun getAppList(): JsonObject =
            json.parseToJsonElement(appListJson).jsonObject
    }
}
