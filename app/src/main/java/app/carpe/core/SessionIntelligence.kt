package app.carpe.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

data class SessionPattern(
    val packageName:String,
    val opens:Int,
    val estimatedMinutes:Long,
    val rapidReturns:Int
)

class SessionIntelligence(private val context:Context){
    fun last24Hours():List<SessionPattern>{
        val manager=context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end=System.currentTimeMillis()
        val start=end-24L*60L*60L*1000L
        val events=manager.queryEvents(start,end)
        val event=UsageEvents.Event()
        val opens=mutableMapOf<String,MutableList<Long>>()
        while(events.hasNextEvent()){
            events.getNextEvent(event)
            if(event.eventType==UsageEvents.Event.ACTIVITY_RESUMED){
                opens.getOrPut(event.packageName){mutableListOf()}.add(event.timeStamp)
            }
        }
        return opens.map{(pkg,times)->
            val sorted=times.sorted()
            val rapid=sorted.zipWithNext().count{(a,b)->b-a<10L*60L*1000L}
            SessionPattern(pkg,sorted.size,0,rapid)
        }.sortedByDescending{it.opens}
    }
}
