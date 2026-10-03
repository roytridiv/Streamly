package com.tridivroy.streamly

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tridivroy.streamly.presentation.app.App
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.presentation.navigation.StreamlyNavGraph
import com.tridivroy.streamly.presentation.splash.StreamlySplash
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate(): hands the system splash over to us instead of letting it
        // dismiss on its own, so the static bolt and the Compose animation are one continuous shot.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // enableEdgeToEdge() picks bar icon colours from the *system* night mode; re-apply it
            // whenever the in-app theme differs so icons stay readable.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            // Saveable so a rotation during the animation does not replay it from the start.
            var splashShown by rememberSaveable { mutableStateOf(false) }

            // The theme now comes from :shared, so Android and iOS cannot drift apart on it.
            App(darkTheme = darkTheme) {
                StreamlyNavGraph(windowSizeClass = calculateWindowSizeClass(this))
                if (!splashShown) {
                    StreamlySplash(onFinished = { splashShown = true })
                }
            }
        }
    }

    private companion object {
        // Same scrims androidx.activity uses by default for 3-button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
