package pl.mobilki.steambrowser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.mobilki.steambrowser.data.model.GameSummary

@Composable
fun SearchContent(
    searchQuery: TextFieldValue,
    searchSuggestions: List<GameSummary>,
    searchResults: List<GameSummary>,
    isLoadingMore: Boolean,
    isDatabaseLoading: Boolean,
    isSearching: Boolean,
    databaseSize: Int,
    databaseError: String?,
    scrollState: LazyListState,
    onGameClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isDatabaseLoading) {
            InfoBox("Wczytywanie bazy gier... ($databaseSize)")
        } else if (databaseError != null) {
            Text(
                text = databaseError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        } else if (isSearching) {
            InfoBox("Szukanie...")
        }

        if (searchResults.isEmpty() && searchQuery.text.isNotBlank() && !isSearching && !isDatabaseLoading) {
            if (searchSuggestions.isNotEmpty()) {
                Text(
                    text = "Podpowiedzi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                searchSuggestions.forEach { suggestion ->
                    SuggestionRow(suggestion = suggestion, onClick = { onSuggestionClick(suggestion.name) })
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Brak wyników dla \"${searchQuery.text}\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (databaseSize > 0) {
                            Text(
                                text = "Przeszukano $databaseSize gier",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        if (searchResults.isNotEmpty()) {
            Text(
                text = "Wyniki wyszukiwania (${searchResults.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyColumn(
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { "search_${it.appId}" }) { game ->
                    GameRow(game = game, onGameClick = onGameClick, onToggleFavorite = onToggleFavorite)
                }
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: GameSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = suggestion.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
