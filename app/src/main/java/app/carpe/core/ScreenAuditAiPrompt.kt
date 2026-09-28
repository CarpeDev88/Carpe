package app.carpe.core

/** Builds a consent-triggered AI prompt from aggregate counts only. */
object ScreenAuditAiPrompt {
    fun build(report: ScreenAuditReport): String = """
        Interpret this brief, user-started screen audit using only these aggregate counts:
        readable screens sampled: ${report.sampledScreens}
        screens with ad or sponsored labels: ${report.adLabelScreens.coerceIn(0, report.sampledScreens)}
        screens with recommendation labels: ${report.recommendationLabelScreens.coerceIn(0, report.sampledScreens)}
        screens with continue or autoplay prompts: ${report.continuePromptScreens.coerceIn(0, report.sampledScreens)}
        screens with similar visible text: ${report.similarScreens.coerceIn(0, report.sampledScreens)}

        The counts describe a short sample, not an entire feed. A label only shows wording visible on a sampled screen. Repeated text may come from the interface. Do not claim to know the service's internal ranking, personalization, or motives. Separate direct observations from possible explanations, state uncertainty, avoid diagnosing the user, and suggest one optional action that supports the user's own goals. Keep the answer concise and respectful.
    """.trimIndent()
}
