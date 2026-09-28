package app.carpe.core

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

data class AppUsage(
    val packageName: String,
    val foregroundMinutes: Long
)

class UsageAccess(private val context: Context) {
    fun isGranted(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun last24Hours(): List<AppUsage> {
        if (!isGranted()) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 24L * 60L * 60L * 1000L
        val events=manager.queryEvents(start,end) ?: return emptyList()
        val event=UsageEvents.Event()
        val active=mutableMapOf<String,Long>()
        val totals=mutableMapOf<String,Long>()
        while(events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg=event.packageName ?: continue
            when(event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> active.putIfAbsent(pkg,event.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED -> active.remove(pkg)?.let{began->
                    totals[pkg]=(totals[pkg] ?: 0L)+(event.timeStamp-began).coerceAtLeast(0L)
                }
            }
        }
        active.forEach{(pkg,began)->totals[pkg]=(totals[pkg] ?: 0L)+(end-began).coerceAtLeast(0L)}
        return totals.map{(pkg,millis)->AppUsage(pkg,millis/60_000L)}
            .filter{it.foregroundMinutes>0}
            .sortedByDescending{it.foregroundMinutes}
    }
}
