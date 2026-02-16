package de.danoeh.antennapod.wear.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingScreenTest {

    @Test
    fun testFormatTime_zero() {
        assertEquals("0:00", formatTimeTestable(0))
    }

    @Test
    fun testFormatTime_negative() {
        assertEquals("0:00", formatTimeTestable(-1000))
    }

    @Test
    fun testFormatTime_seconds() {
        assertEquals("0:30", formatTimeTestable(30000))
    }

    @Test
    fun testFormatTime_minutes() {
        assertEquals("5:00", formatTimeTestable(300000))
    }

    @Test
    fun testFormatTime_minutesAndSeconds() {
        assertEquals("5:30", formatTimeTestable(330000))
    }

    @Test
    fun testFormatTime_hours() {
        assertEquals("1:00:00", formatTimeTestable(3600000))
    }

    @Test
    fun testFormatTime_hoursMinutesSeconds() {
        assertEquals("1:30:45", formatTimeTestable(5445000))
    }

    @Test
    fun testFormatTime_multipleHours() {
        assertEquals("2:05:03", formatTimeTestable(7503000))
    }

    private fun formatTimeTestable(millis: Long): String {
        if (millis <= 0) return "0:00"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
