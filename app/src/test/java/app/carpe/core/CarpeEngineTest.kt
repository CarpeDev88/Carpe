package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarpeEngineTest {
    private val engine = CarpeEngine()

    @Test
    fun durationAloneNeverClassifiesUseAsDetrimental() {
        val assessment = engine.assess(TechUseObservation("video", 240), emptyList())

        assertEquals(Impact.UNCLEAR, assessment.impact)
        assertNull(assessment.suggestedAction)
        assertTrue(assessment.explanation.contains("duration alone"))
    }

    @Test
    fun userReportedMismatchCanSupportAReversiblePause() {
        val assessment = engine.assess(
            TechUseObservation("video", 5, intentional = false),
            emptyList()
        )

        assertEquals(Impact.DETRIMENTAL, assessment.impact)
        assertTrue(assessment.suggestedAction.orEmpty().contains("choose whether"))
    }

    @Test
    fun intentionalGoalDirectedUseIsRecognizedAsBeneficial() {
        val goal = LifeGoal("g1", "Learn Spanish")
        val assessment = engine.assess(
            TechUseObservation("language-app", 180, intentional = true, goalId = "g1"),
            listOf(goal)
        )

        assertEquals(Impact.BENEFICIAL, assessment.impact)
    }

    @Test
    fun specificMismatchOutweighsAnAppsGeneralHelpfulRating() {
        val assessment = engine.assess(
            TechUseObservation("video", 30, intentional = false, userRating = 5),
            emptyList()
        )

        assertEquals(Impact.DETRIMENTAL, assessment.impact)
    }
}
