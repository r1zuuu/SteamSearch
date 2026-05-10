package pl.mobilki.steambrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import pl.mobilki.steambrowser.data.model.GamesUiState
import pl.mobilki.steambrowser.ui.components.ErrorScreen
import pl.mobilki.steambrowser.ui.components.LoadingScreen
import pl.mobilki.steambrowser.ui.screens.DetailsScreen
import pl.mobilki.steambrowser.ui.screens.GamesListScreen
import pl.mobilki.steambrowser.ui.viewmodel.GamesViewModel

@Composable
fun SteamBrowserApp(viewModel: GamesViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF101722), Color(0xFF132033), Color(0xFF101722))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            when (val state = uiState) {
                GamesUiState.Loading -> LoadingScreen()
                is GamesUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
                is GamesUiState.Content -> {
                    if (state.selectedGame != null) {
                        DetailsScreen(
                            game = state.selectedGame,
                            onBack = { viewModel.closeDetails() },
                            onToggleFavorite = { viewModel.toggleFavorite(state.selectedGame.appId) }
                        )
                    } else {
                        GamesListScreen(
                            state = state,
                            onFavoritesOnlyChange = { viewModel.setFavoritesOnly(it) },
                            onGameClick = { viewModel.selectGame(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onRefresh = { viewModel.refresh() },
                            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                            onSearchActiveChange = { viewModel.setSearchActive(it) },
                            onSearch = { viewModel.onSearch(it) },
                            onLoadMore = { viewModel.loadMoreSearchResults() }
                        )
                    }
                }
            }
        }
    }
}
