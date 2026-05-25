package pl.mobilki.steambrowser.ui

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.mobilki.steambrowser.LoginUiState
import pl.mobilki.steambrowser.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSteamId by viewModel.savedSteamId.collectAsStateWithLifecycle()
    val savedPersonaName by viewModel.savedPersonaName.collectAsStateWithLifecycle()
    var showWebView by remember { mutableStateOf(false) }

    if (showWebView) {
        SteamLoginDialog(
            url = viewModel.buildLoginUrl(),
            onRedirect = { url ->
                showWebView = false
                viewModel.verifySteamLogin(url)
            },
            onDismiss = { showWebView = false }
        )
    }

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
                    Button(onClick = { showWebView = true }) {
                        Text("Zaloguj przez Steam")
                    }
                } else {
                    Text(
                        text = "Witaj, ${savedPersonaName ?: "Użytkowniku"}!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Zalogowano jako SteamID: $savedSteamId",
                        style = MaterialTheme.typography.bodyMedium
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
                    text = "Witaj, ${state.personaName}!",
                    style = MaterialTheme.typography.headlineSmall
                )
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
                    showWebView = true
                }) {
                    Text("Spróbuj ponownie")
                }
            }
        }
    }
}

@Composable
fun SteamLoginDialog(url: String, onRedirect: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                    Text(
                        text = "Logowanie Steam",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val redirectUrl = request?.url?.toString() ?: ""
                                    if (redirectUrl.startsWith("https://steambrowser.mobilki.pl/login") ||
                                        redirectUrl.startsWith("steambrowser://login")) {
                                        onRedirect(redirectUrl)
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
