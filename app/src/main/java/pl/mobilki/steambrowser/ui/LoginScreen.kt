package pl.mobilki.steambrowser.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.mobilki.steambrowser.LoginUiState
import pl.mobilki.steambrowser.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSteamId by viewModel.savedSteamId.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = uiState) {
            LoginUiState.Idle -> {
                if (savedSteamId.isNullOrBlank()) {
                    Text(
                        text = "Zaloguj się przez Steam, aby kontynuować.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = {
                        runCatching {
                            val url = viewModel.buildLoginUrl()
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                                .launchUrl(context, Uri.parse(url))
                        }
                    }) {
                        Text("Zaloguj przez Steam")
                    }
                } else {
                    Text(
                        text = "Zalogowano jako SteamID: $savedSteamId",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedButton(onClick = { viewModel.logout() }) {
                        Text("Wyloguj")
                    }
                }
            }

            LoginUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    text = "Weryfikacja logowania…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is LoginUiState.Success -> {
                Text(
                    text = "Zalogowano pomyślnie. SteamID: ${state.steamId}",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = { viewModel.logout() }) {
                    Text("Wyloguj")
                }
            }

            is LoginUiState.Error -> {
                Text(
                    text = "Błąd logowania: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = {
                    viewModel.resetState()
                    runCatching {
                        val url = viewModel.buildLoginUrl()
                        CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                            .launchUrl(context, Uri.parse(url))
                    }
                }) {
                    Text("Spróbuj ponownie")
                }
            }
        }
    }
}
