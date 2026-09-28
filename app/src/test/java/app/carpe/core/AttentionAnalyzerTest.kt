package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttentionAnalyzerTest {
    @Test
    fun durationThresholdsAreDescribedAsObservations() {
        val report = AttentionAnalyzer().analyze(
            listOf(AppUsage("video.app", 35)),
            emptyMap()
        )

        assertEquals(15, report.signals.single().score)
        assertEquals(
            listOf("At least 30 minutes of foreground use"),
            report.signals.single().reasons
        )
        assertTrue(report.signals.single().reasons.none { it.contains("meaningful", ignoreCase = true) })
    }

    @Test
    fun userAssessmentChangesCueAndIsNamedInEvidence() {
        val analyzer = AttentionAnalyzer()
        val app = listOf(AppUsage("video.app", 130))

        val unassessed = analyzer.analyze(app, emptyMap()).signals.single()
        val pullsAway = analyzer.analyze(app, mapOf("video.app" to 1)).signals.single()
        val helpful = analyzer.analyze(app, mapOf("video.app" to 5)).signals.single()

        assertEquals(45, unassessed.score)
        assertEquals(80, pullsAway.score)
        assertEquals(25, helpful.score)
        assertTrue(pullsAway.reasons.contains("You rated this app as pulling you away"))
        assertTrue(helpful.reasons.contains("You rated this app as helpful"))
    }

    @Test
    fun reportTotalReflectsObservedForegroundMinutes() {
        val report = AttentionAnalyzer().analyze(
            listOf(AppUsage("one.app", 20), AppUsage("two.app", 45)),
            emptyMap()
        )

        assertEquals(65L, report.totalObservedMinutes)
    }
}
