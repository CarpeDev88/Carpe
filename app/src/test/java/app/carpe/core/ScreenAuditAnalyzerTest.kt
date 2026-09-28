package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAuditAnalyzerTest {
    @Test
    fun countsVisibleLabelsAndRepeatedTextWithoutKeepingOcrText() {
        val analyzer = ScreenAuditAnalyzer()
        analyzer.observe("Sponsored recipe ideas. Suggested for you. Keep watching more cooking videos now.")
        analyzer.observe("Sponsored recipe ideas. Suggested for you. Autoplay more cooking videos now.")

        val report = analyzer.report(completedAt = 123L)
        assertEquals(2, report.sampledScreens)
        assertEquals(2, report.adLabelScreens)
        assertEquals(2, report.recommendationLabelScreens)
        assertEquals(2, report.continuePromptScreens)
        assertEquals(1, report.similarScreens)
        assertTrue(report.recurringWords.contains("cooking"))
        assertEquals(123L, report.completedAt)
    }

    @Test
    fun ignoresShortOrBlankSamples() {
        val analyzer = ScreenAuditAnalyzer()
        analyzer.observe("")
        analyzer.observe("Home")
        analyzer.observe("ad")

        assertEquals(0, analyzer.report().sampledScreens)
        assertEquals(0, analyzer.report().adLabelScreens)
    }
}
