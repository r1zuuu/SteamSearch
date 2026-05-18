package pl.mobilki.steambrowser

data class GameSummary(
    val appId: Int,
    val name: String,
    val currentPlayers: Int?,
    val isFavorite: Boolean
)

data class GameDetails(
    val appId: Int,
    val name: String,
    val currentPlayers: Int?,
    val isFavorite: Boolean
)

enum class SortOrder { PLAYERS_DESC, PLAYERS_ASC, NAME_ASC }

sealed interface GamesUiState {
    data object Loading : GamesUiState
    data class Content(
        val games: List<GameSummary>,
        val favoritesOnly: Boolean,
        val selectedGame: GameDetails?,
        val searchQuery: String = "",
        val sortOrder: SortOrder = SortOrder.PLAYERS_DESC,
        val isRefreshing: Boolean = false
    ) : GamesUiState
    data class Error(val message: String) : GamesUiState
}
