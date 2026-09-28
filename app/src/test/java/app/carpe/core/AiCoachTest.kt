package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCoachTest {
    @Test
    fun optionalSensitiveGoalsProducePrivateUserLedSuggestions() {
        val suggestions = AiCoach().suggest(
            CoachContext(
                reclaimedMinutes = 0,
                goals = listOf("Reduce porn use", "Think across political viewpoints"),
                topApps = emptyList(),
                recentActions = emptyList()
            )
        )

        assertEquals(2, suggestions.size)
        assertTrue(suggestions.any { it.actionType == "content_plan" && it.reason.contains("does not monitor") })
        assertTrue(suggestions.any { it.actionType == "perspectives" && it.reason.contains("good-faith") })
    }
}
