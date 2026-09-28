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
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carpe.core.*

private val Cream=Color(0xFFF5F1E8); private val Green=Color(0xFF355E48)
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CarpeApp()}}}

private class ChatSession {
 val input=mutableStateOf("")
 val response=mutableStateOf("")
 val thinking=mutableStateOf(false)
 val lastIntent=mutableStateOf(CarpeIntent.UNKNOWN)
 val awaitingCookFollowup=mutableStateOf(false)
 val recipeQuery=mutableStateOf("")
 val actionLogged=mutableStateOf(false)
 val history=mutableStateListOf<AiTurn>()
}

@Composable fun CarpeApp(){
 val context=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember{context.getSharedPreferences("carpe",Context.MODE_PRIVATE)}
 val actions=remember{ActionStore(context)}; val usage=remember{UsageAccess(context)}
 val chat=remember{ChatSession()}; val chatScope=rememberCoroutineScope()
 var tab by remember{mutableIntStateOf(0)}; var refresh by remember{mutableIntStateOf(0)}
 MaterialTheme(colorScheme=lightColorScheme(primary=Green,background=Cream)){
  Scaffold(bottomBar={NavigationBar{listOf("Today","Coach","Focus","Shield","Me").forEachIndexed{i,n->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("☀","✦","◉","⬡","●")[i])},label={Text(n)})}}}){p->
   when(tab){
    0->Today(p,context,actions,{refresh++},{tab=4},{tab=2},chat,chatScope)
    1->Coach(p,prefs,usage,actions,refresh){type->
     when(type){
      "focus"->tab=2
      "cook"->{chat.input.value="Help me cook a meal with what I have";tab=0}
      "move"->{chat.input.value="Help me choose a movement I can start now";tab=0}
      "save"->{chat.input.value="Help me pause before a purchase";tab=0}
      else->tab=4
     }
    }
    2->Focus(p,actions){refresh++}
    3->Shield(p,context,usage)
    else->Me(p,prefs,actions,refresh)
   }
  }
 }
}
@Composable private fun Page(p:PaddingValues,title:String,sub:String,body:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("CARPE",color=Green,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text(title,fontSize=32.sp,fontWeight=FontWeight.Bold);Text(sub,color=Color.DarkGray);body();Spacer(Modifier.height(30.dp))}}
@Composable private fun Today(p:PaddingValues,c:Context,s:ActionStore,changed:()->Unit,openSettings:()->Unit,openFocus:()->Unit,chat:ChatSession,scope:CoroutineScope)=Page(p,"What matters now?","Type what you need or use your voice."){
 var input by chat.input; var response by chat.response; var thinking by chat.thinking
 var lastIntent by chat.lastIntent; var recipeQuery by chat.recipeQuery
 var awaitingCookFollowup by chat.awaitingCookFollowup
 var actionLogged by chat.actionLogged
 var showRecipeSearch by remember{mutableStateOf(false)}
 var recipeInput by remember{mutableStateOf("")}
 val gateway=remember{SecureAiGateway(c)}; val history=chat.history
 val profile=remember{UserProfileStore(c)}
 val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{input=it}}
 val router=remember{IntentRouter()}
 fun act(){val q=input.trim();if(q.isBlank()||thinking)return;profile.learn(q);val routed=router.classify(q)
  val isCookContinuation=routed.intent==CarpeIntent.UNKNOWN && awaitingCookFollowup
  val intent=LocalAssistant.resolveIntent(routed.intent,awaitingCookFollowup)
  lastIntent=intent
  // Keep one cooking follow-up in context. An unrelated later message should
  // return to normal routing instead of being silently forced into cooking.
  awaitingCookFollowup=intent==CarpeIntent.COOK && !isCookContinuation
  recipeQuery=if(intent==CarpeIntent.COOK) q else ""
  val local=LocalAssistant.reply(intent,q)
  response="";actionLogged=false;val prior=history.toList();history+=AiTurn("user",q);input=""
  while(history.size>20)history.removeAt(0)
  if(SecureAiGateway.configuredEndpoint(c).isBlank()){
   response="Cloud AI is not connected yet. Here's what CARPE can do locally:\n\n$local"
   history+=AiTurn("assistant",local)
   return
  }
  thinking=true
  scope.launch{gateway.ask(q,if(profile.enabled())profile.summary() else "",prior).fold(
   onSuccess={answer->response=answer;history+=AiTurn("assistant",answer)},
   onFailure={e->
    response="Cloud AI is unavailable (${e.message ?: "connection failed"}). Here's a local suggestion:\n\n$local"
    if(input.isBlank())input=q
    history+=AiTurn("assistant",local)
   }
  );thinking=false}
 }
 if(SecureAiGateway.configuredEndpoint(c).isBlank()) ElevatedCard {
  Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){
   Column(Modifier.weight(1f)){
    Text("Cloud AI offline",fontWeight=FontWeight.Bold)
    Text("Local guidance is ready.",fontSize=12.sp)
   }
   TextButton(onClick=openSettings){Text("Connect")}
  }
 }
 else Text("When you tap Send, your message, recent chat, and any AI profile you enabled go to the CARPE service and its AI provider.",color=Color.DarkGray,fontSize=12.sp)
 OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.fillMaxWidth().heightIn(min=88.dp),placeholder={Text("Ask CARPE anything…")},maxLines=6)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={try{voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT,"Talk to CARPE"))}catch(_:Exception){response="Voice recognition isn't available on this device."}},modifier=Modifier.weight(1f)){Text("🎤  Speak")};Button(onClick={act()},enabled=!thinking,modifier=Modifier.weight(1f)){Text(if(thinking)"Thinking…" else "Send")}}
 if(response.isNotBlank()) ElevatedCard{Text(response,Modifier.fillMaxWidth().padding(16.dp))}
 if(response.startsWith("Cloud AI is unavailable")) TextButton(onClick=openSettings){Text("Check AI connection")}
 if(response.isNotBlank()&&!actionLogged){
  val completed=when(lastIntent){
   CarpeIntent.COOK->Triple("cook","I cooked a meal",30)
   CarpeIntent.MOVE->Triple("move","I moved for 10 minutes",10)
   CarpeIntent.SPEND->Triple("save","I paused a purchase",5)
   else->null
  }
  if(completed!=null)OutlinedButton(onClick={
   s.add(completed.first,completed.second,completed.third);actionLogged=true;changed()
  }){Text(completed.second)}
 }
 if(recipeQuery.isNotBlank()) OutlinedButton(onClick={
  awaitingCookFollowup=false
  val url="https://www.google.com/search?q="+Uri.encode("recipes "+recipeQuery)
  runCatching{c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
 }){Text("Find recipes in browser")}
 Text("Suggestions",fontSize=18.sp,fontWeight=FontWeight.Bold)
 ActionCard("Cook something","Search recipes using ingredients, a meal idea, or a dietary need."){showRecipeSearch=!showRecipeSearch}
 if(showRecipeSearch){
  OutlinedTextField(value=recipeInput,onValueChange={recipeInput=it},modifier=Modifier.fillMaxWidth(),label={Text("Ingredients or meal")},placeholder={Text("eggs, spinach, 20 minutes")},singleLine=true)
  Button(onClick={
   awaitingCookFollowup=false
   val query="recipes "+recipeInput.trim().ifBlank{"easy dinner"}
   runCatching{c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(query))))}
  }){Text("Search recipes")}
 }
 ActionCard("Move your body","Walk, train, stretch, or get outside."){input="Help me move my body today"}
 ActionCard("Do meaningful work","Start a protected focus block."){openFocus()}
 ActionCard("Spend deliberately","Pause before a non-essential purchase."){input="Help me make a deliberate spending decision"}
 Text("CARPE counts completed offline actions, not time spent inside CARPE.",color=Green,fontWeight=FontWeight.Medium)
}
@Composable private fun ActionCard(t:String,d:String,on:()->Unit){ElevatedCard(onClick=on){Column(Modifier.fillMaxWidth().padding(18.dp)){Text(t,fontWeight=FontWeight.Bold,fontSize=18.sp);Text(d,color=Color.DarkGray)}}}
@Composable private fun Coach(p:PaddingValues,prefs:android.content.SharedPreferences,u:UsageAccess,a:ActionStore,r:Int,onAction:(String)->Unit)=Page(p,"CARPE intelligence","Recommendations use only the context you choose to provide. Device usage stays local in this alpha."){
 val names=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content")
 val goals=names.filter{prefs.getBoolean("goal_"+it.lowercase().replace(" ","_").replace("&","and"),it=="More time offline"||it=="Focused work")}
 val top=if(u.isGranted())u.last24Hours().take(12) else emptyList()
 val coachContext=androidx.compose.ui.platform.LocalContext.current
 val patterns=PatternEngine().findings(top)
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
  TextButton(onClick={onAction(s.actionType)}){Text(when(s.actionType){"focus"->"Start focus";"cook"->"Plan a meal";"move"->"Choose movement";"save"->"Review a purchase";else->"Set goals"})}
  var rated by remember(s.title){mutableStateOf(false)}
  if(!rated) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick={learning.recordHelpful(when(s.actionType){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;else->CarpeIntent.UNKNOWN},true);rated=true}){Text("Helpful")}
   TextButton(onClick={learning.recordHelpful(when(s.actionType){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;else->CarpeIntent.UNKNOWN},false);rated=true}){Text("Not helpful")}
  } else Text("Thanks. CARPE will use that locally.",fontSize=12.sp,color=Green)
 }}}
 Text("Why this is AI-assisted",fontWeight=FontWeight.Bold);Text("CARPE combines your explicit goals, your feedback, completed actions, and—only if you grant it—local app-usage patterns. The recommendation engine is designed to optimize for your stated life goals rather than engagement.")
}
@Composable private fun Focus(p:PaddingValues,a:ActionStore,changed:()->Unit)=Page(p,"Focus","A timer that is successful when you stop looking at CARPE."){
 val context=androidx.compose.ui.platform.LocalContext.current
 val session=remember(context){FocusSessionStore(context)}
 var running by remember{mutableStateOf(session.isActive())}
 var left by remember{mutableLongStateOf(if(running)session.remainingMillis() else 25*60_000L)}
 LaunchedEffect(running){
  while(running){
   if(session.finishIfDue(a)){running=false;left=0L;changed();break}
   left=session.remainingMillis()
   delay(1000)
  }
 }
 Text(String.format("%02d:%02d",left/60000,(left/1000)%60),fontSize=52.sp,fontWeight=FontWeight.Bold)
 Button(onClick={if(!running){session.start(25);running=true;left=session.remainingMillis()}else{session.stop();running=false;left=25*60_000L}},modifier=Modifier.fillMaxWidth()){Text(if(running)"Stop session" else "Start 25-minute focus")}
 Text("Put the phone down. CARPE will not send engagement prompts during the session.")
}
@Composable private fun Shield(p:PaddingValues,c:Context,u:UsageAccess)=Page(p,"Algorithm shield","See and reduce the signals that attention-harvesting systems use."){
 var permissionRefresh by remember{mutableIntStateOf(0)}
 val settingsLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){permissionRefresh++}
 // Recheck after returning from Android settings without requiring a tab switch.
 val granted=remember(permissionRefresh){u.isGranted()}
 ActionCard("Usage intelligence",if(granted)"Enabled. CARPE can analyze foreground app time locally." else "Optional. Tap to grant Android Usage Access."){if(!granted)settingsLauncher.launch(u.settingsIntent())}
 ActionCard("Notification intelligence","Grant CARPE notification access to measure which apps repeatedly compete for your attention."){settingsLauncher.launch(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))}
 ActionCard("Notification controls","Open Android notification settings to silence apps that pull you back."){c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.packageName))}
 ActionCard("Privacy dashboard","Review Android permissions granted to apps on this device."){try{c.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))}catch(_:Exception){}}
 val pressure=remember(c){NotificationPressure(c)}
 val noisyApps=pressure.topToday()
 if(noisyApps.isNotEmpty()){
  Text("Notifications competing for attention today",fontWeight=FontWeight.Bold)
  noisyApps.forEach{(pkg,count)->
   Card{Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){
    Column(Modifier.weight(1f)){Text(pkg.substringAfterLast('.'),fontWeight=FontWeight.Bold);Text("$count notifications",fontSize=12.sp)}
    TextButton(onClick={runCatching{c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,pkg))}}){Text("Review")}
   }}
  }
 }
 if(granted){
  val apps=u.last24Hours().take(12)
  val learning=remember{LearningStore(c)}
  var ratingsRevision by remember{mutableIntStateOf(0)}
  val ratings=remember(apps,ratingsRevision){apps.associate{it.packageName to learning.rating(it.packageName)}}
  val report=AttentionAnalyzer().analyze(apps,ratings)
  Text("Attention intelligence",fontWeight=FontWeight.Bold)
  Text(report.totalObservedMinutes.toString()+" foreground minutes observed locally.")
  report.signals.take(8).forEach{sig->
   Card{Column(Modifier.fillMaxWidth().padding(14.dp)){
    Text(sig.packageName.substringAfterLast('.'),fontWeight=FontWeight.Bold)
    Text("Attention-risk signal: "+sig.score+"/100 • "+sig.minutes+" min • "+pressure.today(sig.packageName)+" notifications today")
    if(sig.reasons.isNotEmpty()) Text(sig.reasons.joinToString(" • "),color=Color.DarkGray)
    Text("Your assessment: "+when(ratings[sig.packageName]){1->"Pulls me away";3->"Mixed";5->"Helps me";else->"Not rated"},fontSize=12.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
     listOf(1 to "Pulls me away",3 to "Mixed",5 to "Helps me").forEach{(rating,label)->
      TextButton(onClick={learning.rate(sig.packageName,rating);ratingsRevision++}){Text(label,fontSize=11.sp)}
     }
    }
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
 Text("Cloud AI uses a CARPE service URL. Your message, recent chat, and any AI profile you enabled are sent to that service and its AI provider when you tap Send. Never enter an AI key here.",color=Color.DarkGray,fontSize=13.sp)
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
 var confirmErase by remember{mutableStateOf(false)}
 OutlinedButton(onClick={confirmErase=true}){Text("Erase CARPE's local data")}
 if(confirmErase) AlertDialog(
  onDismissRequest={confirmErase=false},
  title={Text("Erase local data?")},
  text={Text("This removes your goals, ratings, action history, AI profile, service URL, and focus session from this device. Android permissions remain managed in system settings.")},
  confirmButton={TextButton(onClick={
   listOf("carpe","carpe_actions","carpe_ai_profile","carpe_ai_service","behavior_history","notification_pressure","carpe_learning","carpe_focus").forEach{name->
    context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit()
   }
   confirmErase=false
   (context as? android.app.Activity)?.recreate()
  }){Text("Erase data")}},
  dismissButton={TextButton(onClick={confirmErase=false}){Text("Cancel")}}
 )
 Text("CARPE v"+BuildConfig.VERSION_NAME+" alpha",color=Green,fontWeight=FontWeight.Bold)
}
