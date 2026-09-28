package app.carpe.core

import android.content.Context

enum class CarpeIntent { COOK, FOCUS, MOVE, SPEND, REFLECT, OFFLINE_ACTIVITY, GOAL, CONTENT_GOAL, POLITICAL_BALANCE, UNKNOWN }

data class IntentResult(val intent: CarpeIntent, val confidence: Float)

class IntentRouter {
 fun classify(text: String): IntentResult {
  val q=text.lowercase()
  fun has(vararg words:String)=words.any{q.contains(it)}
  return when {
   has("next step toward this goal","plan a step for this goal") -> IntentResult(CarpeIntent.GOAL,.9f)
   has("porn","pornography","sexual content") -> IntentResult(CarpeIntent.CONTENT_GOAL,.9f)
   has("political viewpoints","multiple political viewpoints","different political perspectives") -> IntentResult(CarpeIntent.POLITICAL_BALANCE,.9f)
   has("offline activity","offline alternative") -> IntentResult(CarpeIntent.OFFLINE_ACTIVITY,.88f)
   has("cook","recipe","dinner","meal","ingredient","food") -> IntentResult(CarpeIntent.COOK,.92f)
   has("focus","work","study","concentrate","productive") -> IntentResult(CarpeIntent.FOCUS,.90f)
   has("walk","workout","exercise","move","outside","gym") -> IntentResult(CarpeIntent.MOVE,.90f)
   has("buy","spend","purchase","save","shopping") -> IntentResult(CarpeIntent.SPEND,.88f)
   has("bored","scroll","stuck","distracted","doomscroll","pause from technology") -> IntentResult(CarpeIntent.REFLECT,.82f)
   else -> IntentResult(CarpeIntent.UNKNOWN,.25f)
  }
 }
}

data class IntentFeedback(val helpful: Int, val total: Int)

object FeedbackMath {
    /** Four balanced prior votes keep one early rating from dominating a recommendation. */
    fun smoothedHelpfulRate(helpful: Int, total: Int): Float {
        val safeTotal = total.coerceAtLeast(0)
        val safeHelpful = helpful.coerceIn(0, safeTotal)
        return (safeHelpful + 2f) / (safeTotal + 4f)
    }
}

class LearningStore(context: Context) {
 private val prefs = context.getSharedPreferences("carpe_learning", Context.MODE_PRIVATE)
 fun rating(pkg:String):Int? = if(prefs.contains("rating_"+pkg)) prefs.getInt("rating_"+pkg,3) else null
 fun rate(pkg:String,rating:Int){ prefs.edit().putInt("rating_"+pkg,rating.coerceIn(1,5)).apply() }
 fun intentional(pkg:String):Boolean? = if(prefs.contains("intentional_"+pkg)) prefs.getBoolean("intentional_"+pkg,false) else null
 fun setIntentional(pkg:String,value:Boolean){ prefs.edit().putBoolean("intentional_"+pkg,value).apply() }

 fun clearAssessment(pkg:String){ prefs.edit().remove("rating_"+pkg).remove("intentional_"+pkg).apply() }

 fun recordHelpful(intent:CarpeIntent, helpful:Boolean){
  val key="helpful_"+intent.name
  val totalKey="total_"+intent.name
  prefs.edit()
   .putInt(key,prefs.getInt(key,0)+(if(helpful)1 else 0))
   .putInt(totalKey,prefs.getInt(totalKey,0)+1)
   .apply()
 }
 fun helpfulRate(intent:CarpeIntent):Float? {
  val counts=feedback(intent)
  if(counts.total==0)return null
  return counts.helpful.toFloat()/counts.total
 }
 fun feedback(intent:CarpeIntent):IntentFeedback {
  val total=prefs.getInt("total_"+intent.name,0).coerceAtLeast(0)
  val helpful=prefs.getInt("helpful_"+intent.name,0).coerceIn(0,total)
  return IntentFeedback(helpful,total)
 }
 fun feedbackSummary():Map<CarpeIntent,IntentFeedback> =
  CarpeIntent.values().associateWith{feedback(it)}
 fun recommendationScore(intent:CarpeIntent):Float? {
  val counts=feedback(intent)
  if(counts.total==0)return null
  return FeedbackMath.smoothedHelpfulRate(counts.helpful,counts.total)
 }
 fun clearRecommendationFeedback() {
  val editor=prefs.edit()
  prefs.all.keys.filter{it.startsWith("helpful_")||it.startsWith("total_")}.forEach{editor.remove(it)}
  editor.apply()
 }
}
