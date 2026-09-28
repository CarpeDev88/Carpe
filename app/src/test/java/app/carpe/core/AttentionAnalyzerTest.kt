package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttentionAnalyzerTest {
    @Test
    fun durationIsShownAsEvidenceButDoesNotCreateAHarmScore() {
        val report = AttentionAnalyzer().analyze(
            listOf(AppUsage("video.app", 130)),
            emptyMap()
        )

        assertEquals(130L, report.signals.single().minutes)
        assertEquals(AttentionAssessment.NOT_ASSESSED, report.signals.single().assessment)
        assertTrue(report.signals.single().reasons.any { it.contains("130 foreground minutes") })
    }

    @Test
    fun userIntentionAndRatingDetermineTheLabel() {
        val apps = listOf(AppUsage("video.app", 130), AppUsage("chat.app", 35), AppUsage("news.app", 60))
        val report = AttentionAnalyzer().analyze(
            apps,
            mapOf("video.app" to 1, "chat.app" to 5),
            mapOf("video.app" to true, "chat.app" to false)
        )

        assertEquals(AttentionAssessment.PULLING_AWAY, report.signals[0].assessment)
        assertEquals(AttentionAssessment.DID_NOT_MATCH, report.signals[1].assessment)
        assertEquals(AttentionAssessment.NOT_ASSESSED, report.signals[2].assessment)
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
