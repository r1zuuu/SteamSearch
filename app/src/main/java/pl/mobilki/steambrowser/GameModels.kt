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

data class GameMetadata(
    val shortDescription: String,
    val genres: List<String>,
    val pegiRating: Int,
    val developers: List<String>
)

enum class SortOrder { PLAYERS_DESC, PLAYERS_ASC, NAME_ASC }

data class PlayerActivityPoint(val date: String, val players: Int)

data class OwnedGame(
    val appId: Int,
    val name: String,
    val playtimeForever: Int, // in minutes
    val imgIconUrl: String?
) {
    val iconUrl: String? get() = if (imgIconUrl != null) {
        "https://media.steampowered.com/steamcommunity/public/images/apps/$appId/$imgIconUrl.jpg"
    } else null
}

data class Achievement(
    val apiname: String,
    val achieved: Int,
    val unlockTime: Long,
    val name: String? = null,
    val description: String? = null
)

sealed interface GamesUiState {
    data object Loading : GamesUiState
    data class Content(
        val games: List<GameSummary>,
        val favoritesOnly: Boolean,
        val selectedGame: GameDetails?,
        val searchQuery: String = "",
        val sortOrder: SortOrder = SortOrder.PLAYERS_DESC,
        val isRefreshing: Boolean = false,
        val isSearching: Boolean = false,
        val isSearchMode: Boolean = false
    ) : GamesUiState
    data class Error(val message: String) : GamesUiState
}
