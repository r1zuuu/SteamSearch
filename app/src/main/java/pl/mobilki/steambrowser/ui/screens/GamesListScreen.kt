package pl.mobilki.steambrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import pl.mobilki.steambrowser.data.model.GamesUiState
import pl.mobilki.steambrowser.ui.components.Header
import pl.mobilki.steambrowser.ui.components.HomeContent
import pl.mobilki.steambrowser.ui.components.SearchContent

@Composable
fun GamesListScreen(
    state: GamesUiState.Content,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onGameClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = scrollState.layoutInfo.totalItemsCount
            state.isSearchActive && totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !state.isLoadingMore) onLoadMore()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Header(
            onRefresh = onRefresh,
            isSearchActive = state.isSearchActive,
            searchQuery = state.searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            onSearch = onSearch
        )

        if (state.isSearchActive) {
            SearchContent(
                searchQuery = state.searchQuery,
                searchSuggestions = state.searchSuggestions,
                searchResults = state.searchResults,
                isLoadingMore = state.isLoadingMore,
                isDatabaseLoading = state.isDatabaseLoading,
                isSearching = state.isSearching,
                databaseSize = state.databaseSize,
                databaseError = state.databaseError,
                scrollState = scrollState,
                onGameClick = onGameClick,
                onToggleFavorite = onToggleFavorite,
                onSuggestionClick = onSearch
            )
        } else {
            HomeContent(
                games = state.games,
                favoritesOnly = state.favoritesOnly,
                popularGamesError = state.popularGamesError,
                onFavoritesOnlyChange = onFavoritesOnlyChange,
                onGameClick = onGameClick,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}
