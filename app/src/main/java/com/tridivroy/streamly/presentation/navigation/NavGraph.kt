package com.tridivroy.streamly.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tridivroy.streamly.MainViewModel
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.presentation.downloads.DownloadsRoute
import com.tridivroy.streamly.presentation.home.HomeRoute
import com.tridivroy.streamly.presentation.navigation.components.MiniPlayer
import com.tridivroy.streamly.presentation.navigation.components.NavBarItem
import com.tridivroy.streamly.presentation.navigation.components.StreamlyBottomBar
import com.tridivroy.streamly.presentation.navigation.components.StreamlyNavRail
import com.tridivroy.streamly.presentation.player.PlayerRoute
import com.tridivroy.streamly.presentation.shorts.ShortsRoute

/** Tabs shown in the bottom bar / navigation rail. */
private enum class TopLevelDestination(
    val key: NavKey,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    Home(HomeKey, StreamlyIcons.Home, R.string.nav_home),
    Shorts(ShortsKey, StreamlyIcons.Bolt, R.string.nav_shorts),
    Downloads(DownloadsKey, StreamlyIcons.Download, R.string.nav_downloads),
}

/**
 * App navigation: a single Nav3 back stack rooted at [HomeKey]. Other tabs sit on top of Home
 * (`[Home, Shorts]`, `[Home, Downloads]`), so system back returns to Home, and leaving the Shorts
 * tab pops its entry — releasing its player pool. Downloads can push the Player for offline
 * playback. The tab UI is a bottom bar on compact widths and a rail on medium/expanded (tablets,
 * unfolded foldables); both are hidden on the Player, which is full-bleed.
 *
 * The mini-player docks above the bar on Home and Downloads — not on Shorts, which is edge-to-edge,
 * and not on the Player itself. Its state and the Downloads badge come from [MainViewModel], since
 * both are app chrome rather than any one screen's content.
 */
@Composable
fun StreamlyNavGraph(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(HomeKey)
    val currentTab = TopLevelDestination.entries.firstOrNull { it.key == backStack.lastOrNull() }
    val useBottomBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val nowPlaying by mainViewModel.nowPlaying.collectAsStateWithLifecycle()
    val activeDownloads by mainViewModel.activeDownloadCount.collectAsStateWithLifecycle()

    val navItems = TopLevelDestination.entries.map { tab ->
        NavBarItem(
            label = stringResource(tab.labelRes),
            icon = tab.icon,
            selected = tab == currentTab,
            badgeCount = if (tab == TopLevelDestination.Downloads) activeDownloads else 0,
            onClick = { backStack.selectTab(tab) },
        )
    }

    val showMiniPlayer = nowPlaying != null &&
        (currentTab == TopLevelDestination.Home || currentTab == TopLevelDestination.Downloads)

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(modifier.fillMaxSize()) {
            if (currentTab != null && !useBottomBar) {
                StreamlyNavRail(items = navItems)
            }

            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize(),
                        onBack = { backStack.removeLastOrNull() },
                        // ViewModel decorator scopes each hiltViewModel() to its entry: cleared when the entry is
                        // popped, kept across rotation. Without it every screen would share the Activity's store.
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
                            entry<DownloadsKey> {
                                DownloadsRoute(onNavigateToPlayer = { videoId -> backStack.add(PlayerKey(videoId)) })
                            }
                            entry<PlayerKey> { key ->
                                PlayerRoute(
                                    videoId = key.videoId,
                                    windowSizeClass = windowSizeClass,
                                    onBack = { backStack.removeLastOrNull() },
                                    // Up Next replaces this Player entry instead of stacking another one,
                                    // so back from any suggestion still returns to the originating tab.
                                    onNavigateToVideo = { videoId ->
                                        backStack.removeLastOrNull()
                                        backStack.add(PlayerKey(videoId))
                                    },
                                )
                            }
                        },
                    )

                    nowPlaying?.takeIf { showMiniPlayer }?.let { playing ->
                        MiniPlayer(
                            nowPlaying = playing,
                            onClick = { backStack.add(PlayerKey(playing.video.id)) },
                            onTogglePlay = mainViewModel::onMiniPlayerToggle,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                if (currentTab != null && useBottomBar) {
                    StreamlyBottomBar(items = navItems)
                }
            }
        }
    }
}

/** Pops back to Home, then pushes [tab] unless it is Home itself. No-op if already on [tab]. */
private fun NavBackStack<NavKey>.selectTab(tab: TopLevelDestination) {
    if (lastOrNull() == tab.key) return
    while (size > 1) removeAt(lastIndex)
    if (tab.key != HomeKey) add(tab.key)
}
