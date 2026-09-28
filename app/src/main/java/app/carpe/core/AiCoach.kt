package app.carpe.core

import java.time.LocalDate
import java.time.ZoneId

data class CoachContext(
    val reclaimedMinutes: Int,
    val goals: List<String>,
    val topApps: List<AppUsage>,
    val recentActions: List<CarpeAction>,
    val ratings: Map<String, Int?> = emptyMap(),
    val intentions: Map<String, Boolean?> = emptyMap()
)
data class CoachSuggestion(val title:String,val reason:String,val actionType:String,val minutes:Int)

class AiCoach {
 fun suggest(c:CoachContext, learning:LearningStore?=null):List<CoachSuggestion>{
  val out=mutableListOf<CoachSuggestion>()
  val appTheUserFlagged=c.topApps.firstOrNull { app ->
   app.foregroundMinutes>=45 && (c.ratings[app.packageName]?.let { it <= 2 } == true || c.intentions[app.packageName] == false)
  }
  if(appTheUserFlagged!=null) out+=CoachSuggestion(
   "Plan a pause from an app you flagged",
   "You said "+appTheUserFlagged.packageName.substringAfterLast('.')+" pulls you away or this time did not match your intention. CARPE cannot tell what the time meant; choose whether a short pause would help.",
   "reset",10)
  val todayStart=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
  if(c.goals.any{it.equals("More time offline",true)})
   out+=CoachSuggestion("Choose an offline next step","You chose more time offline. Pick one activity that would feel worthwhile now; no app-use target is required.","offline",10)
  if(c.goals.any{it.equals("Fitness & movement",true)})
   out+=CoachSuggestion("Choose a movement that fits","Movement is one of the goals you chose. Pick something that fits your time, energy, and body today.","move",10)
  if(c.goals.any{it.contains("cooking",true)}&&c.recentActions.none{it.type=="cook"&&it.completedAt>=todayStart})
   out+=CoachSuggestion("Make your next meal","Home cooking is one of the intentions you chose. Start with what you already have.","cook",30)
  if(c.goals.any{it.contains("Focused",true)||it.contains("work",true)})
   out+=CoachSuggestion("Protect one focus block","A 25-minute single-task block advances a goal you selected without asking you to stay inside CARPE.","focus",25)
  if(c.goals.any{it.contains("Saving",true)})
   out+=CoachSuggestion("Use a purchase pause","Before a non-essential purchase, wait and write down what you expect it to improve.","save",5)
  if(c.goals.any{it.equals("Less compulsive content",true)||it.equals("Reduce porn use",true)})
   out+=CoachSuggestion("Make a private content plan","You chose this goal. CARPE does not monitor browsing or viewing content; decide what support or alternative would help.","content_plan",5)
  if(c.goals.any{it.equals("Think across political viewpoints",true)})
   out+=CoachSuggestion("Compare good-faith viewpoints","You chose this goal. Pick one issue and examine strong good-faith arguments from more than one perspective.","perspectives",5)
  if(out.isEmpty()) out+=CoachSuggestion("Choose one deliberate action","CARPE does not have enough context to infer what would help. You stay in control.","choose",10)

  fun intent(type:String)=when(type){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;"offline"->CarpeIntent.OFFLINE_ACTIVITY;"content_plan","perspectives","reset"->CarpeIntent.REFLECT;else->CarpeIntent.UNKNOWN}
  return out.sortedByDescending{s->learning?.recommendationScore(intent(s.actionType)) ?: .5f}
 }
}
