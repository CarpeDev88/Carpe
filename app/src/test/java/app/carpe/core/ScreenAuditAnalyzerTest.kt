package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenAuditAnalyzerTest {
    @Test
    fun countsVisibleLabelsAndProducesAggregateReport() {
        val analyzer = ScreenAuditAnalyzer()
        analyzer.observe("Sponsored recipe ideas. Suggested for you. Keep watching more cooking videos now.")
        analyzer.observe("Sponsored recipe ideas. Suggested for you. Keep watching. Autoplay more cooking videos now.")

        val report = analyzer.report(completedAt = 123L)
        assertEquals(2, report.sampledScreens)
        assertEquals(2, report.adLabelScreens)
        assertEquals(2, report.recommendationLabelScreens)
        assertEquals(2, report.continuePromptScreens)
        assertEquals(1, report.similarScreens)
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

    @Test
    fun reportsCueShareOfSmallSampleAndHandlesInvalidCounts() {
        val report = ScreenAuditReport(
            sampledScreens = 8,
            adLabelScreens = 3,
            recommendationLabelScreens = 0,
            continuePromptScreens = 0,
            similarScreens = 0,
            completedAt = 1L
        )

        assertEquals(38, report.percentOfSamples(report.adLabelScreens))
        assertEquals(0, report.percentOfSamples(-1))
        assertEquals(100, report.percentOfSamples(99))
        assertEquals(null, report.copy(sampledScreens = 0).percentOfSamples(1))
    }
}
