package pl.mobilki.steambrowser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.mobilki.steambrowser.data.model.GameDetails
import pl.mobilki.steambrowser.ui.util.playersText

@Composable
fun DetailsScreen(game: GameDetails, onBack: () -> Unit, onToggleFavorite: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
            Text(text = "Szczegóły gry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(22.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = game.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                DetailLine(icon = { Icon(Icons.Outlined.Tag, contentDescription = null) }, label = "App ID", value = game.appId.toString())
                DetailLine(icon = { Icon(Icons.Outlined.Group, contentDescription = null) }, label = "Aktualni gracze", value = playersText(game.currentPlayers))
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onToggleFavorite,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (game.isFavorite) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null)
                    Text(text = if (game.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailLine(icon: @Composable () -> Unit, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { icon() }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
