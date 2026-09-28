package app.carpe.core
data class PatternFinding(val title:String,val evidence:String,val confidence:String,val nextStep:String)
class PatternEngine{
 fun findings(today:List<AppUsage>):List<PatternFinding>{
  val out=mutableListOf<PatternFinding>()
  val heavy=today.firstOrNull{it.foregroundMinutes>=90};if(heavy!=null)out+=PatternFinding("One app is taking a large attention share",heavy.packageName.substringAfterLast('.')+" has "+heavy.foregroundMinutes+" foreground minutes in the last 24 hours.","High","Decide whether that time matched what you intended to do.")
  if(out.isEmpty())out+=PatternFinding("More context helps","CARPE cannot tell whether screen time was valuable from duration alone.","Early","Rate apps as helpful, mixed, or distracting in Shield.")
  return out
 }
}
