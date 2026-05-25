package pl.mobilki.steambrowser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import pl.mobilki.steambrowser.ui.SteamBrowserApp
import pl.mobilki.steambrowser.ui.theme.SteamBrowserTheme

class MainActivity : ComponentActivity() {
    private val steamApiService: SteamApiService by lazy { DefaultSteamApiService() }

    private val viewModel: GamesViewModel by viewModels {
        GamesViewModel.factory(
            SteamRepository(steamApiService),
            FavoritesRepository(applicationContext),
            SearchCacheRepository(applicationContext)
        )
    }

    private val dealsViewModel: DealsViewModel by viewModels {
        DealsViewModel.factory(SteamRepository(steamApiService))
    }

    private val reviewViewModel: ReviewViewModel by viewModels {
        ReviewViewModel.factory(
            ReviewRepository(steamApiService, GroqApiService())
        )
    }

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.factory(
            UserRepository(applicationContext),
            steamApiService
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLoginIntent(intent)
        setContent {
            SteamBrowserTheme {
                SteamBrowserApp(
                    viewModel = viewModel,
                    dealsViewModel = dealsViewModel,
                    reviewViewModel = reviewViewModel,
                    loginViewModel = loginViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginIntent(intent)
    }

    private fun handleLoginIntent(intent: Intent?) {
        runCatching {
            val data = intent?.data ?: return
            if ((data.scheme == "https" && data.host == "steambrowser.mobilki.pl") ||
                (data.scheme == "steambrowser" && data.host == "login")
            ) {
                loginViewModel.verifySteamLogin(data.toString())
            }
        }
    }
}
