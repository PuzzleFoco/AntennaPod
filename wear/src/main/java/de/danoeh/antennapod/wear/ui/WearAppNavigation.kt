package de.danoeh.antennapod.wear.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider

object WearScreens {
    const val HOME = "home"
    const val SUBSCRIPTIONS = "subscriptions"
    const val EPISODES = "episodes/{feedId}"
    const val NOW_PLAYING = "now_playing"
    const val DOWNLOADS = "downloads"
    const val QUEUE = "queue"
    const val SETTINGS = "settings"
    const val ADD_PODCAST = "add_podcast"
    const val SYNC_LOGIN = "sync_login/{provider}"
    const val NEXTCLOUD_LOGIN = "nextcloud_login"

    fun episodes(feedId: Long): String = "episodes/$feedId"
    fun syncLogin(provider: String): String = "sync_login/$provider"
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
                    },
                    onNavigateToAddPodcast = {
                        navController.navigate(WearScreens.ADD_PODCAST)
                    }
                )
            }
            composable(WearScreens.SUBSCRIPTIONS) {
                SubscriptionsScreen(
                    onFeedClick = { feedId ->
                        navController.navigate(WearScreens.episodes(feedId))
                    },
                    onNavigateToAddPodcast = {
                        navController.navigate(WearScreens.ADD_PODCAST)
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
                SettingsScreen(
                    onNavigateToSyncLogin = { provider ->
                        navController.navigate(WearScreens.syncLogin(provider))
                    },
                    onNavigateToNextcloudLogin = {
                        navController.navigate(WearScreens.NEXTCLOUD_LOGIN)
                    }
                )
            }
            composable(WearScreens.ADD_PODCAST) {
                AddPodcastScreen(
                    onSubscribed = {
                        navController.popBackStack()
                    }
                )
            }
            composable(WearScreens.SYNC_LOGIN) { backStackEntry ->
                val providerKey = backStackEntry.arguments?.getString("provider") ?: "GPODDER_NET"
                val provider = SynchronizationProvider.fromIdentifier(providerKey)
                    ?: SynchronizationProvider.GPODDER_NET
                SyncLoginScreen(
                    provider = provider,
                    onLoginSuccess = {
                        navController.popBackStack()
                    }
                )
            }
            composable(WearScreens.NEXTCLOUD_LOGIN) {
                NextcloudLoginScreen(
                    onLoginSuccess = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
