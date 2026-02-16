package de.danoeh.antennapod.wear.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WearScreensTest {

    @Test
    fun testScreenRoutes() {
        assertEquals("home", WearScreens.HOME)
        assertEquals("subscriptions", WearScreens.SUBSCRIPTIONS)
        assertEquals("now_playing", WearScreens.NOW_PLAYING)
        assertEquals("downloads", WearScreens.DOWNLOADS)
        assertEquals("queue", WearScreens.QUEUE)
        assertEquals("settings", WearScreens.SETTINGS)
        assertEquals("add_podcast", WearScreens.ADD_PODCAST)
        assertEquals("sync_login/{provider}", WearScreens.SYNC_LOGIN)
    }

    @Test
    fun testEpisodesRoute() {
        assertEquals("episodes/{feedId}", WearScreens.EPISODES)
    }

    @Test
    fun testEpisodesRouteWithId() {
        assertEquals("episodes/42", WearScreens.episodes(42))
        assertEquals("episodes/0", WearScreens.episodes(0))
        assertEquals("episodes/999", WearScreens.episodes(999))
    }

    @Test
    fun testSyncLoginRoute() {
        assertEquals("sync_login/GPODDER_NET", WearScreens.syncLogin("GPODDER_NET"))
        assertEquals("sync_login/NEXTCLOUD_GPODDER", WearScreens.syncLogin("NEXTCLOUD_GPODDER"))
    }
}
