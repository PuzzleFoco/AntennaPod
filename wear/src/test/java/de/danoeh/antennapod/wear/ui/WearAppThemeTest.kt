package de.danoeh.antennapod.wear.ui

import org.junit.Assert.assertNotNull
import org.junit.Test

class WearAppThemeTest {

    @Test
    fun testWearColors_notNull() {
        assertNotNull(WearColors.primary)
        assertNotNull(WearColors.primaryVariant)
        assertNotNull(WearColors.secondary)
        assertNotNull(WearColors.background)
        assertNotNull(WearColors.surface)
        assertNotNull(WearColors.error)
        assertNotNull(WearColors.onPrimary)
        assertNotNull(WearColors.onSecondary)
        assertNotNull(WearColors.onBackground)
        assertNotNull(WearColors.onSurface)
        assertNotNull(WearColors.onSurfaceVariant)
        assertNotNull(WearColors.onError)
    }
}
