package app.carpe.core

import android.content.Context
import java.time.LocalDate

class NotificationPressure(context:Context){
    private val prefs=context.getSharedPreferences("notification_pressure",Context.MODE_PRIVATE)
    fun record(packageName:String){
        val day=LocalDate.now().toEpochDay()
        if(prefs.getLong("cleanup_day",Long.MIN_VALUE)!=day){
            val edit=prefs.edit()
            prefs.all.keys.filter{key->key.substringBefore('_').toLongOrNull()?.let{it<day-14}==true}
                .forEach{edit.remove(it)}
            edit.putLong("cleanup_day",day).apply()
        }
        val key=day.toString()+"_"+packageName
        prefs.edit().putInt(key,prefs.getInt(key,0)+1).apply()
    }
    fun today(packageName:String):Int{
        val day=LocalDate.now().toEpochDay()
        return prefs.getInt(day.toString()+"_"+packageName,0)
    }
    fun topToday(limit:Int=5):List<Pair<String,Int>>{
        val prefix=LocalDate.now().toEpochDay().toString()+"_"
        return prefs.all.mapNotNull{(key,value)->
            val count=value as? Int
            if(!key.startsWith(prefix)||count==null||count<=0)null
            else key.removePrefix(prefix) to count
        }.sortedByDescending{it.second}.take(limit)
    }
}
