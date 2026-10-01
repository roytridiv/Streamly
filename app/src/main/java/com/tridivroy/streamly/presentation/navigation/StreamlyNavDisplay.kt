package com.tridivroy.streamly.presentation.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tridivroy.streamly.presentation.home.HomeRoute
import com.tridivroy.streamly.presentation.player.PlayerRoute

@Composable
fun StreamlyNavDisplay(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeKey)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        // ViewModel decorator scopes each hiltViewModel() to its entry: cleared when the entry is popped,
        // kept across rotation. Without it every screen would share the Activity's ViewModelStore.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeRoute(onNavigateToPlayer = { videoId -> backStack.add(PlayerKey(videoId)) })
            }
            entry<PlayerKey> { key ->
                PlayerRoute(
                    videoId = key.videoId,
                    windowSizeClass = windowSizeClass,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
