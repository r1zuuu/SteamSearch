package pl.mobilki.steambrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import pl.mobilki.steambrowser.DealItem
import pl.mobilki.steambrowser.DealsUiState
import pl.mobilki.steambrowser.DealsViewModel
import pl.mobilki.steambrowser.GamePrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(viewModel: DealsViewModel, onGameClick: (Int, String) -> Unit = { _, _ -> }) {
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Promocje na Steam",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sprawdzaj aktualne przeceny i oceniaj, czy warto kupić grę teraz.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            DealsUiState.Loading -> DealsShimmer()
            is DealsUiState.Error -> DealsError(message = state.message, onRetry = viewModel::refresh)
            is DealsUiState.Content -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.deals.isEmpty()) {
                        DealsEmpty()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.deals, key = { it.appId }) { deal ->
                                DealCard(deal = deal, onClick = { onGameClick(deal.appId, deal.name) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DealCard(deal: DealItem, onClick: () -> Unit) {
    val isHotDeal = (deal.price?.discountPercent ?: 0) >= 70
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHotDeal) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF00D4FF))
                )
            }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 43.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = deal.name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://cdn.akamai.steamstatic.com/steam/apps/${deal.appId}/capsule_184x69.jpg")
                        .crossfade(true)
                        .build(),
                    contentDescription = deal.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deal.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                PriceInfo(price = deal.price)
                Spacer(modifier = Modifier.height(3.dp))
                DealRatingText(price = deal.price)
                deal.currentPlayers?.let { players ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDealPlayers(players),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            deal.price?.let { price ->
                if (price.discountPercent > 0) {
                    DiscountBadge(discount = price.discountPercent)
                }
            }
        }
        }
    }
}

@Composable
private fun PriceInfo(price: GamePrice?) {
    when {
        price == null -> Text(
            text = "Free to play",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF00D4FF),
            fontWeight = FontWeight.SemiBold
        )
        price.discountPercent > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = price.finalFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF00D4FF),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = price.initialFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough
            )
        }
        else -> Text(
            text = price.finalFormatted.ifBlank { "Brak ceny" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DealRatingText(price: GamePrice?) {
    val (text, color) = when {
        price == null -> "Free to play" to Color(0xFF00D4FF)
        price.discountPercent >= 70 -> "Bardzo dobra promocja" to Color(0xFF00D4FF)
        price.discountPercent >= 40 -> "Dobra promocja" to Color(0xFF0099CC)
        price.discountPercent >= 1 -> "Mała promocja" to Color(0xFFFFC107)
        else -> "Brak promocji" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun DiscountBadge(discount: Int) {
    Box(
        modifier = Modifier
            .background(Color(0xFF00D4FF), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = "-$discount%",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF001B22),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DealsShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {}
        }
    }
}

@Composable
private fun DealsError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Spacer(modifier = Modifier.height(10.dp))
                Text("Nie udało się pobrać promocji", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("Spróbuj ponownie", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private fun formatDealPlayers(players: Int): String = when {
    players >= 1_000_000 -> "${"%.1f".format(players / 1_000_000f)}M graczy teraz"
    players >= 1_000 -> "${(players / 1_000)} tys. graczy teraz"
    else -> "$players graczy teraz"
}

@Composable
private fun DealsEmpty() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Brak aktualnych promocji", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
