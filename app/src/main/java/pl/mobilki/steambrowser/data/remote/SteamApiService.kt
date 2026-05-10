package pl.mobilki.steambrowser.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface SteamApiService {
    suspend fun getMostPlayedGames(apiKey: String): JsonObject
    suspend fun getCurrentPlayers(appId: Int): JsonObject
    suspend fun getAppList(): JsonObject
    suspend fun getAppDetails(appId: Int): JsonObject
    suspend fun searchStore(term: String): JsonObject
}

class DefaultSteamApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : SteamApiService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getMostPlayedGames(apiKey: String): JsonObject {
        val inputJson = """{"context":{"language":"polish","country_code":"PL"},"data_request":{"include_basic_info":true}}"""
        val url = "https://api.steampowered.com/ISteamChartsService/GetMostPlayedGames/v1/".toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("input_json", inputJson)
            .build()
        return getJson(url)
    }

    override suspend fun getCurrentPlayers(appId: Int): JsonObject {
        val url = "https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/".toHttpUrl().newBuilder()
            .addQueryParameter("appid", appId.toString())
            .build()
        return getJson(url)
    }

    override suspend fun getAppList(): JsonObject {
        // Poprawiony URL z IStoreService (404) na ISteamApps (poprawny)
        val url = "https://api.steampowered.com/ISteamApps/GetAppList/v2/".toHttpUrl()
        return getJson(url)
    }

    override suspend fun getAppDetails(appId: Int): JsonObject {
        val url = "https://store.steampowered.com/api/appdetails".toHttpUrl().newBuilder()
            .addQueryParameter("appids", appId.toString())
            .addQueryParameter("l", "polish")
            .build()
        return getJson(url, useBrowserAgent = true)
    }

    override suspend fun searchStore(term: String): JsonObject {
        // API: https://store.steampowered.com/api/storesearch/?term={term}&l=polish&cc=PL
        val url = "https://store.steampowered.com/api/storesearch/".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("l", "polish")
            .addQueryParameter("cc", "PL")
            .build()
        return getJson(url, useBrowserAgent = true)
    }

    private suspend fun getJson(url: HttpUrl, useBrowserAgent: Boolean = false): JsonObject = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
        
        if (useBrowserAgent) {
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        } else {
            requestBuilder.header("User-Agent", "SteamBrowser/1.0 (Android)")
        }
        
        val response = client.newCall(requestBuilder.build()).execute()
        response.use {
            if (!it.isSuccessful) {
                val errorMsg = when (it.code) {
                    429 -> "Zbyt wiele zapytań (Rate Limit). Spróbuj ponownie za chwilę."
                    403 -> "Brak dostępu (403). Store API wymaga User-Agenta."
                    404 -> "Nie znaleziono zasobu (404)."
                    else -> "Błąd API Steam (Kod: ${it.code})."
                }
                throw SteamApiException("$errorMsg [${url.encodedPath}]")
            }
            val body = it.body?.string() ?: throw IOException("Pusta odpowiedź z API")
            json.parseToJsonElement(body).jsonObject
        }
    }
}

class SteamApiException(message: String) : IOException(message)
