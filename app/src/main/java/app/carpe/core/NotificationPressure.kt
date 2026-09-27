package app.carpe.core

import android.content.Context

class NotificationPressure(context:Context){
    private val prefs=context.getSharedPreferences("notification_pressure",Context.MODE_PRIVATE)
    fun record(packageName:String){
        val day=System.currentTimeMillis()/86_400_000L
        val key=day.toString()+"_"+packageName
        prefs.edit().putInt(key,prefs.getInt(key,0)+1).apply()
    }
    fun today(packageName:String):Int{
        val day=System.currentTimeMillis()/86_400_000L
        return prefs.getInt(day.toString()+"_"+packageName,0)
    }
}
