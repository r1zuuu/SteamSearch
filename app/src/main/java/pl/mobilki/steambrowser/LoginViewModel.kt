package pl.mobilki.steambrowser

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(
        val steamId: String,
        val personaName: String,
        val ownedGames: List<OwnedGame> = emptyList()
    ) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val steamApiService: SteamApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val savedSteamId: StateFlow<String?> = userRepository.steamIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val savedPersonaName: StateFlow<String?> = userRepository.personaNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            savedSteamId.collect { steamId ->
                if (!steamId.isNullOrBlank() && _uiState.value is LoginUiState.Idle) {
                    _uiState.value = LoginUiState.Loading
                    fetchUserData(steamId)
                }
            }
        }
    }

    fun buildLoginUrl(): String {
        val returnTo = "https://steambrowser.mobilki.pl/login"
        val realm = "https://steambrowser.mobilki.pl"
        return Uri.parse("https://steamcommunity.com/openid/login")
            .buildUpon()
            .appendQueryParameter("openid.ns", "http://specs.openid.net/auth/2.0")
            .appendQueryParameter("openid.mode", "checkid_setup")
            .appendQueryParameter("openid.return_to", returnTo)
            .appendQueryParameter("openid.realm", realm)
            .appendQueryParameter(
                "openid.identity",
                "http://specs.openid.net/auth/2.0/identifier_select"
            )
            .appendQueryParameter(
                "openid.claimed_id",
                "http://specs.openid.net/auth/2.0/identifier_select"
            )
            .build()
            .toString()
    }

    fun verifySteamLogin(uriString: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            runCatching {
                val claimedId = Uri.parse(uriString).getQueryParameter("openid.claimed_id")
                    ?: throw IllegalArgumentException("Brak openid.claimed_id w odpowiedzi.")
                val steamId = claimedId.substringAfterLast('/')
                if (steamId.isBlank() || steamId.any { !it.isDigit() }) {
                    throw IllegalArgumentException("Nieprawidłowy SteamID64.")
                }
                val isValid = steamApiService.verifyOpenIdResponse(uriString)
                if (!isValid) {
                    throw IllegalStateException("Weryfikacja OpenID nie powiodła się.")
                }

                fetchUserData(steamId)
            }.onFailure { error ->
                _uiState.value = LoginUiState.Error(
                    error.message ?: "Nieznany błąd logowania."
                )
            }
        }
    }

    private suspend fun fetchUserData(steamId: String) {
        val playerResponse = steamApiService.getPlayerSummaries(BuildConfig.STEAM_API_KEY, steamId)
        val responseObj = playerResponse["response"]?.jsonObject
        val players = responseObj?.get("players") as? JsonArray
        val firstPlayer = players?.firstOrNull()?.jsonObject
        val personaName = (firstPlayer?.get("personaname") as? JsonPrimitive)?.content ?: "Użytkownik Steam"

        userRepository.saveUserData(steamId, personaName)

        val gamesResponse = steamApiService.getOwnedGames(BuildConfig.STEAM_API_KEY, steamId)
        val gamesList = parseOwnedGames(gamesResponse)

        _uiState.value = LoginUiState.Success(steamId, personaName, gamesList)
    }

    private fun parseOwnedGames(jsonObject: JsonObject): List<OwnedGame> {
        val response = jsonObject["response"]?.jsonObject
        val games = response?.get("games") as? JsonArray ?: return emptyList()
        return games.mapNotNull { element ->
            val obj = element.jsonObject
            val appId = (obj["appid"] as? JsonPrimitive)?.content?.toIntOrNull() ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.content ?: "Nieznana gra"
            val playtime = (obj["playtime_forever"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            val imgIcon = (obj["img_icon_url"] as? JsonPrimitive)?.content
            OwnedGame(appId, name, playtime, imgIcon)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.clearUserData()
            _uiState.value = LoginUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        fun factory(
            userRepository: UserRepository,
            steamApiService: SteamApiService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(userRepository, steamApiService) as T
            }
        }
    }
}
