package pl.mobilki.steambrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GamesViewModel(
    private val repository: SteamRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<GamesUiState>(GamesUiState.Loading)
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    private var allGames: List<GameSummary> = emptyList()
    private var favorites = emptySet<Int>()
    private var selectedAppId: Int? = null
    private var favoritesOnly = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = GamesUiState.Loading
            repository.getPopularGames(BuildConfig.STEAM_API_KEY)
                .onSuccess { games ->
                    allGames = games
                    publishContent()
                }
                .onFailure { error ->
                    _uiState.value = GamesUiState.Error(error.toPolishMessage())
                }
        }
    }

    fun selectGame(appId: Int) {
        selectedAppId = appId
        publishContent()
        viewModelScope.launch {
            repository.getCurrentPlayers(appId).onSuccess { players ->
                allGames = allGames.map { game ->
                    if (game.appId == appId) game.copy(currentPlayers = players) else game
                }
                publishContent()
            }
        }
    }

    fun closeDetails() {
        selectedAppId = null
        publishContent()
    }

    fun toggleFavorite(appId: Int) {
        favorites = if (appId in favorites) {
            favorites - appId
        } else {
            favorites + appId
        }
        publishContent()
    }

    fun setFavoritesOnly(enabled: Boolean) {
        favoritesOnly = enabled
        publishContent()
    }

    private fun publishContent() {
        val gamesWithFavorites = allGames.map { it.copy(isFavorite = it.appId in favorites) }
        val visibleGames = if (favoritesOnly) {
            gamesWithFavorites.filter { it.isFavorite }
        } else {
            gamesWithFavorites
        }
        val selected = gamesWithFavorites.firstOrNull { it.appId == selectedAppId }?.let {
            GameDetails(
                appId = it.appId,
                name = it.name,
                currentPlayers = it.currentPlayers,
                isFavorite = it.isFavorite
            )
        }
        _uiState.update {
            GamesUiState.Content(
                games = visibleGames,
                favoritesOnly = favoritesOnly,
                selectedGame = selected
            )
        }
    }

    companion object {
        fun factory(repository: SteamRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GamesViewModel(repository) as T
                }
            }
    }
}
