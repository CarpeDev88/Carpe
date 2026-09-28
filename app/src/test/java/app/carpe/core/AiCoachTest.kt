package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCoachTest {
    @Test
    fun durationAloneDoesNotTriggerAnIntervention() {
        val suggestions = AiCoach().suggest(
            CoachContext(
                reclaimedMinutes = 0,
                goals = emptyList(),
                topApps = listOf(AppUsage("video.app", 180)),
                recentActions = emptyList()
            )
        )

        assertTrue(suggestions.none { it.actionType == "reset" || it.actionType == "move" })
    }

    @Test
    fun aUserFlaggedMismatchCanTriggerAnOptionalPause() {
        val suggestions = AiCoach().suggest(
            CoachContext(
                reclaimedMinutes = 0,
                goals = emptyList(),
                topApps = listOf(AppUsage("video.app", 180)),
                recentActions = emptyList(),
                intentions = mapOf("video.app" to false)
            )
        )

        assertTrue(suggestions.any { it.actionType == "reset" && it.reason.contains("You said") })
    }

    @Test
    fun userMarkedHelpfulUseDoesNotTriggerAPause() {
        val suggestions = AiCoach().suggest(
            CoachContext(
                reclaimedMinutes = 0,
                goals = emptyList(),
                topApps = listOf(AppUsage("video.app", 180)),
                recentActions = emptyList(),
                ratings = mapOf("video.app" to 5),
                intentions = mapOf("video.app" to true)
            )
        )

        assertTrue(suggestions.none { it.actionType == "reset" })
    }

    @Test
    fun eachOptionalBuiltInGoalHasAFunctionalSuggestion() {
        val goals = listOf(
            "More time offline", "Fitness & movement", "Home cooking", "Focused work",
            "Saving money", "Less compulsive content", "Reduce porn use",
            "Think across political viewpoints"
        )
        val suggestions = AiCoach().suggest(
            CoachContext(0, goals, emptyList(), emptyList())
        )

        assertTrue(suggestions.any { it.actionType == "offline" })
        assertTrue(suggestions.any { it.actionType == "move" })
        assertTrue(suggestions.any { it.actionType == "cook" })
        assertTrue(suggestions.any { it.actionType == "focus" })
        assertTrue(suggestions.any { it.actionType == "save" })
        assertEquals(1, suggestions.count { it.actionType == "content_plan" })
        assertTrue(suggestions.any { it.actionType == "perspectives" })
    }

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
