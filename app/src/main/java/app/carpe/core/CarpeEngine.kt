package app.carpe.core

/**
 * Local-first decision engine for Carpe's counter-algorithm loop.
 * It intentionally does not treat all technology use as harmful.
 * The user's own goals and corrections remain authoritative.
 */
data class LifeGoal(
    val id: String,
    val label: String,
    val importance: Int = 3
)

data class TechUseObservation(
    val source: String,
    val minutes: Int,
    val intentional: Boolean? = null,
    val goalId: String? = null,
    val userRating: Int? = null
)

enum class Impact { BENEFICIAL, UNCLEAR, DETRIMENTAL }

data class CarpeAssessment(
    val impact: Impact,
    val confidence: Float,
    val explanation: String,
    val suggestedAction: String? = null
)

class CarpeEngine {
    fun assess(observation: TechUseObservation, goals: List<LifeGoal>): CarpeAssessment {
        observation.userRating?.let { rating ->
            return when {
                rating >= 4 -> CarpeAssessment(
                    Impact.BENEFICIAL, 0.95f,
                    "You marked this technology use as helpful.",
                    null
                )
                rating <= 2 -> CarpeAssessment(
                    Impact.DETRIMENTAL, 0.95f,
                    "You marked this technology use as unhelpful.",
                    "Would you like help changing what happens next time?"
                )
                else -> CarpeAssessment(
                    Impact.UNCLEAR, 0.90f,
                    "You marked this experience as mixed.",
                    "Tell Carpe what was useful and what was not."
                )
            }
        }

        if (observation.intentional == true && observation.goalId != null &&
            goals.any { it.id == observation.goalId }) {
            return CarpeAssessment(
                Impact.BENEFICIAL, 0.72f,
                "This was intentional and connected to a goal you chose.",
                null
            )
        }

        if (observation.minutes >= 45 && observation.intentional == false) {
            return CarpeAssessment(
                Impact.DETRIMENTAL, 0.68f,
                "This session was long and you did not identify it as intentional.",
                "Pause and choose whether continuing supports what matters to you."
            )
        }

        return CarpeAssessment(
            Impact.UNCLEAR, 0.35f,
            "Carpe does not have enough context to judge this use.",
            "Was this time helpful, harmful, or somewhere in between?"
        )
    }
}
