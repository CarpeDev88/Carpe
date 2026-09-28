package app.carpe.core

enum class AttentionAssessment(val label: String, val priority: Int) {
    PULLING_AWAY("You said this app pulls you away", 0),
    DID_NOT_MATCH("You said this time did not match your intention", 1),
    NOT_ASSESSED("Needs your assessment", 2),
    MATCHED("You said this time matched your intention", 3),
    HELPFUL("You said this app is helpful", 4)
}

data class AttentionSignal(
    val packageName: String,
    val assessment: AttentionAssessment,
    val reasons: List<String>,
    val minutes: Long
)
data class AttentionReport(val signals:List<AttentionSignal>,val totalObservedMinutes:Long)

class AttentionAnalyzer {
    fun analyze(
        apps: List<AppUsage>,
        ratings: Map<String, Int?>,
        intentions: Map<String, Boolean?> = emptyMap()
    ): AttentionReport {
        val signals=apps.map { app ->
            val reasons=mutableListOf<String>()
            if (app.foregroundMinutes > 0) reasons += "${app.foregroundMinutes} foreground minutes in the last 24 hours"
            val rating = ratings[app.packageName]
            val matchedIntention = intentions[app.packageName]
            val assessment = when {
                rating?.let { it in 1..2 } == true -> AttentionAssessment.PULLING_AWAY
                matchedIntention == false -> AttentionAssessment.DID_NOT_MATCH
                rating?.let { it in 4..5 } == true -> AttentionAssessment.HELPFUL
                matchedIntention == true -> AttentionAssessment.MATCHED
                else -> AttentionAssessment.NOT_ASSESSED
            }
            if (rating?.let { it in 1..2 } == true) reasons += "Your rating says this app pulls you away"
            if (rating?.let { it in 4..5 } == true) reasons += "Your rating says this app is helpful"
            if (matchedIntention != null) reasons += "Your feedback says this time ${if (matchedIntention) "matched" else "did not match"} your intention"
            AttentionSignal(app.packageName,assessment,reasons,app.foregroundMinutes)
        }.sortedWith(compareBy<AttentionSignal> { it.assessment.priority }.thenByDescending { it.minutes })
        return AttentionReport(signals,apps.sumOf{it.foregroundMinutes})
    }
}
