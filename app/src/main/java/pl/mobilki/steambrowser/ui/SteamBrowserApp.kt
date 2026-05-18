package pl.mobilki.steambrowser.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import pl.mobilki.steambrowser.DealsViewModel
import pl.mobilki.steambrowser.GameDetails
import pl.mobilki.steambrowser.GameMetadata
import pl.mobilki.steambrowser.GameSummary
import pl.mobilki.steambrowser.GamesUiState
import pl.mobilki.steambrowser.GamesViewModel
import pl.mobilki.steambrowser.ReviewViewModel
import pl.mobilki.steambrowser.SortOrder
import java.text.NumberFormat
import java.util.Locale

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Wyszukaj", Icons.Filled.Search, Icons.Outlined.Search),
    DEALS("Promocje", Icons.Filled.LocalOffer, Icons.Outlined.LocalOffer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteamBrowserApp(viewModel: GamesViewModel, dealsViewModel: DealsViewModel, reviewViewModel: ReviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val gameMetadata by viewModel.gameMetadata.collectAsState()
    var currentTab by remember { mutableStateOf(AppTab.HOME) }

    val selectedGame = (uiState as? GamesUiState.Content)?.selectedGame

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedGame == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomNavBar(
                    currentTab = currentTab,
                    onTabChange = { tab ->
                        if (tab != AppTab.SEARCH && currentTab == AppTab.SEARCH) {
                            viewModel.setSearchQuery("")
                        }
                        currentTab = tab
                    }
                )
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF070B11), Color(0xFF0A1520), Color(0xFF070B11))
                    )
                )
                .statusBarsPadding()
        ) {
            when (val state = uiState) {
                GamesUiState.Loading -> ShimmerLoadingScreen()
                is GamesUiState.Error -> ErrorScreen(message = state.message, onRetry = viewModel::refresh)
                is GamesUiState.Content -> {
                    AnimatedContent(
                        targetState = state.selectedGame,
                        transitionSpec = {
                            if (targetState != null) {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it / 3 } + fadeOut()
                            } else {
                                slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "screen_transition"
                    ) { selectedGame ->
                        if (selectedGame != null) {
                            DetailsScreen(
                                game = selectedGame,
                                metadata = gameMetadata,
                                onBack = viewModel::closeDetails,
                                onToggleFavorite = { viewModel.toggleFavorite(selectedGame.appId) },
                                reviewViewModel = reviewViewModel
                            )
                        } else {
                            when (currentTab) {
                                AppTab.HOME -> GamesListScreen(
                                    games = state.games,
                                    favoritesOnly = state.favoritesOnly,
                                    searchQuery = state.searchQuery,
                                    sortOrder = state.sortOrder,
                                    isRefreshing = state.isRefreshing,
                                    isSearching = state.isSearching,
                                    isSearchMode = state.isSearchMode,
                                    onFavoritesOnlyChange = viewModel::setFavoritesOnly,
                                    onGameClick = viewModel::selectGame,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onRefresh = viewModel::refresh,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSortOrderChange = viewModel::setSortOrder
                                )
                                AppTab.SEARCH -> SearchScreen(
                                    games = state.games,
                                    searchQuery = state.searchQuery,
                                    isSearching = state.isSearching,
                                    isSearchMode = state.isSearchMode,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onGameClick = viewModel::selectGame,
                                    onToggleFavorite = viewModel::toggleFavorite
                                )
                                AppTab.DEALS -> DealsScreen(
                                    viewModel = dealsViewModel,
                                    onGameClick = { appId, name ->
                                        viewModel.selectGameFromDeals(appId, name)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(currentTab: AppTab, onTabChange: (AppTab) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0A1520)) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabChange(tab) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00D4FF),
                    selectedTextColor = Color(0xFF00D4FF),
                    indicatorColor = Color(0xFF003543),
                    unselectedIconColor = Color(0xFF7A9AB5),
                    unselectedTextColor = Color(0xFF7A9AB5)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesListScreen(
    games: List<GameSummary>,
    favoritesOnly: Boolean,
    searchQuery: String,
    sortOrder: SortOrder,
    isRefreshing: Boolean,
    isSearching: Boolean,
    isSearchMode: Boolean,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onGameClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit
) {
    var searchVisible by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Header(
            searchVisible = searchVisible,
            onSearchToggle = {
                searchVisible = !searchVisible
                if (!searchVisible) onSearchQueryChange("")
            },
            onRefresh = onRefresh
        )

        AnimatedVisibility(
            visible = searchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Szukaj gry na Steam...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }

        AnimatedVisibility(
            visible = !isSearchMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { onFavoritesOnlyChange(false) },
                    label = { Text("Popularne") },
                    leadingIcon = {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { onFavoritesOnlyChange(true) },
                    label = { Text("Ulubione") },
                    leadingIcon = {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            Icons.Filled.Sort,
                            contentDescription = "Sortuj",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label()) },
                                onClick = {
                                    onSortOrderChange(order)
                                    sortMenuExpanded = false
                                },
                                leadingIcon = {
                                    if (sortOrder == order) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    } else {
                                        Spacer(modifier = Modifier.width(24.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isSearchMode && isSearching) {
            SearchingIndicator(query = searchQuery)
        } else if (games.isEmpty() && !isRefreshing) {
            EmptyFavorites(favoritesOnly = favoritesOnly, searchQuery = searchQuery)
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (isSearchMode) {
                        item {
                            Text(
                                text = "Wyniki dla \"$searchQuery\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    items(games, key = { it.appId }) { game ->
                        GameRow(
                            game = game,
                            onGameClick = onGameClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    games: List<GameSummary>,
    searchQuery: String,
    isSearching: Boolean,
    isSearchMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onGameClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Wyszukaj",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Wpisz nazwę gry...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Wyczyść")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isSearchMode && isSearching -> SearchingIndicator(query = searchQuery)
            isSearchMode && games.isEmpty() -> EmptyFavorites(favoritesOnly = false, searchQuery = searchQuery)
            isSearchMode -> {
                Text(
                    text = "Wyniki dla \"$searchQuery\"",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(games, key = { it.appId }) { game ->
                        GameRow(
                            game = game,
                            onGameClick = onGameClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
            else -> SearchEmptyHint()
        }
    }
}

@Composable
private fun SearchEmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Wpisz nazwę gry, żeby wyszukać",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SearchingIndicator(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Szukam \"$query\"...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Header(
    searchVisible: Boolean,
    onSearchToggle: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Steam Browser",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Popularne gry i aktualni gracze",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(onClick = onSearchToggle) {
            Icon(
                imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                contentDescription = if (searchVisible) "Zamknij wyszukiwanie" else "Szukaj"
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "Odśwież")
        }
    }
}

@Composable
private fun GameRow(
    game: GameSummary,
    onGameClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onGameClick(game.appId) },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameThumbnail(appId = game.appId, name = game.name)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = playersText(game.currentPlayers),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { onToggleFavorite(game.appId) }) {
                Icon(
                    imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (game.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    tint = if (game.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GameThumbnail(appId: Int, name: String) {
    Box(
        modifier = Modifier
            .size(width = 92.dp, height = 43.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://cdn.akamai.steamstatic.com/steam/apps/$appId/capsule_184x69.jpg")
                .crossfade(true)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DetailsScreen(
    game: GameDetails,
    metadata: GameMetadata?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    reviewViewModel: ReviewViewModel
) {
    LaunchedEffect(game.appId) { reviewViewModel.loadForGame(game.appId) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://cdn.akamai.steamstatic.com/steam/apps/${game.appId}/header.jpg")
                    .crossfade(true)
                    .build(),
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xBB000000), Color.Transparent, Color(0xFF070B11)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć", tint = Color.White)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (metadata != null) {
                Spacer(modifier = Modifier.height(10.dp))
                GameMetadataSection(metadata = metadata)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailLine(
                        icon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                        label = "App ID",
                        value = game.appId.toString()
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    DetailLine(
                        icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                        label = "Aktualni gracze",
                        value = playersText(game.currentPlayers)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onToggleFavorite,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (game.isFavorite) Color(0xFF003543)
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (game.isFavorite) Color(0xFF00D4FF)
                    else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null
                )
                Text(
                    text = if (game.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PlayerActivityChart(appId = game.appId, currentPlayers = game.currentPlayers)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ReviewPulseCard(viewModel = reviewViewModel)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GameMetadataSection(metadata: GameMetadata) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (metadata.genres.isNotEmpty() || metadata.pegiRating > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (metadata.genres.isNotEmpty()) {
                    Text(
                        text = metadata.genres.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (metadata.pegiRating > 0) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF152234), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PEGI ${metadata.pegiRating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00D4FF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        if (metadata.developers.isNotEmpty()) {
            Text(
                text = metadata.developers.take(2).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (metadata.shortDescription.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metadata.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailLine(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ShimmerLoadingScreen() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF0E1825), Color(0xFF152234), Color(0xFF0E1825)),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 300f, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        repeat(7) {
            ShimmerRow(shimmerBrush)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ShimmerRow(shimmerBrush: Brush) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 43.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nie udało się pobrać danych",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("Spróbuj ponownie", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyFavorites(favoritesOnly: Boolean, searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when {
                    searchQuery.isNotBlank() -> Icons.Filled.Search
                    favoritesOnly -> Icons.Filled.FavoriteBorder
                    else -> Icons.Filled.SportsEsports
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    searchQuery.isNotBlank() -> "Brak wyników dla \"$searchQuery\""
                    favoritesOnly -> "Brak ulubionych gier"
                    else -> "Brak gier"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun playersText(players: Int?): String {
    if (players == null) return "Brak danych o graczach"
    val formatted = NumberFormat.getIntegerInstance(Locale("pl", "PL")).format(players)
    return "$formatted graczy teraz"
}

private fun SortOrder.label(): String = when (this) {
    SortOrder.PLAYERS_DESC -> "Najwięcej graczy"
    SortOrder.PLAYERS_ASC -> "Najmniej graczy"
    SortOrder.NAME_ASC -> "Nazwa A→Z"
}
