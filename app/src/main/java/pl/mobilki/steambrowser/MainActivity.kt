package pl.mobilki.steambrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import pl.mobilki.steambrowser.ui.SteamBrowserApp
import pl.mobilki.steambrowser.ui.theme.SteamBrowserTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GamesViewModel by viewModels {
        GamesViewModel.factory(
            SteamRepository(DefaultSteamApiService()),
            FavoritesRepository(applicationContext),
            SearchCacheRepository(applicationContext)
        )
    }

    private val dealsViewModel: DealsViewModel by viewModels {
        DealsViewModel.factory(SteamRepository(DefaultSteamApiService()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteamBrowserTheme {
                SteamBrowserApp(viewModel = viewModel, dealsViewModel = dealsViewModel)
            }
        }
    }
}
