package com.tridivroy.streamly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.presentation.home.HomeRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamlyTheme {
                // TODO: replace with Navigation 3 graph once the player screen exists.
                HomeRoute(onNavigateToPlayer = {})
            }
        }
    }
}
