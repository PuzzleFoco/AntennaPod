package de.danoeh.antennapod.wear.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

object WearScreens {
    const val HOME = "home"
    const val SUBSCRIPTIONS = "subscriptions"
    const val EPISODES = "episodes/{feedId}"
    const val NOW_PLAYING = "now_playing"
    const val DOWNLOADS = "downloads"
    const val QUEUE = "queue"
    const val SETTINGS = "settings"

    fun episodes(feedId: Long): String = "episodes/$feedId"
}

@Composable
fun WearAppNavigation() {
    val navController = rememberSwipeDismissableNavController()

    WearAppTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearScreens.HOME
        ) {
            composable(WearScreens.HOME) {
                HomeScreen(
                    onNavigateToSubscriptions = {
                        navController.navigate(WearScreens.SUBSCRIPTIONS)
                    },
                    onNavigateToNowPlaying = {
                        navController.navigate(WearScreens.NOW_PLAYING)
                    },
                    onNavigateToDownloads = {
                        navController.navigate(WearScreens.DOWNLOADS)
                    },
                    onNavigateToQueue = {
                        navController.navigate(WearScreens.QUEUE)
                    },
                    onNavigateToSettings = {
                        navController.navigate(WearScreens.SETTINGS)
                    }
                )
            }
            composable(WearScreens.SUBSCRIPTIONS) {
                SubscriptionsScreen(
                    onFeedClick = { feedId ->
                        navController.navigate(WearScreens.episodes(feedId))
                    }
                )
            }
            composable(WearScreens.EPISODES) { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId")?.toLongOrNull() ?: 0L
                EpisodesScreen(
                    feedId = feedId,
                    onNavigateToNowPlaying = {
                        navController.navigate(WearScreens.NOW_PLAYING)
                    }
                )
            }
            composable(WearScreens.NOW_PLAYING) {
                NowPlayingScreen()
            }
            composable(WearScreens.DOWNLOADS) {
                DownloadsScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(WearScreens.NOW_PLAYING)
                    }
                )
            }
            composable(WearScreens.QUEUE) {
                QueueScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(WearScreens.NOW_PLAYING)
                    }
                )
            }
            composable(WearScreens.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
