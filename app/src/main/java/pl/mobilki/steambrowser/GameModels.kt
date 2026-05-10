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

sealed interface GamesUiState {
    data object Loading : GamesUiState
    data class Content(
        val games: List<GameSummary>,
        val favoritesOnly: Boolean,
        val selectedGame: GameDetails?
    ) : GamesUiState

    data class Error(val message: String) : GamesUiState
}
