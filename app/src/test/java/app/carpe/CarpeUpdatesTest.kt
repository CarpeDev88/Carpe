package app.carpe

import org.junit.Assert.assertEquals
import org.junit.Test

class CarpeUpdatesTest {
    @Test
    fun comparesVersionNumbersNumerically() {
        assertEquals(1, CarpeUpdates.compareVersions("0.28.0", "0.27.9"))
        assertEquals(-1, CarpeUpdates.compareVersions("1.2.3", "1.2.4"))
        assertEquals(0, CarpeUpdates.compareVersions("v0.28.0", "0.28.0"))
        assertEquals(1, CarpeUpdates.compareVersions("0.28.0", "0.28"))
    }

    @Test
    fun malformedVersionsDoNotTriggerAnUpdate() {
        assertEquals(0, CarpeUpdates.compareVersions("main", "0.28.0"))
        assertEquals(0, CarpeUpdates.compareVersions("0.28.0-rc1", "0.28.0"))
    }
}
