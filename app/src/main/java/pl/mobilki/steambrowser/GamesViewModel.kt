package pl.mobilki.steambrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GamesViewModel(
    private val repository: SteamRepository,
    private val favoritesRepository: FavoritesRepository,
    private val searchCacheRepository: SearchCacheRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<GamesUiState>(GamesUiState.Loading)
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    private val _gameMetadata = MutableStateFlow<GameMetadata?>(null)
    val gameMetadata: StateFlow<GameMetadata?> = _gameMetadata.asStateFlow()
    private val metadataCache = mutableMapOf<Int, GameMetadata>()

    private var allGames: List<GameSummary> = emptyList()
    private var searchResults: List<GameSummary>? = null
    private var favorites = emptySet<Int>()
    private var selectedAppId: Int? = null
    private var favoritesOnly = false
    private var searchQuery = ""
    private var sortOrder = SortOrder.PLAYERS_DESC
    private var isSearching = false
    private var searchJob: Job? = null

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
        _gameMetadata.value = metadataCache[appId]
        publishContent()
        viewModelScope.launch {
            repository.getCurrentPlayers(appId).onSuccess { players ->
                allGames = allGames.map { if (it.appId == appId) it.copy(currentPlayers = players) else it }
                searchResults = searchResults?.map { if (it.appId == appId) it.copy(currentPlayers = players) else it }
                publishContent()
            }
            if (appId !in metadataCache) {
                repository.getGameMetadata(appId).onSuccess { meta ->
                    if (meta != null) {
                        metadataCache[appId] = meta
                        if (selectedAppId == appId) _gameMetadata.value = meta
                    }
                }
            }
        }
    }

    fun selectGameFromDeals(appId: Int, name: String) {
        if (allGames.none { it.appId == appId }) {
            allGames = allGames + GameSummary(appId, name, null, appId in favorites)
        }
        selectGame(appId)
    }

    fun closeDetails() {
        selectedAppId = null
        _gameMetadata.value = null
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
        searchJob?.cancel()

        if (query.isBlank()) {
            searchResults = null
            isSearching = false
            publishContent()
            return
        }

        isSearching = true
        searchResults = null
        publishContent()

        searchJob = viewModelScope.launch {
            delay(500)
            repository.searchGames(query)
                .onSuccess { games ->
                    searchResults = games.map { it.copy(isFavorite = it.appId in favorites) }
                    isSearching = false
                    searchCacheRepository.save(games)
                    publishContent()
                }
                .onFailure {
                    searchResults = emptyList()
                    isSearching = false
                    publishContent()
                }
        }
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder = order
        publishContent()
    }

    private fun publishContent() {
        val isSearchMode = searchQuery.isNotBlank()

        val games: List<GameSummary> = when {
            isSearchMode -> {
                val results = searchResults ?: emptyList()
                results.map { it.copy(isFavorite = it.appId in favorites) }
            }
            else -> {
                allGames.map { it.copy(isFavorite = it.appId in favorites) }
                    .let { list -> if (favoritesOnly) list.filter { it.isFavorite } else list }
                    .let { list ->
                        when (sortOrder) {
                            SortOrder.PLAYERS_DESC -> list.sortedByDescending { it.currentPlayers ?: -1 }
                            SortOrder.PLAYERS_ASC -> list.sortedBy { it.currentPlayers ?: Int.MAX_VALUE }
                            SortOrder.NAME_ASC -> list.sortedBy { it.name }
                        }
                    }
            }
        }

        val allWithFavorites = allGames.map { it.copy(isFavorite = it.appId in favorites) }
        val searchWithFavorites = searchResults?.map { it.copy(isFavorite = it.appId in favorites) }
        val selected = (searchWithFavorites ?: allWithFavorites)
            .firstOrNull { it.appId == selectedAppId }
            ?.let { GameDetails(it.appId, it.name, it.currentPlayers, it.isFavorite) }

        _uiState.update {
            GamesUiState.Content(
                games = games,
                favoritesOnly = favoritesOnly,
                selectedGame = selected,
                searchQuery = searchQuery,
                sortOrder = sortOrder,
                isRefreshing = false,
                isSearching = isSearching,
                isSearchMode = isSearchMode
            )
        }
    }

    companion object {
        fun factory(
            repository: SteamRepository,
            favoritesRepository: FavoritesRepository,
            searchCacheRepository: SearchCacheRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GamesViewModel(repository, favoritesRepository, searchCacheRepository) as T
                }
            }
    }
}
