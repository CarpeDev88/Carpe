package app.carpe

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carpe.core.*
import java.net.URLEncoder

private val Cream=Color(0xFFF5F1E8); private val Green=Color(0xFF355E48)
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CarpeApp()}}}

@Composable fun CarpeApp(){
 val context=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember{context.getSharedPreferences("carpe",Context.MODE_PRIVATE)}
 val actions=remember{ActionStore(context)}; val usage=remember{UsageAccess(context)}
 var tab by remember{mutableIntStateOf(0)}; var refresh by remember{mutableIntStateOf(0)}
 MaterialTheme(colorScheme=lightColorScheme(primary=Green,background=Cream)){
  Scaffold(bottomBar={NavigationBar{listOf("Today","Coach","Focus","Shield","Me").forEachIndexed{i,n->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("☀","✦","◉","⬡","●")[i])},label={Text(n)})}}}){p->
   when(tab){
    0->Today(p,context,actions){refresh++}
    1->Coach(p,prefs,usage,actions,refresh)
    2->Focus(p,actions){refresh++}
    3->Shield(p,context,usage)
    else->Me(p,prefs,actions,refresh)
   }
  }
 }
}
@Composable private fun Page(p:PaddingValues,title:String,sub:String,body:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("CARPE",color=Green,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text(title,fontSize=32.sp,fontWeight=FontWeight.Bold);Text(sub,color=Color.DarkGray);body();Spacer(Modifier.height(30.dp))}}
@Composable private fun Today(p:PaddingValues,c:Context,s:ActionStore,changed:()->Unit)=Page(p,"Own your attention.","Choose an action that improves life outside this app."){
 Text("Choose what happens next",fontSize=20.sp,fontWeight=FontWeight.Bold)
 ActionCard("Cook something","Search recipes and make a meal."){s.add("cook","Cooked something",30);changed();val q=URLEncoder.encode("healthy easy home cooking recipes","UTF-8");c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+q)))}
 ActionCard("Move your body","Walk, train, stretch, or get outside."){s.add("move","Movement",15);changed()}
 ActionCard("Do meaningful work","Start a protected 25-minute focus block."){s.add("focus","Meaningful work",25);changed()}
 ActionCard("Spend deliberately","Use a pause before a non-essential purchase."){s.add("save","Purchase pause",5);changed()}
 Text("CARPE counts completed offline actions, not time spent inside CARPE.",color=Green,fontWeight=FontWeight.Medium)
}
@Composable private fun ActionCard(t:String,d:String,on:()->Unit){ElevatedCard(onClick=on){Column(Modifier.fillMaxWidth().padding(18.dp)){Text(t,fontWeight=FontWeight.Bold,fontSize=18.sp);Text(d,color=Color.DarkGray)}}}
@Composable private fun Coach(p:PaddingValues,prefs:android.content.SharedPreferences,u:UsageAccess,a:ActionStore,r:Int)=Page(p,"CARPE intelligence","Recommendations use only the context you choose to provide. Device usage stays local in this alpha."){
 val names=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content")
 val goals=names.filter{prefs.getBoolean("goal_"+it.lowercase().replace(" ","_").replace("&","and"),it=="More time offline"||it=="Focused work")}
 val top=if(u.isGranted())u.last24Hours().take(12) else emptyList()
 val historyStore=remember{BehaviorHistory(androidx.compose.ui.platform.LocalContext.current)}
 if(u.isGranted()) historyStore.capture(top,a.todayMinutes())
 val patterns=PatternEngine().findings(historyStore.recent(),top)
 val suggestions=AiCoach().suggest(CoachContext(a.todayMinutes(),goals,top,a.recent()))
 Text("What CARPE is noticing",fontWeight=FontWeight.Bold,fontSize=20.sp)
 patterns.forEach{finding->
  ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp)){
   Text(finding.title,fontWeight=FontWeight.Bold)
   Text(finding.evidence)
   Text("Confidence: "+finding.confidence,color=Green)
   Text("Try: "+finding.nextStep,color=Color.DarkGray)
  }}
 }
 Text("Suggested next moves",fontWeight=FontWeight.Bold,fontSize=20.sp)
 suggestions.forEach{s->Card{Column(Modifier.fillMaxWidth().padding(18.dp)){Text(s.title,fontWeight=FontWeight.Bold);Text(s.reason);Text("Suggested: "+s.minutes+" min",color=Green)}}}
 Text("Why this is AI-assisted",fontWeight=FontWeight.Bold);Text("CARPE combines your explicit goals, your feedback, completed actions, and—only if you grant it—local app-usage patterns. The recommendation engine is designed to optimize for your stated life goals rather than engagement.")
}
@Composable private fun Focus(p:PaddingValues,a:ActionStore,changed:()->Unit)=Page(p,"Focus","A timer that is successful when you stop looking at CARPE."){
 var running by remember{mutableStateOf(false)};var left by remember{mutableLongStateOf(25*60_000L)};var timer by remember{mutableStateOf<FocusTimer?>(null)}
 Text(String.format("%02d:%02d",left/60000,(left/1000)%60),fontSize=52.sp,fontWeight=FontWeight.Bold)
 Button(onClick={if(!running){running=true;timer=FocusTimer(25,{left=it},{running=false;left=0;a.add("focus","Completed focus session",25);changed()}).also{it.start()}}else{timer?.cancel();running=false}},modifier=Modifier.fillMaxWidth()){Text(if(running)"Stop session" else "Start 25-minute focus")}
 Text("Put the phone down. CARPE will not send engagement prompts during the session.")
}
@Composable private fun Shield(p:PaddingValues,c:Context,u:UsageAccess)=Page(p,"Algorithm shield","See and reduce the signals that attention-harvesting systems use."){
 val granted=u.isGranted()
 ActionCard("Usage intelligence",if(granted)"Enabled. CARPE can analyze foreground app time locally." else "Optional. Tap to grant Android Usage Access."){if(!granted)c.startActivity(u.settingsIntent())}
 ActionCard("Notification intelligence","Grant CARPE notification access to measure which apps repeatedly compete for your attention."){c.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))}
 ActionCard("Notification controls","Open Android notification settings to silence apps that pull you back."){c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.packageName))}
 ActionCard("Privacy dashboard","Review Android permissions granted to apps on this device."){try{c.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))}catch(_:Exception){}}
 if(granted){
  val apps=u.last24Hours().take(12)
  val learning=remember{LearningStore(c)}
  val ratings=apps.associate{it.packageName to learning.rating(it.packageName)}
  val report=AttentionAnalyzer().analyze(apps,ratings)
  val pressure=remember{NotificationPressure(c)}
  Text("Attention intelligence",fontWeight=FontWeight.Bold)
  Text(report.totalObservedMinutes.toString()+" foreground minutes observed locally.")
  report.signals.take(8).forEach{sig->
   Card{Column(Modifier.fillMaxWidth().padding(14.dp)){
    Text(sig.packageName.substringAfterLast('.'),fontWeight=FontWeight.Bold)
    Text("Attention-risk signal: "+sig.score+"/100 • "+sig.minutes+" min • "+pressure.today(sig.packageName)+" notifications today")
    if(sig.reasons.isNotEmpty()) Text(sig.reasons.joinToString(" • "),color=Color.DarkGray)
   }}
  }
 }
 if(granted){
  val sessions=remember{SessionIntelligence(c)}.last24Hours().take(6)
  Text("Reopening patterns",fontWeight=FontWeight.Bold)
  sessions.forEach{s->
   Text(s.packageName.substringAfterLast('.')+" • "+s.opens+" opens • "+s.rapidReturns+" rapid returns")
  }
 }
 Text("CARPE does not require these permissions. Granting them should add capability, never unlock basic dignity or usefulness.",color=Green)
}
@Composable private fun Me(p:PaddingValues,prefs:android.content.SharedPreferences,a:ActionStore,r:Int)=Page(p,"Your life, not a feed","Set what CARPE should optimize for and review what you actually did."){
 val goals=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content")
 goals.forEach{g->val k="goal_"+g.lowercase().replace(" ","_").replace("&","and");var on by remember{mutableStateOf(prefs.getBoolean(k,g=="More time offline"||g=="Focused work"))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(g,Modifier.weight(1f));Switch(on,{on=it;prefs.edit().putBoolean(k,it).apply()})}}
 HorizontalDivider();Text(a.todayMinutes().toString()+" minutes invested in deliberate actions",fontSize=22.sp,fontWeight=FontWeight.Bold)
 a.recent(8).forEach{Text("• "+it.title+" — "+it.minutes+" min")}
 Text("CARPE v0.3 alpha",color=Green,fontWeight=FontWeight.Bold)
}