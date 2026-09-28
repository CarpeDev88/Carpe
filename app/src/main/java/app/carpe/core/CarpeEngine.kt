package app.carpe.core

data class LifeGoal(val id: String, val label: String, val importance: Int = 3)
data class TechUseObservation(val source: String, val minutes: Int, val intentional: Boolean? = null, val goalId: String? = null, val userRating: Int? = null)
enum class Impact { BENEFICIAL, UNCLEAR, DETRIMENTAL }
data class CarpeAssessment(val impact: Impact, val confidence: Float, val explanation: String, val suggestedAction: String? = null)

class CarpeEngine {
 fun assess(o: TechUseObservation, goals: List<LifeGoal>): CarpeAssessment {
  if (o.intentional == false) return CarpeAssessment(Impact.DETRIMENTAL,.92f,"You marked this use as not matching your intention.","Pause and choose whether you want to continue.")
  o.userRating?.let { r -> return when { r >= 4 -> CarpeAssessment(Impact.BENEFICIAL,.98f,"You told Carpe this technology supports your life."); r <= 2 -> CarpeAssessment(Impact.DETRIMENTAL,.98f,"You told Carpe this technology tends to pull you away from what matters.","Pause and choose whether to continue."); else -> CarpeAssessment(Impact.UNCLEAR,.95f,"You described this technology as mixed.","Use it deliberately, then check in again.") } }
  if (o.intentional == true && o.goalId != null && goals.any { it.id == o.goalId }) return CarpeAssessment(Impact.BENEFICIAL,.82f,"This session was intentional and connected to a goal you chose.")
  if (o.minutes > 0) return CarpeAssessment(Impact.UNCLEAR,.30f,"Carpe observed " + o.minutes + " minutes, but duration alone cannot show whether this use served you.")
  return CarpeAssessment(Impact.UNCLEAR,.20f,"Not enough evidence yet to judge this use.")
 }
}
