package app.carpe.core
data class PatternFinding(val title:String,val evidence:String,val confidence:String,val nextStep:String)
class PatternEngine{
 fun findings(history:List<DailySnapshot>,today:List<AppUsage>):List<PatternFinding>{
  val out=mutableListOf<PatternFinding>()
  if(history.size>=3){val baseline=history.drop(1).map{it.totalMinutes}.average();val cur=history.first().totalMinutes.toDouble();if(baseline>0&&cur>baseline*1.25)out+=PatternFinding("Screen use is above your recent baseline","Today is "+cur.toLong()+" min versus a recent average of "+baseline.toLong()+" min.","Moderate","Choose one 20-minute offline action before opening another feed.")}
  val heavy=today.firstOrNull{it.foregroundMinutes>=90};if(heavy!=null)out+=PatternFinding("One app is taking a large attention share",heavy.packageName.substringAfterLast('.')+" has "+heavy.foregroundMinutes+" foreground minutes in the last 24 hours.","High","Decide whether that time matched what you intended to do.")
  if(out.isEmpty())out+=PatternFinding("Learning your baseline","CARPE needs several days of observations before trend comparisons become meaningful.","Early","Keep using your phone normally and rate apps as helpful, mixed, or harmful.")
  return out
 }
}