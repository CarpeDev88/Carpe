package app.carpe.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAuditAiPromptTest {
    @Test
    fun promptContainsOnlyAggregateEvidenceAndItsLimits() {
        val report = ScreenAuditReport(
            sampledScreens = 10,
            adLabelScreens = 2,
            recommendationLabelScreens = 3,
            continuePromptScreens = 1,
            similarScreens = 4,
            completedAt = 100L,
            sourceLabel = "Private app label"
        )

        val prompt = ScreenAuditAiPrompt.build(report)

        assertTrue(prompt.contains("readable screens sampled: 10"))
        assertTrue(prompt.contains("screens with ad or sponsored labels: 2"))
        assertTrue(prompt.contains("Separate direct observations from possible explanations"))
        assertFalse(prompt.contains("Private app label"))
    }

    @Test
    fun countsAreClampedToTheSampleDenominator() {
        val report = ScreenAuditReport(5, 8, -1, 1, 0, 100L, "ignored label")
        val prompt = ScreenAuditAiPrompt.build(report)

        assertTrue(prompt.contains("screens with ad or sponsored labels: 5"))
        assertTrue(prompt.contains("screens with recommendation labels: 0"))
    }
}
