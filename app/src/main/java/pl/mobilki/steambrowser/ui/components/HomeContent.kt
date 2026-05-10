package pl.mobilki.steambrowser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.mobilki.steambrowser.data.model.GameSummary

@Composable
fun HomeContent(
    games: List<GameSummary>,
    favoritesOnly: Boolean,
    popularGamesError: String?,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onGameClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !favoritesOnly,
                onClick = { onFavoritesOnlyChange(false) },
                label = { Text("Popularne") },
                leadingIcon = { Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            FilterChip(
                selected = favoritesOnly,
                onClick = { onFavoritesOnlyChange(true) },
                label = { Text("Ulubione") },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = if (favoritesOnly) "Brak ulubionych." else (popularGamesError ?: "Brak danych."),
                    color = if (popularGamesError != null && !favoritesOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(games, key = { it.appId }) { game ->
                    GameRow(game = game, onGameClick = onGameClick, onToggleFavorite = onToggleFavorite)
                }
            }
        }
    }
}
