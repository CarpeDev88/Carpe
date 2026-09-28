package app.carpe.core

import android.content.Context
import java.time.LocalDate

class NotificationPressure(context:Context){
    private val prefs=context.getSharedPreferences("notification_pressure",Context.MODE_PRIVATE)
    fun record(packageName:String){
        val day=LocalDate.now().toEpochDay()
        val key=day.toString()+"_"+packageName
        prefs.edit().putInt(key,prefs.getInt(key,0)+1).apply()
    }
    fun today(packageName:String):Int{
        val day=LocalDate.now().toEpochDay()
        return prefs.getInt(day.toString()+"_"+packageName,0)
    }
}
