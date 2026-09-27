package app.carpe.core
data class CoachContext(val reclaimedMinutes:Int,val goals:List<String>,val topApps:List<AppUsage>,val recentActions:List<CarpeAction>)
data class CoachSuggestion(val title:String,val reason:String,val actionType:String,val minutes:Int)
class AiCoach{
 fun suggest(c:CoachContext):List<CoachSuggestion>{
  val out=mutableListOf<CoachSuggestion>(); val heavy=c.topApps.firstOrNull{it.foregroundMinutes>=45}
  if(heavy!=null) out+=CoachSuggestion("Take a 10-minute departure",heavy.packageName.substringAfterLast('.')+" has "+heavy.foregroundMinutes+" minutes of foreground use. A short intentional break can interrupt autopilot.","move",10)
  if(c.goals.any{it.contains("cooking",true)}&&c.recentActions.none{it.type=="cook"}) out+=CoachSuggestion("Make your next meal","Home cooking is one of the intentions you chose. Start with a recipe rather than another feed.","cook",30)
  if(c.goals.any{it.contains("Focused",true)||it.contains("work",true)}) out+=CoachSuggestion("Protect one focus block","A 25-minute single-task block advances a goal without asking you to stay inside Carpe.","focus",25)
  if(c.goals.any{it.contains("Saving",true)}) out+=CoachSuggestion("Use a purchase pause","Before a non-essential purchase, wait and write down what you expect it to improve.","save",5)
  if(out.isEmpty()) out+=CoachSuggestion("Choose one deliberate action","Carpe does not have enough context to infer what would help most. You stay in control.","choose",10)
  return out.take(3)
 }
}