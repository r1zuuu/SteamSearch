package pl.mobilki.steambrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import pl.mobilki.steambrowser.data.remote.DefaultSteamApiService
import pl.mobilki.steambrowser.data.repository.SteamRepository
import pl.mobilki.steambrowser.ui.SteamBrowserApp
import pl.mobilki.steambrowser.ui.theme.SteamBrowserTheme
import pl.mobilki.steambrowser.ui.viewmodel.GamesViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GamesViewModel by viewModels {
        GamesViewModel.factory(SteamRepository(DefaultSteamApiService()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteamBrowserTheme {
                SteamBrowserApp(viewModel = viewModel)
            }
        }
    }
}
