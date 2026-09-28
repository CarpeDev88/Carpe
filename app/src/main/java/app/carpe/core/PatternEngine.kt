package app.carpe.core
data class PatternFinding(val title:String,val evidence:String,val confidence:String,val nextStep:String)
class PatternEngine{
 fun findings(today:List<AppUsage>):List<PatternFinding>{
  val out=mutableListOf<PatternFinding>()
  val heavy=today.firstOrNull{it.foregroundMinutes>=90};if(heavy!=null)out+=PatternFinding("Extended app use observed",heavy.packageName.substringAfterLast('.')+" had "+heavy.foregroundMinutes+" foreground minutes in the last 24 hours. This measures duration, not value.","Observed","Decide whether this matched your intention; duration alone cannot answer that.")
  if(out.isEmpty())out+=PatternFinding("More context helps","CARPE cannot tell whether screen time was valuable from duration alone.","Early","Rate apps as helpful, mixed, or distracting in Mirror.")
  return out
 }
}
