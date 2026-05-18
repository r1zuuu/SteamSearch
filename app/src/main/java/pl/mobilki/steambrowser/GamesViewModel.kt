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
    private val repository: SteamRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<GamesUiState>(GamesUiState.Loading)
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    private var allGames: List<GameSummary> = emptyList()
    private var favorites = emptySet<Int>()
    private var selectedAppId: Int? = null
    private var favoritesOnly = false
    private var searchQuery = ""
    private var sortOrder = SortOrder.PLAYERS_DESC

    init {
        viewModelScope.launch {
            favoritesRepository.favoritesFlow.collect { saved ->
                favorites = saved
                if (allGames.isNotEmpty()) publishContent()
            }
        }
        refresh()
    }

    fun refresh() {
        val current = _uiState.value
        if (current is GamesUiState.Content) {
            _uiState.value = current.copy(isRefreshing = true)
        } else {
            _uiState.value = GamesUiState.Loading
        }
        viewModelScope.launch {
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
        val newFavorites = if (appId in favorites) favorites - appId else favorites + appId
        favorites = newFavorites
        viewModelScope.launch { favoritesRepository.save(newFavorites) }
        publishContent()
    }

    fun setFavoritesOnly(enabled: Boolean) {
        favoritesOnly = enabled
        publishContent()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        publishContent()
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder = order
        publishContent()
    }

    private fun publishContent() {
        val gamesWithFavorites = allGames.map { it.copy(isFavorite = it.appId in favorites) }

        val filtered = gamesWithFavorites
            .let { list -> if (favoritesOnly) list.filter { it.isFavorite } else list }
            .let { list ->
                if (searchQuery.isNotBlank()) list.filter { it.name.contains(searchQuery, ignoreCase = true) } else list
            }

        val sorted = when (sortOrder) {
            SortOrder.PLAYERS_DESC -> filtered.sortedByDescending { it.currentPlayers ?: -1 }
            SortOrder.PLAYERS_ASC -> filtered.sortedBy { it.currentPlayers ?: Int.MAX_VALUE }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name }
        }

        val selected = gamesWithFavorites.firstOrNull { it.appId == selectedAppId }?.let {
            GameDetails(appId = it.appId, name = it.name, currentPlayers = it.currentPlayers, isFavorite = it.isFavorite)
        }

        _uiState.update {
            GamesUiState.Content(
                games = sorted,
                favoritesOnly = favoritesOnly,
                selectedGame = selected,
                searchQuery = searchQuery,
                sortOrder = sortOrder,
                isRefreshing = false
            )
        }
    }

    companion object {
        fun factory(
            repository: SteamRepository,
            favoritesRepository: FavoritesRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GamesViewModel(repository, favoritesRepository) as T
                }
            }
    }
}
