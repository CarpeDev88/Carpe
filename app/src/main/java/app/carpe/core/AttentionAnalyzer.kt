package app.carpe.core

data class AttentionSignal(val packageName:String,val score:Int,val reasons:List<String>,val minutes:Long)
data class AttentionReport(val signals:List<AttentionSignal>,val totalObservedMinutes:Long)

class AttentionAnalyzer {
    fun analyze(apps:List<AppUsage>, ratings:Map<String,Int?>):AttentionReport {
        val signals=apps.map { app ->
            var score=0
            val reasons=mutableListOf<String>()
            if(app.foregroundMinutes>=120){score+=45;reasons+="More than two hours of foreground use"}
            else if(app.foregroundMinutes>=60){score+=30;reasons+="More than one hour of foreground use"}
            else if(app.foregroundMinutes>=30){score+=15;reasons+="At least 30 minutes of foreground use"}
            when(ratings[app.packageName]){
                1,2->{score+=35;reasons+="You rated this app as pulling you away"}
                4,5->{score-=20;reasons+="You rated this app as helpful"}
            }
            AttentionSignal(app.packageName,score.coerceIn(0,100),reasons,app.foregroundMinutes)
        }.sortedByDescending{it.score}
        return AttentionReport(signals,apps.sumOf{it.foregroundMinutes})
    }
}
