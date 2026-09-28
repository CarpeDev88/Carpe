package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressTest {
    @Test
    fun countsOnlyCheckInsInsideTheRequestedWindow() {
        val now = 20L * 24 * 60 * 60 * 1000
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        val checkIns = listOf(
            GoalCheckIn("walk", now),
            GoalCheckIn("walk", now - 3 * 24 * 60 * 60 * 1000),
            GoalCheckIn("walk", now - sevenDays),
            GoalCheckIn("walk", now - sevenDays - 1),
            GoalCheckIn("walk", now + 1)
        )

        assertEquals(3, GoalProgress.countWithinDays(checkIns, 7, now))
    }

    @Test
    fun nonPositiveWindowUsesOneDayAndDoesNotCountFutureCheckIns() {
        val now = 10_000L
        val checkIns = listOf(
            GoalCheckIn("cook", now),
            GoalCheckIn("cook", now - 86_400_000L),
            GoalCheckIn("cook", now + 1)
        )

        assertEquals(2, GoalProgress.countWithinDays(checkIns, 0, now))
    }
}
