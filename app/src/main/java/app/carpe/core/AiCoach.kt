package app.carpe.core

import java.time.LocalDate
import java.time.ZoneId

data class CoachContext(
    val reclaimedMinutes: Int,
    val goals: List<String>,
    val topApps: List<AppUsage>,
    val recentActions: List<CarpeAction>
)
data class CoachSuggestion(val title:String,val reason:String,val actionType:String,val minutes:Int)

class AiCoach {
 fun suggest(c:CoachContext, learning:LearningStore?=null):List<CoachSuggestion>{
  val out=mutableListOf<CoachSuggestion>()
  val heavy=c.topApps.firstOrNull{it.foregroundMinutes>=45}
  if(heavy!=null) out+=CoachSuggestion(
   "Take a 10-minute departure",
   heavy.packageName.substringAfterLast('.')+" has "+heavy.foregroundMinutes+" minutes of foreground use. CARPE cannot know whether that was helpful; a short pause gives you a chance to decide.",
   "move",10)
  val todayStart=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
  if(c.goals.any{it.contains("cooking",true)}&&c.recentActions.none{it.type=="cook"&&it.completedAt>=todayStart})
   out+=CoachSuggestion("Make your next meal","Home cooking is one of the intentions you chose. Start with what you already have.","cook",30)
  if(c.goals.any{it.contains("Focused",true)||it.contains("work",true)})
   out+=CoachSuggestion("Protect one focus block","A 25-minute single-task block advances a goal you selected without asking you to stay inside CARPE.","focus",25)
  if(c.goals.any{it.contains("Saving",true)})
   out+=CoachSuggestion("Use a purchase pause","Before a non-essential purchase, wait and write down what you expect it to improve.","save",5)
  if(c.goals.any{it.equals("Reduce porn use",true)})
   out+=CoachSuggestion("Make a private pause plan","You chose this goal. CARPE does not monitor browsing or viewing content; you decide what support would help.","content_plan",5)
  if(c.goals.any{it.equals("Think across political viewpoints",true)})
   out+=CoachSuggestion("Compare good-faith viewpoints","You chose this goal. Pick one issue and examine strong good-faith arguments from more than one perspective.","perspectives",5)
  if(out.isEmpty()) out+=CoachSuggestion("Choose one deliberate action","CARPE does not have enough context to infer what would help. You stay in control.","choose",10)

  fun intent(type:String)=when(type){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;"content_plan","perspectives"->CarpeIntent.REFLECT;else->CarpeIntent.UNKNOWN}
  return out.sortedByDescending{s->learning?.recommendationScore(intent(s.actionType)) ?: .5f}.take(6)
 }
}
