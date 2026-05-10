package pl.mobilki.steambrowser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(42.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Wystąpił błąd", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("Spróbuj ponownie", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
