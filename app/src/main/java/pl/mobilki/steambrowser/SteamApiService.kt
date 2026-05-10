package pl.mobilki.steambrowser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface SteamApiService {
    suspend fun getMostPlayedGames(apiKey: String): JsonObject
    suspend fun getCurrentPlayers(appId: Int): JsonObject
}

class DefaultSteamApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : SteamApiService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getMostPlayedGames(apiKey: String): JsonObject {
        val inputJson = """
            {
              "context": {"language": "polish", "country_code": "PL"},
              "data_request": {"include_basic_info": true}
            }
        """.trimIndent()

        val url = "https://api.steampowered.com/ISteamChartsService/GetMostPlayedGames/v1/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("input_json", inputJson)
            .build()

        return getJson(url.toString())
    }

    override suspend fun getCurrentPlayers(appId: Int): JsonObject {
        val url = "https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("appid", appId.toString())
            .build()

        return getJson(url.toString())
    }

    private suspend fun getJson(url: String): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw SteamApiException("Steam API zwróciło kod ${it.code}.")
            }
            if (body.isBlank()) {
                throw IOException("Steam API zwróciło pustą odpowiedź.")
            }
            json.parseToJsonElement(body).jsonObject
        }
    }
}

class SteamApiException(message: String) : IOException(message)
