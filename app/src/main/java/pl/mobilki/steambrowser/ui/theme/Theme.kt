package pl.mobilki.steambrowser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SteamDarkScheme = darkColorScheme(
    primary = Color(0xFF66D9A3),
    onPrimary = Color(0xFF082016),
    secondary = Color(0xFF82B7FF),
    onSecondary = Color(0xFF0B1B31),
    background = Color(0xFF101722),
    onBackground = Color(0xFFE8EEF7),
    surface = Color(0xFF182438),
    onSurface = Color(0xFFE8EEF7),
    surfaceVariant = Color(0xFF233149),
    onSurfaceVariant = Color(0xFFC5D0E0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun SteamBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SteamDarkScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
