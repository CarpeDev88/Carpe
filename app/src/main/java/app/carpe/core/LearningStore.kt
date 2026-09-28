package app.carpe.core

import android.content.Context

enum class CarpeIntent { COOK, FOCUS, MOVE, SPEND, REFLECT, UNKNOWN }

data class IntentResult(val intent: CarpeIntent, val confidence: Float)

class IntentRouter {
 fun classify(text: String): IntentResult {
  val q=text.lowercase()
  fun has(vararg words:String)=words.any{q.contains(it)}
  return when {
   has("cook","recipe","dinner","meal","ingredient","food") -> IntentResult(CarpeIntent.COOK,.92f)
   has("focus","work","study","concentrate","productive") -> IntentResult(CarpeIntent.FOCUS,.90f)
   has("walk","workout","exercise","move","outside","gym") -> IntentResult(CarpeIntent.MOVE,.90f)
   has("buy","spend","purchase","save","shopping") -> IntentResult(CarpeIntent.SPEND,.88f)
   has("bored","scroll","stuck","distracted","doomscroll") -> IntentResult(CarpeIntent.REFLECT,.82f)
   else -> IntentResult(CarpeIntent.UNKNOWN,.25f)
  }
 }
}

class LearningStore(context: Context) {
 private val prefs = context.getSharedPreferences("carpe_learning", Context.MODE_PRIVATE)
 fun rating(pkg:String):Int? = if(prefs.contains("rating_"+pkg)) prefs.getInt("rating_"+pkg,3) else null
 fun rate(pkg:String,rating:Int){ prefs.edit().putInt("rating_"+pkg,rating.coerceIn(1,5)).apply() }
 fun intentional(pkg:String):Boolean? = if(prefs.contains("intentional_"+pkg)) prefs.getBoolean("intentional_"+pkg,false) else null
 fun setIntentional(pkg:String,value:Boolean){ prefs.edit().putBoolean("intentional_"+pkg,value).apply() }

 fun recordHelpful(intent:CarpeIntent, helpful:Boolean){
  val key="helpful_"+intent.name
  val totalKey="total_"+intent.name
  prefs.edit()
   .putInt(key,prefs.getInt(key,0)+(if(helpful)1 else 0))
   .putInt(totalKey,prefs.getInt(totalKey,0)+1)
   .apply()
 }
 fun helpfulRate(intent:CarpeIntent):Float? {
  val total=prefs.getInt("total_"+intent.name,0)
  if(total==0)return null
  return prefs.getInt("helpful_"+intent.name,0).toFloat()/total
 }
}
