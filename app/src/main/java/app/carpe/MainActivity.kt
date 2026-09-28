package app.carpe

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carpe.core.*

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
@Composable private fun Today(p:PaddingValues,c:Context,s:ActionStore,changed:()->Unit)=Page(p,"What do you want to do right now?","Tell CARPE what you need. Type naturally or use your voice."){
 var input by remember{mutableStateOf("")}; var response by remember{mutableStateOf("")}; var thinking by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope(); val gateway=remember{SecureAiGateway(c)}; val history=remember{mutableStateListOf<AiTurn>()}
 val profile=remember{UserProfileStore(c)}
 val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{input=it}}
 val router=remember{IntentRouter()}
 fun act(){val q=input.trim();if(q.isBlank()||thinking)return;profile.learn(q);val routed=router.classify(q)
  val local=when(routed.intent){
   CarpeIntent.COOK->"Let's cook without leaving CARPE. Tell me what ingredients you have, how much time you want to spend, and any budget or dietary limits."
   CarpeIntent.FOCUS->"Let's turn that intention into action. Open Focus below for a protected 25-minute block, then put the phone down."
   CarpeIntent.MOVE->"Choose the smallest useful movement you can start now: a 10-minute walk, stretching, or a short workout."
   CarpeIntent.SPEND->"Before buying, name what problem the purchase solves, whether you already own an alternative, and whether waiting 24 hours would change the decision."
   CarpeIntent.REFLECT->"You noticed the loop. Pick one small departure: put the phone down for 10 minutes, walk outside, make food, or start one task you care about."
   CarpeIntent.UNKNOWN->null
  }
  thinking=true;response="";val prior=history.toList();history+=AiTurn("user",q);input=""
  scope.launch{gateway.ask(q,if(profile.enabled())profile.summary() else "",prior).fold(
   onSuccess={answer->response=answer;history+=AiTurn("assistant",answer)},
   onFailure={e->
    response=if(local!=null) "Cloud AI is unavailable (${e.message ?: "connection failed"}). Here's a local suggestion:\n\n$local"
     else (e.message ?: "CARPE could not reach its AI service. Please try again.")
    if(local!=null) history+=AiTurn("assistant",local)
   }
  );thinking=false}
 }
 OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.fillMaxWidth().heightIn(min=120.dp),placeholder={Text("Ask CARPE anything…")},maxLines=6)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={try{voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT,"Talk to CARPE"))}catch(_:Exception){response="Voice recognition isn't available on this device."}},modifier=Modifier.weight(1f)){Text("🎤  Speak")};Button(onClick={act()},enabled=!thinking,modifier=Modifier.weight(1f)){Text(if(thinking)"Thinking…" else "Send")}}
 if(response.isNotBlank()) ElevatedCard{Text(response,Modifier.fillMaxWidth().padding(16.dp))}
 Text("Suggestions",fontSize=18.sp,fontWeight=FontWeight.Bold)
 ActionCard("Cook something","Tell CARPE what you have, what sounds good, your budget, or how much time you have."){input="Help me cook something. Ask me what ingredients I have, what sounds good, and how much time I have."}
 ActionCard("Move your body","Walk, train, stretch, or get outside."){input="Help me move my body today"}
 ActionCard("Do meaningful work","Start a protected focus block."){input="Help me focus on meaningful work"}
 ActionCard("Spend deliberately","Pause before a non-essential purchase."){input="Help me make a deliberate spending decision"}
 Text("CARPE counts completed offline actions, not time spent inside CARPE.",color=Green,fontWeight=FontWeight.Medium)
}
@Composable private fun ActionCard(t:String,d:String,on:()->Unit){ElevatedCard(onClick=on){Column(Modifier.fillMaxWidth().padding(18.dp)){Text(t,fontWeight=FontWeight.Bold,fontSize=18.sp);Text(d,color=Color.DarkGray)}}}
@Composable private fun Coach(p:PaddingValues,prefs:android.content.SharedPreferences,u:UsageAccess,a:ActionStore,r:Int)=Page(p,"CARPE intelligence","Recommendations use only the context you choose to provide. Device usage stays local in this alpha."){
 val names=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content")
 val goals=names.filter{prefs.getBoolean("goal_"+it.lowercase().replace(" ","_").replace("&","and"),it=="More time offline"||it=="Focused work")}
 val top=if(u.isGranted())u.last24Hours().take(12) else emptyList()
 val coachContext=androidx.compose.ui.platform.LocalContext.current
 val historyStore=remember(coachContext){BehaviorHistory(coachContext)}
 if(u.isGranted()) historyStore.capture(top,a.todayMinutes())
 val patterns=PatternEngine().findings(historyStore.recent(),top)
 val learning=remember(coachContext){LearningStore(coachContext)}
 val suggestions=AiCoach().suggest(CoachContext(a.todayMinutes(),goals,top,a.recent()),learning)
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
 suggestions.forEach{s->Card{Column(Modifier.fillMaxWidth().padding(18.dp)){
  Text(s.title,fontWeight=FontWeight.Bold);Text(s.reason);Text("Suggested: "+s.minutes+" min",color=Green)
  Text("Why: based on goals and local patterns you allowed CARPE to use.",fontSize=12.sp,color=Color.DarkGray)
  var rated by remember(s.title){mutableStateOf(false)}
  if(!rated) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick={learning.recordHelpful(when(s.actionType){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;else->CarpeIntent.UNKNOWN},true);rated=true}){Text("Helpful")}
   TextButton(onClick={learning.recordHelpful(when(s.actionType){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;else->CarpeIntent.UNKNOWN},false);rated=true}){Text("Not helpful")}
  } else Text("Thanks. CARPE will use that locally.",fontSize=12.sp,color=Green)
 }}}
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
 val context=androidx.compose.ui.platform.LocalContext.current
 val profile=remember{UserProfileStore(context)}
 var aiProfile by remember{mutableStateOf(profile.enabled())}
 Text("AI & privacy",fontWeight=FontWeight.Bold,fontSize=20.sp)
 Text("Cloud AI uses a CARPE service URL. Your messages are sent to that service and its AI provider when you tap Send. Never enter an AI key here.",color=Color.DarkGray,fontSize=13.sp)
 var endpointInput by remember{mutableStateOf(SecureAiGateway.configuredEndpoint(context))}
 var serviceStatus by remember{mutableStateOf("")}
 var testing by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 OutlinedTextField(value=endpointInput,onValueChange={endpointInput=it},modifier=Modifier.fillMaxWidth(),label={Text("CARPE AI service URL")},placeholder={Text("https://…/v1/ask")},singleLine=true)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Button(onClick={
   runCatching{SecureAiGateway.setEndpoint(context,endpointInput)}.fold(
    onSuccess={serviceStatus="Service URL saved. Tap Test AI to check the provider."},
    onFailure={serviceStatus=it.message ?: "Invalid service URL"})
  }){Text("Save URL")}
  OutlinedButton(onClick={
   if(SecureAiGateway.configuredEndpoint(context).isBlank())serviceStatus="Save a service URL first."
   else {testing=true;serviceStatus="Contacting CARPE AI…";scope.launch{
    SecureAiGateway(context).ask("Reply with one short sentence confirming CARPE AI is responding.","",emptyList()).fold(
     onSuccess={serviceStatus="AI responding: $it"},
     onFailure={serviceStatus="AI test failed: ${it.message ?: "Unknown error"}"})
    testing=false
   }}
  },enabled=!testing){Text(if(testing)"Testing…" else "Test AI")}
 }
 if(serviceStatus.isNotBlank())Text(serviceStatus,color=Green)
 if(SecureAiGateway.configuredEndpoint(context).isNotBlank())TextButton(onClick={SecureAiGateway.clearEndpoint(context);endpointInput="";serviceStatus="Custom URL cleared."}){Text("Clear custom URL")}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("Build my AI profile");Text("Learn from what I tell CARPE. Stored locally and sent to Google only with my requests.",color=Color.DarkGray,fontSize=12.sp)};Switch(aiProfile,{aiProfile=it;profile.setEnabled(it)})}
 if(aiProfile){var profileText by remember{mutableStateOf(profile.summary())};Text("What CARPE remembers",fontWeight=FontWeight.Bold);Text(profileText,fontSize=13.sp);OutlinedButton(onClick={profile.clear();profileText=profile.summary()}){Text("Clear AI profile")}}
 HorizontalDivider()
 val goals=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content")
 goals.forEach{g->val k="goal_"+g.lowercase().replace(" ","_").replace("&","and");var on by remember{mutableStateOf(prefs.getBoolean(k,g=="More time offline"||g=="Focused work"))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(g,Modifier.weight(1f));Switch(on,{on=it;prefs.edit().putBoolean(k,it).apply()})}}
 HorizontalDivider();Text(a.todayMinutes().toString()+" minutes invested in deliberate actions",fontSize=22.sp,fontWeight=FontWeight.Bold)
 a.recent(8).forEach{Text("• "+it.title+" — "+it.minutes+" min")}
 Text("CARPE v"+BuildConfig.VERSION_NAME+" alpha",color=Green,fontWeight=FontWeight.Bold)
}
