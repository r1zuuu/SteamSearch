package pl.mobilki.steambrowser.data.model

import androidx.compose.ui.text.input.TextFieldValue

data class GameSummary(
    val appId: Int,
    val name: String,
    val currentPlayers: Int?,
    val isFavorite: Boolean,
    val imageUrl: String = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/${appId}/header.jpg"
)

data class GameDetails(
    val appId: Int,
    val name: String,
    val currentPlayers: Int?,
    val isFavorite: Boolean,
    val description: String = "",
    val imageUrl: String = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/${appId}/header.jpg",
    val developers: List<String> = emptyList(),
    val genres: List<String> = emptyList()
)

sealed interface GamesUiState {
    data object Loading : GamesUiState
    data class Content(
        val games: List<GameSummary>,
        val favoritesOnly: Boolean,
        val selectedGame: GameDetails?,
        val searchQuery: TextFieldValue = TextFieldValue(""),
        val isSearchActive: Boolean = false,
        val searchSuggestions: List<GameSummary> = emptyList(),
        val searchResults: List<GameSummary> = emptyList(),
        val isLoadingMore: Boolean = false,
        val isDatabaseLoading: Boolean = false,
        val isSearching: Boolean = false,
        val databaseSize: Int = 0,
        val databaseError: String? = null,
        val popularGamesError: String? = null
    ) : GamesUiState

    data class Error(val message: String) : GamesUiState
}
