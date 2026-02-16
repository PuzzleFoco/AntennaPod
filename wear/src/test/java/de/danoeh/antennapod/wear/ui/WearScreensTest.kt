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
}
