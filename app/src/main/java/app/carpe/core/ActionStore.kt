package app.carpe.core
import android.content.Context
data class CarpeAction(val type:String,val title:String,val minutes:Int,val completedAt:Long=System.currentTimeMillis())
class ActionStore(context:Context){
 private val prefs=context.getSharedPreferences("carpe_actions",Context.MODE_PRIVATE)
 fun add(type:String,title:String,minutes:Int){ val old=prefs.getString("log","")?:""; val row=listOf(System.currentTimeMillis(),type,title.replace("|"," "),minutes).joinToString("|"); prefs.edit().putString("log",if(old.isBlank()) row else row+"\n"+old).apply() }
 fun recent(limit:Int=30):List<CarpeAction>=(prefs.getString("log","")?:"").lines().filter{it.isNotBlank()}.mapNotNull{val p=it.split("|");if(p.size<4)null else CarpeAction(p[1],p[2],p[3].toIntOrNull()?:0,p[0].toLongOrNull()?:0)}.take(limit)
 fun todayMinutes():Int{val cutoff=System.currentTimeMillis()-86400000L;return recent(100).filter{it.completedAt>=cutoff}.sumOf{it.minutes}}
}