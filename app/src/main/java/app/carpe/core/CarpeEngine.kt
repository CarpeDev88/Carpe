package app.carpe.core

data class LifeGoal(val id: String, val label: String, val importance: Int = 3)
data class TechUseObservation(val source: String, val minutes: Int, val intentional: Boolean? = null, val goalId: String? = null, val userRating: Int? = null)
enum class Impact { BENEFICIAL, UNCLEAR, DETRIMENTAL }
data class CarpeAssessment(val impact: Impact, val confidence: Float, val explanation: String, val suggestedAction: String? = null)

class CarpeEngine {
 fun assess(o: TechUseObservation, goals: List<LifeGoal>): CarpeAssessment {
  o.userRating?.let { r -> return when { r >= 4 -> CarpeAssessment(Impact.BENEFICIAL,.98f,"You told Carpe this technology supports your life."); r <= 2 -> CarpeAssessment(Impact.DETRIMENTAL,.98f,"You told Carpe this technology tends to pull you away from what matters.","Add friction before the next session."); else -> CarpeAssessment(Impact.UNCLEAR,.95f,"You described this technology as mixed.","Use it deliberately, then check in again.") } }
  if (o.intentional == true && o.goalId != null && goals.any { it.id == o.goalId }) return CarpeAssessment(Impact.BENEFICIAL,.82f,"This session was intentional and connected to a goal you chose.")
  if (o.minutes >= 60 && o.intentional != true) return CarpeAssessment(Impact.DETRIMENTAL,.72f,"This app has taken " + o.minutes + " minutes today without a stated purpose.","Take a 10-minute departure and decide whether to return.")
  if (o.minutes >= 30 && o.intentional == false) return CarpeAssessment(Impact.DETRIMENTAL,.68f,"You marked this use as unintentional and it has accumulated " + o.minutes + " minutes.","Pause before opening it again.")
  if (o.minutes >= 20) return CarpeAssessment(Impact.UNCLEAR,.52f,"This app has meaningful attention today, but Carpe does not know whether that time served you.","Teach Carpe: helpful, mixed, or harmful?")
  return CarpeAssessment(Impact.UNCLEAR,.30f,"Not enough evidence yet to judge this use.")
 }
}