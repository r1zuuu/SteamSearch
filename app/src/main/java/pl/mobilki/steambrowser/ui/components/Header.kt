package pl.mobilki.steambrowser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun Header(
    onRefresh: () -> Unit,
    isSearchActive: Boolean,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!isSearchActive) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Steam Browser", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Popularne gry", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { onSearchActiveChange(true) }) { Icon(Icons.Filled.Search, contentDescription = null) }
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = null) }
            } else {
                IconButton(onClick = { onSearchActiveChange(false) }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Szukaj gry...") },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange(TextFieldValue("")) }) { Icon(Icons.Filled.Clear, contentDescription = null) }
                        }
                    }
                )
                if (searchQuery.text.isNotBlank()) {
                    IconButton(onClick = { onSearch(searchQuery.text) }) { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
