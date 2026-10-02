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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tridivroy.streamly.presentation.onboarding.OnboardingRoute
import com.tridivroy.streamly.presentation.player.PlayerRoute
import com.tridivroy.streamly.presentation.profile.ProfileRoute
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
 * unfolded foldables).
 *
 * The bar stays up on the Player too, so a tab is always one tap away while something is playing; it
 * only disappears for fullscreen playback, Onboarding and Profile. The tab it highlights is the one
 * the Player was opened from, found by scanning down the stack.
 *
 * The mini-player docks above the bar on Home and Downloads — not on Shorts, which is edge-to-edge,
 * and not on the Player, where the real player is already on screen. Its state and the Downloads badge
 * come from [MainViewModel], since both are app chrome rather than any one screen's content.
 */
@Composable
fun StreamlyNavGraph(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val hasSession by mainViewModel.hasSession.collectAsStateWithLifecycle()

    // DataStore has not answered yet. Showing nothing for a frame or two beats guessing: rooting at
    // Home and then yanking a returning user to onboarding (or the reverse) is visible and jarring.
    // The splash is still on top at this point, so this is not a blank screen in practice.
    val sessionExists = hasSession ?: run {
        Surface(color = MaterialTheme.colorScheme.background) { Box(Modifier.fillMaxSize()) }
        return
    }

    // Evaluated once, when the back stack is first created: later sign-in and sign-out move the stack
    // explicitly rather than re-rooting it underneath whatever the user is looking at.
    val backStack = rememberNavBackStack(if (sessionExists) HomeKey else OnboardingKey)
    val useBottomBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val topKey = backStack.lastOrNull()
    val isOnTopLevelTab = TopLevelDestination.entries.any { it.key == topKey }
    val isOnPlayer = topKey is PlayerKey

    /*
     * Which tab is highlighted, found by scanning *down* the stack rather than reading its top.
     *
     * The Player is pushed on top of whichever tab opened it, so on `[Home, Player]` the answer is
     * Home. Reading only the top would leave every item unselected while a video plays.
     */
    val activeTab = backStack
        .lastOrNull { key -> TopLevelDestination.entries.any { it.key == key } }
        ?.let { key -> TopLevelDestination.entries.first { it.key == key } }

    // Fullscreen is the Player's own state, reported up because only the nav graph can hide the bar.
    var isPlayerFullscreen by remember { mutableStateOf(false) }

    /*
     * The bar stays up on the Player so tabs are reachable while something plays, and goes away for
     * fullscreen playback and for Onboarding and Profile, which are full-screen flows that own their
     * own navigation.
     */
    val showNavBar = isOnTopLevelTab || (isOnPlayer && !isPlayerFullscreen)

    val nowPlaying by mainViewModel.nowPlaying.collectAsStateWithLifecycle()
    val activeDownloads by mainViewModel.activeDownloadCount.collectAsStateWithLifecycle()

    val navItems = TopLevelDestination.entries.map { tab ->
        NavBarItem(
            label = stringResource(tab.labelRes),
            icon = tab.icon,
            selected = tab == activeTab,
            badgeCount = if (tab == TopLevelDestination.Downloads) activeDownloads else 0,
            onClick = { backStack.selectTab(tab) },
        )
    }

    // Keyed on the top of the stack, not the active tab: on the Player the real player is on screen,
    // so a mini-player of the same video docked over it would be absurd.
    val showMiniPlayer = nowPlaying != null && (topKey == HomeKey || topKey == DownloadsKey)

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(modifier.fillMaxSize()) {
            if (showNavBar && !useBottomBar) {
                StreamlyNavRail(items = navItems)
            }

            val showBottomBar = showNavBar && useBottomBar

            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        // The bar below already sits over the system navigation bar, so screens above
                        // it must not pad for those insets a second time.
                        .then(
                            if (showBottomBar) {
                                Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                            } else {
                                Modifier
                            },
                        ),
                ) {
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
                            entry<OnboardingKey> {
                                OnboardingRoute(
                                    // Onboarding is done with: it is replaced, not stacked under Home,
                                    // so back from Home leaves the app rather than returning here.
                                    onSignedIn = { backStack.resetTo(HomeKey) },
                                )
                            }
                            entry<HomeKey> {
                                HomeRoute(
                                    onNavigateToPlayer = { videoId -> backStack.add(PlayerKey(videoId)) },
                                    onNavigateToProfile = { backStack.add(ProfileKey) },
                                )
                            }
                            entry<ShortsKey> {
                                ShortsRoute()
                            }
                            entry<ProfileKey> {
                                ProfileRoute(
                                    onBack = { backStack.removeLastOrNull() },
                                    onNavigateToDownloads = { backStack.selectTab(TopLevelDestination.Downloads) },
                                    onSignedOut = { backStack.resetTo(OnboardingKey) },
                                )
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
                                    onFullscreenChange = { fullscreen -> isPlayerFullscreen = fullscreen },
                                )
                            }
                        },
                    )

                    nowPlaying?.takeIf { showMiniPlayer }?.let { playing ->
                        MiniPlayer(
                            nowPlaying = playing,
                            onClick = { backStack.add(PlayerKey(playing.video.id)) },
                            onTogglePlay = mainViewModel::onMiniPlayerToggle,
                            onDismiss = mainViewModel::onMiniPlayerDismiss,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                if (showBottomBar) {
                    StreamlyBottomBar(items = navItems)
                }
            }
        }
    }
}

/**
 * Replaces the whole back stack with [key].
 *
 * Pushes first and trims after, so the stack is never momentarily empty — NavDisplay has nothing to
 * render in that state.
 */
private fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    add(key)
    while (size > 1) removeAt(0)
}

/** Pops back to Home, then pushes [tab] unless it is Home itself. No-op if already on [tab]. */
private fun NavBackStack<NavKey>.selectTab(tab: TopLevelDestination) {
    if (lastOrNull() == tab.key) return
    while (size > 1) removeAt(lastIndex)
    if (tab.key != HomeKey) add(tab.key)
}
