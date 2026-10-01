package com.tridivroy.streamly.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tridivroy.streamly.R
import com.tridivroy.streamly.presentation.home.HomeRoute
import com.tridivroy.streamly.presentation.player.PlayerRoute
import com.tridivroy.streamly.presentation.shorts.ShortsRoute

/** Tabs shown in the bottom bar / navigation rail. */
private enum class TopLevelDestination(
    val key: NavKey,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    Home(HomeKey, Icons.Filled.Home, R.string.nav_home),
    Shorts(ShortsKey, Icons.Filled.PlayArrow, R.string.nav_shorts),
}

/**
 * App navigation: a single Nav3 back stack rooted at [HomeKey]. Shorts sits on top of Home
 * (`[Home, Shorts]`), so system back from Shorts returns to Home, and leaving the Shorts tab pops
 * its entry — releasing its player pool. The tab UI is a bottom bar on compact widths and a
 * navigation rail on medium/expanded (tablets, unfolded foldables); it is hidden on the Player.
 */
@Composable
fun StreamlyNavGraph(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeKey)
    val currentTab = TopLevelDestination.entries.firstOrNull { it.key == backStack.lastOrNull() }
    val useBottomBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val onSelectTab: (TopLevelDestination) -> Unit = { tab -> backStack.selectTab(tab) }

    Row(modifier.fillMaxSize()) {
        if (currentTab != null && !useBottomBar) {
            NavigationRail {
                TopLevelDestination.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = tab == currentTab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (currentTab != null && useBottomBar) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = { onSelectTab(tab) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
            // Screens handle system insets themselves; this Scaffold only reserves room for the bar.
            contentWindowInsets = WindowInsets(0),
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
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
                    entry<ShortsKey> {
                        ShortsRoute()
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
    }
}

/** Pops back to Home, then pushes [tab] unless it is Home itself. No-op if already on [tab]. */
private fun NavBackStack<NavKey>.selectTab(tab: TopLevelDestination) {
    if (lastOrNull() == tab.key) return
    while (size > 1) removeAt(lastIndex)
    if (tab.key != HomeKey) add(tab.key)
}
