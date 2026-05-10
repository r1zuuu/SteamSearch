package pl.mobilki.steambrowser.ui.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.mobilki.steambrowser.BuildConfig
import pl.mobilki.steambrowser.data.model.GameDetails
import pl.mobilki.steambrowser.data.model.GameSummary
import pl.mobilki.steambrowser.data.model.GamesUiState
import pl.mobilki.steambrowser.data.repository.SteamRepository
import pl.mobilki.steambrowser.data.repository.toPolishMessage

class GamesViewModel(
    private val repository: SteamRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<GamesUiState>(GamesUiState.Loading)
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    private var allGames: List<GameSummary> = emptyList()
    private var allApps: List<GameSummary> = emptyList()
    private var searchResults: List<GameSummary> = emptyList()
    private var searchSuggestions: List<GameSummary> = emptyList()
    private var favorites = emptySet<Int>()
    private var playerCounts = mutableMapOf<Int, Int>()
    private var loadedDetails = mutableMapOf<Int, GameDetails>()
    private var selectedAppId: Int? = null
    private var favoritesOnly = false
    
    private var searchTextFieldValue = TextFieldValue("")
    private var isSearchActive = false
    private var isSearching = false
    private var isDatabaseLoading = false
    private var databaseError: String? = null
    private var popularGamesError: String? = null
    private var searchJob: Job? = null
    
    private val pageSize = 25
    private var currentPage = 0

    init {
        refresh()
    }

    fun refresh() {
        loadDatabase()
        
        viewModelScope.launch {
            if (_uiState.value !is GamesUiState.Content) {
                _uiState.value = GamesUiState.Loading
            }
            
            repository.getPopularGames(BuildConfig.STEAM_API_KEY).onSuccess { games ->
                allGames = games
                games.forEach { game ->
                    game.currentPlayers?.let { playerCounts[game.appId] = it }
                }
                popularGamesError = null
                publishContent()
            }.onFailure { error ->
                popularGamesError = error.toPolishMessage()
                publishContent()
            }
        }
    }

    private fun loadDatabase() {
        if (allApps.isNotEmpty() || isDatabaseLoading) return
        
        isDatabaseLoading = true
        databaseError = null
        publishContent()

        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllGames().onSuccess { apps ->
                allApps = apps
                isDatabaseLoading = false
                withContext(Dispatchers.Main) {
                    if (searchTextFieldValue.text.isNotBlank()) {
                        startSearchJob(searchTextFieldValue.text, delayMs = 0)
                    } else {
                        publishContent()
                    }
                }
            }.onFailure { error ->
                isDatabaseLoading = false
                databaseError = error.toPolishMessage()
                withContext(Dispatchers.Main) { publishContent() }
            }
        }
    }

    fun onSearchQueryChange(value: TextFieldValue) {
        val oldText = searchTextFieldValue.text
        searchTextFieldValue = value
        publishContent() 
        
        if (value.text != oldText) {
            startSearchJob(value.text)
        }
    }

    private fun startSearchJob(query: String, delayMs: Long = 300) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            searchSuggestions = emptyList()
            isSearching = false
            publishContent()
            return
        }

        searchJob = viewModelScope.launch {
            isSearching = true
            publishContent()
            if (delayMs > 0) delay(delayMs)
            
            val filtered = withContext(Dispatchers.Default) {
                val q = query.trim().lowercase()
                if (q.isEmpty()) return@withContext emptyList()
                
                val queryWords = q.split(Regex("[\\s\\-_:]+")).filter { it.isNotEmpty() }
                val queryNoSpaces = q.replace(Regex("[\\s\\-_:]+"), "")

                allApps.asSequence()
                    .map { game ->
                        val name = game.name
                        val nameLower = name.lowercase()
                        val nameWords = nameLower.split(Regex("[\\s\\-_:]+")).filter { it.isNotEmpty() }
                        val nameNoSpaces = nameLower.replace(Regex("[\\s\\-_:]+"), "")
                        
                        var score = 0
                        when {
                            nameLower == q -> score = 100
                            nameLower.startsWith(q) -> score = 95
                            nameNoSpaces == queryNoSpaces -> score = 90
                            nameNoSpaces.startsWith(queryNoSpaces) -> score = 85
                            
                            // Acronym check: CS -> Counter-Strike
                            queryWords.all { it.length <= 2 } && queryNoSpaces.length >= 2 -> {
                                val initials = nameWords.mapNotNull { it.firstOrNull() }.joinToString("")
                                if (initials == queryNoSpaces) score = 80
                                else if (initials.startsWith(queryNoSpaces)) score = 75
                            }
                        }
                        
                        if (score == 0) {
                            if (queryWords.all { word -> nameLower.contains(word) }) {
                                score = 70
                                if (nameWords.any { it.startsWith(queryWords[0]) }) score += 5
                            } else if (nameLower.contains(q)) {
                                score = 40
                            }
                        }

                        game to score
                    }
                    .filter { it.second > 0 }
                    .sortedWith(compareByDescending<Pair<GameSummary, Int>> { it.second }.thenBy { it.first.name.length })
                    .map { it.first }
                    .take(200) 
                    .toList()
            }
            
            searchResults = filtered
            searchSuggestions = filtered.take(5)
            currentPage = 0
            isSearching = false
            publishContent()
        }
    }

    fun setSearchActive(active: Boolean) {
        isSearchActive = active
        if (!active) {
            searchTextFieldValue = TextFieldValue("")
            searchResults = emptyList()
            searchSuggestions = emptyList()
            isSearching = false
        }
        publishContent()
    }

    fun onSearch(query: String) {
        startSearchJob(query, delayMs = 0)
    }

    fun loadMoreSearchResults() {
        if (searchResults.isEmpty() || (currentPage + 1) * pageSize >= searchResults.size) return
        viewModelScope.launch {
            _uiState.update { if (it is GamesUiState.Content) it.copy(isLoadingMore = true) else it }
            delay(100)
            currentPage++
            publishContent()
        }
    }

    fun selectGame(appId: Int) {
        selectedAppId = appId
        publishContent()
        
        viewModelScope.launch {
            // Fetch player count
            repository.getCurrentPlayers(appId).onSuccess { players ->
                playerCounts[appId] = players
                publishContent()
            }
            
            // Fetch full details from Store API
            if (!loadedDetails.containsKey(appId)) {
                repository.getGameDetails(appId).onSuccess { details ->
                    loadedDetails[appId] = details
                    publishContent()
                }
            }
        }
    }

    fun closeDetails() = run { selectedAppId = null; publishContent() }
    fun toggleFavorite(appId: Int) = run { favorites = if (appId in favorites) favorites - appId else favorites + appId; publishContent() }
    fun setFavoritesOnly(enabled: Boolean) = run { favoritesOnly = enabled; publishContent() }

    private fun GameSummary.withContextData() = copy(
        isFavorite = appId in favorites,
        currentPlayers = playerCounts[appId] ?: currentPlayers
    )

    private fun publishContent() {
        val gamesWithData = allGames.map { it.withContextData() }
        val suggestions = searchSuggestions.map { it.withContextData() }
        val paginatedResults = if (isSearchActive && searchTextFieldValue.text.isNotBlank()) {
            searchResults.take((currentPage + 1) * pageSize).map { it.withContextData() }
        } else emptyList()

        val visibleGames = if (favoritesOnly) gamesWithData.filter { it.isFavorite } else gamesWithData
        
        val selectedId = selectedAppId
        val selected = if (selectedId != null) {
            val base = (gamesWithData + paginatedResults + suggestions).firstOrNull { it.appId == selectedId }
            val details = loadedDetails[selectedId]
            
            if (details != null) {
                details.copy(
                    isFavorite = selectedId in favorites,
                    currentPlayers = playerCounts[selectedId] ?: details.currentPlayers
                )
            } else if (base != null) {
                GameDetails(base.appId, base.name, base.currentPlayers, base.isFavorite)
            } else null
        } else null
        
        _uiState.update {
            GamesUiState.Content(
                games = visibleGames,
                favoritesOnly = favoritesOnly,
                selectedGame = selected,
                searchQuery = searchTextFieldValue,
                isSearchActive = isSearchActive,
                searchSuggestions = suggestions,
                searchResults = paginatedResults,
                isLoadingMore = false,
                isDatabaseLoading = isDatabaseLoading,
                isSearching = isSearching,
                databaseSize = allApps.size,
                databaseError = databaseError,
                popularGamesError = popularGamesError
            )
        }
    }

    companion object {
        fun factory(repository: SteamRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = GamesViewModel(repository) as T
        }
    }
}
