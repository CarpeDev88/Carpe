package app.carpe.core
import android.content.Context
class LearningStore(context: Context) {
 private val prefs = context.getSharedPreferences("carpe_learning", Context.MODE_PRIVATE)
 fun rating(pkg:String):Int? = if(prefs.contains("rating_"+pkg)) prefs.getInt("rating_"+pkg,3) else null
 fun rate(pkg:String,rating:Int){ prefs.edit().putInt("rating_"+pkg,rating.coerceIn(1,5)).apply() }
 fun intentional(pkg:String):Boolean? = if(prefs.contains("intentional_"+pkg)) prefs.getBoolean("intentional_"+pkg,false) else null
 fun setIntentional(pkg:String,value:Boolean){ prefs.edit().putBoolean("intentional_"+pkg,value).apply() }
}