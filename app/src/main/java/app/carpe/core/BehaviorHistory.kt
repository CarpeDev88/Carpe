package app.carpe.core
import android.content.Context
import java.time.LocalDate
data class DailySnapshot(val day:Long,val totalMinutes:Long,val topPackage:String?,val topMinutes:Long,val reclaimedMinutes:Int)
class BehaviorHistory(context:Context){
 private val prefs=context.getSharedPreferences("behavior_history",Context.MODE_PRIVATE)
 fun capture(apps:List<AppUsage>,reclaimed:Int){val day=LocalDate.now().toEpochDay();val top=apps.maxByOrNull{it.foregroundMinutes};prefs.edit().putString("day_"+day,listOf(apps.sumOf{it.foregroundMinutes},top?.packageName?:"",top?.foregroundMinutes?:0,reclaimed).joinToString("|")).apply()}
 fun recent(days:Int=14):List<DailySnapshot>{val now=LocalDate.now().toEpochDay();return (0 until days).mapNotNull{i->val day=now-i;val raw=prefs.getString("day_"+day,null)?:return@mapNotNull null;val p=raw.split("|");if(p.size<4)null else DailySnapshot(day,p[0].toLongOrNull()?:0,p[1].ifBlank{null},p[2].toLongOrNull()?:0,p[3].toIntOrNull()?:0)}}
}
