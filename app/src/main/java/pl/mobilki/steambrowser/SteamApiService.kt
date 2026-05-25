package pl.mobilki.steambrowser

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface SteamApiService {
    suspend fun getMostPlayedGames(apiKey: String): JsonObject
    suspend fun getCurrentPlayers(appId: Int): JsonObject
    suspend fun searchGames(query: String): JsonObject
    suspend fun getAppDetails(appId: Int): JsonObject
    suspend fun getFeaturedCategories(): JsonObject
    suspend fun getSteamReviews(appId: Int, filter: String, num: Int): JsonObject
    suspend fun getGameFullDetails(appId: Int): JsonObject
    suspend fun verifyOpenIdResponse(responseUrl: String): Boolean
    suspend fun getPlayerSummaries(apiKey: String, steamIds: String): JsonObject
    suspend fun getOwnedGames(apiKey: String, steamId: String): JsonObject
    suspend fun getPlayerAchievements(apiKey: String, steamId: String, appId: Int): JsonObject
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

    override suspend fun searchGames(query: String): JsonObject {
        val url = "https://store.steampowered.com/api/storesearch/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("cc", "PL")
            .addQueryParameter("l", "polish")
            .addQueryParameter("count", "15")
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

    override suspend fun getAppDetails(appId: Int): JsonObject {
        val url = "https://store.steampowered.com/api/appdetails"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("appids", appId.toString())
            .addQueryParameter("cc", "PL")
            .addQueryParameter("filters", "price_overview")
            .build()
        return getJson(url.toString())
    }

    override suspend fun getFeaturedCategories(): JsonObject {
        val url = "https://store.steampowered.com/api/featuredcategories/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("cc", "PL")
            .addQueryParameter("l", "polish")
            .build()
        return getJson(url.toString())
    }

    override suspend fun getGameFullDetails(appId: Int): JsonObject {
        val url = "https://store.steampowered.com/api/appdetails"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("appids", appId.toString())
            .addQueryParameter("cc", "PL")
            .addQueryParameter("l", "polish")
            .addQueryParameter("filters", "basic,genres")
            .build()
        return getJson(url.toString())
    }

    override suspend fun getSteamReviews(appId: Int, filter: String, num: Int): JsonObject {
        val url = "https://store.steampowered.com/appreviews/$appId"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("json", "1")
            .addQueryParameter("filter", filter)
            .addQueryParameter("language", "all")
            .addQueryParameter("review_type", "all")
            .addQueryParameter("purchase_type", "all")
            .addQueryParameter("num_per_page", num.toString())
            .build()
        return getJson(url.toString())
    }

    override suspend fun verifyOpenIdResponse(responseUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(responseUrl)
            val formBuilder = FormBody.Builder()
            for (name in uri.queryParameterNames) {
                val value = if (name == "openid.mode") {
                    "check_authentication"
                } else {
                    uri.getQueryParameter(name).orEmpty()
                }
                formBuilder.add(name, value)
            }
            val request = Request.Builder()
                .url("https://steamcommunity.com/openid/login")
                .post(formBuilder.build())
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                response.isSuccessful && body.contains("is_valid:true")
            }
        }.getOrDefault(false)
    }

    override suspend fun getPlayerSummaries(apiKey: String, steamIds: String): JsonObject {
        val url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("steamids", steamIds)
            .build()
        return getJson(url.toString())
    }

    override suspend fun getOwnedGames(apiKey: String, steamId: String): JsonObject {
        val url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("steamid", steamId)
            .addQueryParameter("include_appinfo", "true")
            .addQueryParameter("format", "json")
            .build()
        return getJson(url.toString())
    }

    override suspend fun getPlayerAchievements(apiKey: String, steamId: String, appId: Int): JsonObject {
        val url = "https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("steamid", steamId)
            .addQueryParameter("appid", appId.toString())
            .addQueryParameter("l", "polish")
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
