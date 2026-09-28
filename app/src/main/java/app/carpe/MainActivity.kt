package app.carpe

import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.usage.UsageStatsManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carpe.core.*

private val WarmWhite=Color(0xFFFFFBF8)
private val White=Color(0xFFFFFFFF)
private val Orange=Color(0xFFB54800)
private val BrightOrange=Color(0xFFF47721)
private val Peach=Color(0xFFFFEFE3)
private val Ink=Color(0xFF292522)
private val Muted=Color(0xFF6D625B)
private val Line=Color(0xFFE9DED6)
class MainActivity:ComponentActivity(){
 private var updateCheckStarted=false
 private var pendingUpdate:CarpeUpdate?=null
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CarpeApp()}}
 override fun onResume(){
  super.onResume()
  if(pendingUpdate!=null && Build.VERSION.SDK_INT>=26 && packageManager.canRequestPackageInstalls()){
   val update=pendingUpdate
   pendingUpdate=null
   if(update!=null) downloadUpdate(update)
  }else if(!updateCheckStarted && getSharedPreferences("carpe",Context.MODE_PRIVATE).getBoolean("welcome_complete",false)){
   updateCheckStarted=true
   lifecycleScope.launch {
    val update=CarpeUpdates.checkForUpdate(BuildConfig.VERSION_NAME)
    if(update!=null && !isFinishing && !isDestroyed){
     val updatePrefs=getSharedPreferences("carpe_updates",Context.MODE_PRIVATE)
     if(System.currentTimeMillis()>=updatePrefs.getLong("snooze_until",0L)) offerUpdate(update)
    }
   }
  }
 }
 private fun offerUpdate(update:CarpeUpdate){
  AlertDialog.Builder(this)
   .setTitle("CARPE "+update.version+" is available")
   .setMessage("CARPE will download the verified update. Android will ask you to approve installation. You can keep using the app if you choose Later.")
   .setPositiveButton("Update"){_,_->prepareUpdate(update)}
   .setNegativeButton("Later"){_,_->getSharedPreferences("carpe_updates",Context.MODE_PRIVATE).edit().putLong("snooze_until",System.currentTimeMillis()+24L*60L*60L*1000L).apply()}
   .show()
 }
 private fun prepareUpdate(update:CarpeUpdate){
  if(Build.VERSION.SDK_INT>=26 && !packageManager.canRequestPackageInstalls()){
   AlertDialog.Builder(this)
    .setTitle("Allow CARPE updates")
    .setMessage("Android needs your approval to let CARPE open the system installer. CARPE cannot install silently.")
    .setPositiveButton("Open Android settings"){_,_->
     pendingUpdate=update
     startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+packageName)))
    }
    .setNegativeButton("Cancel",null)
    .show()
   return
  }
  downloadUpdate(update)
 }
 private fun downloadUpdate(update:CarpeUpdate){
  val progress=ProgressDialog(this).apply{setMessage("Downloading and checking CARPE update…");setCancelable(false);show()}
  lifecycleScope.launch {
   try{
    val apk=CarpeUpdates.downloadAndVerify(this@MainActivity,update)
    startActivity(CarpeUpdates.installerIntent(this@MainActivity,apk))
   }catch(error:Exception){
    if(!isFinishing && !isDestroyed) AlertDialog.Builder(this@MainActivity)
     .setTitle("Update not installed")
     .setMessage(error.message?: "CARPE could not download this update. The current app and its data were left in place.")
     .setPositiveButton("OK",null)
     .show()
   }finally{if(progress.isShowing)progress.dismiss()}
  }
 }
}

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
 var showWelcome by rememberSaveable{mutableStateOf(!prefs.getBoolean("welcome_complete",false))}
 var tab by remember{mutableIntStateOf(0)}; var refresh by remember{mutableIntStateOf(0)}
 MaterialTheme(colorScheme=lightColorScheme(primary=Orange,onPrimary=White,secondary=BrightOrange,background=WarmWhite,onBackground=Ink,surface=White,onSurface=Ink,surfaceVariant=Peach,onSurfaceVariant=Ink,outline=Line)){
  if(showWelcome){
   WelcomeFlow(onFinish={destination->
    prefs.edit().putBoolean("welcome_complete",true).apply()
    when(destination){
     "focus"->tab=2
     "mirror"->tab=3
     "goals"->tab=4
     "offline"->{chat.input.value="Help me choose one offline activity that fits my time and energy.";tab=0}
     else->tab=0
    }
    showWelcome=false
   })
  }else{
  Scaffold(containerColor=WarmWhite,bottomBar={NavigationBar(containerColor=White,tonalElevation=2.dp){
   val labels=listOf("Today","Coach","Focus","Mirror","Me")
   val icons=listOf(Icons.Filled.Today,Icons.Filled.AutoAwesome,Icons.Filled.CenterFocusStrong,Icons.Filled.Visibility,Icons.Filled.Person)
   labels.forEachIndexed{i,n->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],contentDescription=n)},label={Text(n)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Orange,selectedTextColor=Orange,indicatorColor=Peach,unselectedIconColor=Muted,unselectedTextColor=Muted))}
  }}){p->
   when(tab){
    0->Today(p,context,actions,{refresh++},{tab=4},{tab=2},chat,chatScope)
    1->Coach(p,prefs,usage,actions,refresh,onAction={type->
     when(type){
      "focus"->tab=2
      "offline"->{chat.input.value="Help me choose one offline activity that fits my time and energy. Offer a few options without assuming screen use is bad.";tab=0}
      "reset"->{chat.input.value="Help me make a short, nonjudgmental pause from technology and choose what I want to do next.";tab=0}
      "cook"->{chat.input.value="Help me cook a meal with what I have";tab=0}
      "move"->{chat.input.value="Help me choose a movement I can start now";tab=0}
      "save"->{chat.input.value="Help me pause before a purchase";tab=0}
      "content_plan"->{chat.input.value="Help me make a private, nonjudgmental plan to reduce porn use. Do not ask me to share browsing or viewing history. Offer a few offline alternatives and let me decide.";tab=0}
      "perspectives"->{chat.input.value="Help me examine an issue from multiple political viewpoints. Present strong good-faith arguments and distinguish facts from uncertainty. Do not steer me to a conclusion.";tab=0}
      else->tab=4
     }
    },onPlanGoal={title->chat.input.value=GoalStepPlanner.prompt(title);chat.response.value="";tab=0},onGoalLogged={refresh++})
    2->Focus(p,actions){refresh++}
    3->Mirror(p,context,usage)
    else->Me(p,prefs,actions,refresh,onWelcome={showWelcome=true}){refresh++}
   }
  }
 }
}
}
@Composable private fun Page(p:PaddingValues,title:String,sub:String,body:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(horizontal=20.dp, vertical=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("CARPE",color=Orange,fontWeight=FontWeight.Bold,letterSpacing=3.sp,fontSize=13.sp);Text(title,fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink);Text(sub,color=Muted,fontSize=15.sp,lineHeight=21.sp);body();Spacer(Modifier.height(30.dp))}}
@Composable private fun Today(p:PaddingValues,c:Context,s:ActionStore,changed:()->Unit,openSettings:()->Unit,openFocus:()->Unit,chat:ChatSession,scope:CoroutineScope)=Page(p,"What matters now?","Type what you need or use your voice."){
 var input by chat.input; var response by chat.response; var thinking by chat.thinking
 var lastIntent by chat.lastIntent; var recipeQuery by chat.recipeQuery
 var awaitingCookFollowup by chat.awaitingCookFollowup
 var actionLogged by chat.actionLogged
 var showRecipeSearch by remember{mutableStateOf(false)}
 var recipeInput by remember{mutableStateOf("")}
 val gateway=remember{SecureAiGateway(c)}; val history=chat.history
 val directKeyStore=remember{AiStudioKeyStore(c)}
 val directProvider=remember{DirectGeminiProvider(directKeyStore)}
 val profile=remember{UserProfileStore(c)}
 val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{input=it}}
 val router=remember{IntentRouter()}
 val localRecipes=remember(recipeQuery){RecipeCatalog.search(recipeQuery)}
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
  if(!directKeyStore.hasKey() && SecureAiGateway.configuredEndpoint(c).isBlank()){
   response="Cloud AI is not connected yet. Here's what CARPE can do locally:\n\n$local"
   history+=AiTurn("assistant",local)
   return
  }
  thinking=true
  val provider=if(directKeyStore.hasKey())directProvider else gateway
  scope.launch{provider.ask(q,if(profile.enabled())profile.summary() else "",prior).fold(
   onSuccess={answer->response=answer;history+=AiTurn("assistant",answer)},
   onFailure={e->
    response="Cloud AI is unavailable (${e.message ?: "connection failed"}). Here's a local suggestion:\n\n$local"
    if(input.isBlank())input=q
    history+=AiTurn("assistant",local)
   }
  );thinking=false}
 }
 OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.fillMaxWidth().heightIn(min=88.dp),placeholder={Text("Ask CARPE anything…")},maxLines=6)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={try{voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT,"Talk to CARPE"))}catch(_:Exception){response="Voice recognition isn't available on this device."}},modifier=Modifier.weight(1f)){Text("Speak")};Button(onClick={act()},enabled=!thinking,modifier=Modifier.weight(1f)){Text(if(thinking)"Thinking…" else "Send")}}
 Card(colors=CardDefaults.cardColors(containerColor=Peach)){Row(Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(!directKeyStore.hasKey() && SecureAiGateway.configuredEndpoint(c).isBlank())"AI is optional" else "AI connection ready",fontWeight=FontWeight.SemiBold,color=Ink,fontSize=13.sp);Text(if(!directKeyStore.hasKey() && SecureAiGateway.configuredEndpoint(c).isBlank())"Local guidance works without connecting." else "Only the current request is sent when you choose Send.",fontSize=12.sp,color=Muted)};TextButton(onClick=openSettings){Text(if(!directKeyStore.hasKey() && SecureAiGateway.configuredEndpoint(c).isBlank())"Connect" else "Details")}}}
 if(directKeyStore.hasKey()) Text("Google AI Studio may use free-tier prompts to improve Google products. Avoid sensitive details. Earlier chat turns and captured screen samples are never included.",color=Muted,fontSize=12.sp)
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
 if(recipeQuery.isNotBlank()){
  Text("Recipe ideas matched on this device",fontWeight=FontWeight.Bold,fontSize=18.sp)
  if(localRecipes.isEmpty())Text("No close match yet. Try one main ingredient, or broaden the time and diet terms.",color=Muted)
  localRecipes.forEach{recipe->
   var expanded by remember(recipe.id){mutableStateOf(false)}
   Card(colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text(recipe.title,fontWeight=FontWeight.Bold,fontSize=17.sp,color=Ink)
    Text(recipe.minutes.toString()+" min • "+(if(recipe.budgetTier==1)"Budget-friendly" else "Everyday"),color=Orange,fontSize=13.sp)
    Text("Ingredients: "+recipe.ingredients.joinToString(", "),color=Muted,fontSize=13.sp)
    if(expanded)recipe.steps.forEachIndexed{index,step->Text((index+1).toString()+". "+step,color=Ink,fontSize=13.sp)}
    OutlinedButton(onClick={expanded=!expanded}){Text(if(expanded)"Hide steps" else "Show steps")}
   }}
  }
  Text("Time is approximate. Check ingredient labels for allergies or dietary needs; CARPE does not verify brands or cross-contact.",color=Muted,fontSize=12.sp)
 }
 Text("Suggestions",fontSize=18.sp,fontWeight=FontWeight.Bold)
 ActionCard("Cook something","Search recipes using ingredients, a meal idea, or a dietary need."){showRecipeSearch=!showRecipeSearch}
 if(showRecipeSearch){
  OutlinedTextField(value=recipeInput,onValueChange={recipeInput=it},modifier=Modifier.fillMaxWidth(),label={Text("Ingredients or meal")},placeholder={Text("eggs, spinach, 20 minutes")},singleLine=true)
  Button(onClick={
   awaitingCookFollowup=false
   recipeQuery=recipeInput.trim().ifBlank{"quick dinner"}
  }){Text("Find ideas in CARPE")}
 }
 ActionCard("Move your body","Walk, train, stretch, or get outside."){input="Help me move my body today"}
 ActionCard("Do meaningful work","Start a protected focus block."){openFocus()}
 ActionCard("Spend deliberately","Pause before a non-essential purchase."){input="Help me make a deliberate spending decision"}
 Text("CARPE counts completed offline actions, not time spent inside CARPE.",color=Orange,fontWeight=FontWeight.Medium)
}
@Composable private fun ActionCard(t:String,d:String,on:()->Unit){ElevatedCard(onClick=on,colors=CardDefaults.elevatedCardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(18.dp)){Text(t,fontWeight=FontWeight.Bold,fontSize=18.sp,color=Ink);Text(d,color=Muted,lineHeight=20.sp)}}}
@Composable private fun Coach(p:PaddingValues,prefs:android.content.SharedPreferences,u:UsageAccess,a:ActionStore,r:Int,onAction:(String)->Unit,onPlanGoal:(String)->Unit,onGoalLogged:()->Unit)=Page(p,"CARPE intelligence","Recommendations use only the context you choose to provide. Device usage stays local in this alpha."){
 val names=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content","Reduce porn use","Think across political viewpoints")
 val goals=names.filter{prefs.getBoolean("goal_"+it.lowercase().replace(" ","_").replace("&","and"),it=="More time offline"||it=="Focused work")}
 val hasUsageAccess=u.isGranted()
 val top by produceState(initialValue=emptyList<AppUsage>(),key1=r,key2=hasUsageAccess){
  value=if(hasUsageAccess)runCatching{withContext(Dispatchers.IO){u.last24Hours().take(12)}}.getOrDefault(emptyList()) else emptyList()
 }
 val coachContext=androidx.compose.ui.platform.LocalContext.current
  val goalStore=remember(coachContext){UserGoalStore(coachContext)}
  val userGoals=remember(r){goalStore.goals()}
  val patterns=PatternEngine().findings(top)
  val learning=remember(coachContext){LearningStore(coachContext)}
  val ratings=top.associate{it.packageName to learning.rating(it.packageName)}
  val intentions=top.associate{it.packageName to learning.intentional(it.packageName)}
  val suggestions=AiCoach().suggest(CoachContext(a.todayMinutes(),goals,top,a.recent(),ratings,intentions),learning)
 Text("What CARPE is noticing",fontWeight=FontWeight.Bold,fontSize=20.sp)
 patterns.forEach{finding->
  ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp)){
   Text(finding.title,fontWeight=FontWeight.Bold)
   Text(finding.evidence)
   Text("Confidence: "+finding.confidence,color=Orange)
   Text("Try: "+finding.nextStep,color=Color.DarkGray)
  }}
 }
 Text("Suggested next moves",fontWeight=FontWeight.Bold,fontSize=20.sp)
 suggestions.forEach{s->Card{Column(Modifier.fillMaxWidth().padding(18.dp)){
  Text(s.title,fontWeight=FontWeight.Bold);Text(s.reason);Text("Optional starting point: "+s.minutes+" min",color=Orange)
  Text("Based on the goal you chose and any local assessment you gave. This suggestion is generated on your device.",fontSize=12.sp,color=Color.DarkGray)
  TextButton(onClick={onAction(s.actionType)}){Text(when(s.actionType){"focus"->"Start focus";"cook"->"Plan a meal";"move"->"Choose movement";"save"->"Review a purchase";"offline"->"Choose an offline activity";"reset"->"Plan a pause";"content_plan"->"Make a private plan";"perspectives"->"Explore perspectives";else->"Set goals"})}
  var rated by remember(s.title){mutableStateOf(false)}
  if(!rated) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick={learning.recordHelpful(when(s.actionType){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;"offline"->CarpeIntent.OFFLINE_ACTIVITY;"content_plan","perspectives","reset"->CarpeIntent.REFLECT;else->CarpeIntent.UNKNOWN},true);rated=true}){Text("Helpful")}
   TextButton(onClick={learning.recordHelpful(when(s.actionType){"cook"->CarpeIntent.COOK;"focus"->CarpeIntent.FOCUS;"move"->CarpeIntent.MOVE;"save"->CarpeIntent.SPEND;"offline"->CarpeIntent.OFFLINE_ACTIVITY;"content_plan","perspectives","reset"->CarpeIntent.REFLECT;else->CarpeIntent.UNKNOWN},false);rated=true}){Text("Not helpful")}
  } else Text("Thanks. CARPE will use that locally.",fontSize=12.sp,color=Orange)
 }}}
 if(userGoals.isNotEmpty()){
  Text("Your goals",fontWeight=FontWeight.Bold,fontSize=20.sp)
  userGoals.forEach{goal->
   val count=GoalProgress.countWithinDays(goalStore.checkIns(goal.id),7,System.currentTimeMillis())
   Card(colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
    Text(goal.title,fontWeight=FontWeight.SemiBold)
    Text("$count of ${goal.weeklyTarget} planned steps in the last 7 days. This is your check-in, not a measure of your worth.",color=Muted,fontSize=13.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     OutlinedButton(onClick={onPlanGoal(goal.title)}){Text("Plan a next step")}
     TextButton(onClick={goalStore.checkIn(goal.id);onGoalLogged()}){Text("Log a step")}
    }
   }}
  }
 }
 Text("How these suggestions work",fontWeight=FontWeight.Bold);Text("These are local rules, not AI-generated advice. They use your selected goals, private action history, and feedback. Usage observations are considered only if you grant Usage Access or rate an app. Coach context stays on this device; AI in Today is separate and sends a request only when you choose Send.")
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
@Composable private fun Mirror(p:PaddingValues,c:Context,u:UsageAccess)=Page(p,"Algorithm mirror","Inspect visible feed cues, understand what CARPE can observe, and choose what to do next."){
 val auditStore=remember(c){ScreenAuditStore(c)}
 val aiKeyStore=remember(c){AiStudioKeyStore(c)}
 val directAi=remember(c){DirectGeminiProvider(aiKeyStore)}
 val aiGateway=remember(c){SecureAiGateway(c)}
 val aiScope=rememberCoroutineScope()
 var aiAnalysis by remember{mutableStateOf("")}
 var aiAnalyzing by remember{mutableStateOf(false)}
 var auditRefresh by remember{mutableIntStateOf(0)}
 var auditSourceLabel by remember(c){mutableStateOf(auditStore.sourceLabel())}
 var showAuditLabel by remember{mutableStateOf(auditStore.sourceLabel().isNotBlank())}
 val isAuditing=remember(auditRefresh){auditStore.isActive()}
 val auditReport=remember(auditRefresh){auditStore.report()}
 LaunchedEffect(auditReport?.completedAt){aiAnalysis=""}
 val auditHistory=remember(auditRefresh){auditStore.history()}
 val auditComparison=remember(auditHistory){ScreenAuditComparison.between(auditHistory)}
 var compareSamples by remember(auditRefresh){mutableStateOf(auditStore.historyEnabled())}
 val auditError=remember(auditRefresh){auditStore.error()}
 var auditPending by remember{mutableStateOf(false)}
 LaunchedEffect(isAuditing,auditPending){
  if(!isAuditing&&!auditPending)return@LaunchedEffect
  while(true){
   delay(1000)
   auditRefresh++
   val activeNow=auditStore.isActive()
   if(auditPending&&(activeNow||auditStore.error().isNotBlank())){
    auditPending=false
    break
   }
   if(!activeNow&&!auditPending)break
  }
 }
 val projectionManager=remember(c){c.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager}
 val projectionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  val data=result.data
  if(result.resultCode==android.app.Activity.RESULT_OK&&data!=null){
   val serviceIntent=Intent(c,ScreenAuditService::class.java).putExtra(ScreenAuditIntents.EXTRA_RESULT_CODE,result.resultCode).putExtra(ScreenAuditIntents.EXTRA_RESULT_DATA,data)
   runCatching{if(Build.VERSION.SDK_INT>=26)c.startForegroundService(serviceIntent)else c.startService(serviceIntent)}
    .onFailure{auditStore.setError("Screen audit could not start: ${it.message ?: "try again"}");auditRefresh++}
  }else{auditStore.setError("Screen sharing was canceled. Nothing was captured.");auditRefresh++}
 }
 val startProjection={projectionLauncher.launch(projectionManager.createScreenCaptureIntent())}
 val notificationLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  if(granted)startProjection() else {auditStore.setError("Allow notifications so Android can show the active session and Stop control.");auditRefresh++}
 }
 Text("Visible feed sample",fontWeight=FontWeight.Bold,fontSize=21.sp,color=Ink)
 Card(colors=CardDefaults.cardColors(containerColor=White)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Column(Modifier.weight(1f)){
   Text("Compare recent samples",fontWeight=FontWeight.SemiBold,color=Ink)
   Text("Optional. Keeps up to 5 aggregate summaries on this device; no images or recognized words. Turning this off stops new saves; clear saved summaries below.",fontSize=12.sp,color=Muted,lineHeight=16.sp)
  }
  Switch(checked=compareSamples,onCheckedChange={enabled->compareSamples=enabled;auditStore.setHistoryEnabled(enabled);auditRefresh++})
 }}
 Card(colors=CardDefaults.cardColors(containerColor=Peach)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("A short, optional check of what appears on your screen",fontWeight=FontWeight.SemiBold)
  Text("Choose one app if available; older Android may share the whole screen. Up to 2 minutes, on-device only. Images and recognized words are discarded, never sent to AI.",color=Muted,fontSize=13.sp,lineHeight=18.sp)
  if(showAuditLabel)OutlinedTextField(value=auditSourceLabel,onValueChange={auditSourceLabel=it.take(60)},modifier=Modifier.fillMaxWidth(),label={Text("App/feed label (optional)")},singleLine=true)
  else TextButton(onClick={showAuditLabel=true}){Text("Add an app label (optional)")}
  if(isAuditing){
   Button(onClick={c.startService(Intent(c,ScreenAuditService::class.java).setAction(ScreenAuditIntents.ACTION_STOP))},modifier=Modifier.fillMaxWidth()){Text("Stop screen audit")}
   Text("Audit active • samples are processed locally",color=Orange,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
  }else Button(onClick={
   auditStore.setSourceLabel(auditSourceLabel)
   auditStore.setError(null)
   auditPending=true
   if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
   else startProjection()
  },modifier=Modifier.fillMaxWidth()){Text("Start 2-minute audit")}
  if(auditError.isNotBlank())Text(auditError,color=Orange,fontSize=13.sp)
 }}
 if(auditReport!=null){
  Text(if(auditReport.sourceLabel.isBlank())"What was visible" else "What was visible in ${auditReport.sourceLabel}",fontWeight=FontWeight.Bold,fontSize=19.sp)
  Card(colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
   Text("${auditReport.sampledScreens} readable screens sampled",fontWeight=FontWeight.SemiBold)
   Text("Ad or sponsored labels: ${cueSummary(auditReport,auditReport.adLabelScreens)}")
   Text("Recommendation labels: ${cueSummary(auditReport,auditReport.recommendationLabelScreens)}")
   Text("Continue or autoplay prompts: ${cueSummary(auditReport,auditReport.continuePromptScreens)}")
   Text("Screens with similar visible text: ${cueSummary(auditReport,auditReport.similarScreens)}")
   Text("These are counts from this short sample, not the full feed or the app’s internal ranking. A visible label is evidence of wording on screen; similarity can also come from repeated interface text. CARPE does not keep the words it reads.",fontSize=12.sp,color=Muted)
   TextButton(onClick={auditStore.clearReports();auditRefresh++},enabled=!isAuditing){Text("Clear local audit summaries")}
  }}
  Card(colors=CardDefaults.cardColors(containerColor=Peach)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("Optional AI interpretation",fontWeight=FontWeight.SemiBold)
   Text("Only the aggregate counts above are sent when you tap. No screenshots, recognized words, or app/feed label are included. The sample cannot reveal an app's internal ranking.",fontSize=12.sp,color=Muted,lineHeight=17.sp)
   Button(onClick={
    val connected=aiKeyStore.hasKey()||SecureAiGateway.configuredEndpoint(c).isNotBlank()
    if(!connected){aiAnalysis="Connect AI in Me → AI & privacy to use this optional explanation."}
    else{
     aiAnalyzing=true
     aiAnalysis=""
     val prompt=ScreenAuditAiPrompt.build(auditReport)
     aiScope.launch{
      val provider=if(aiKeyStore.hasKey())directAi else aiGateway
      provider.ask(prompt,"",emptyList()).fold(
       onSuccess={answer->if(auditStore.report()?.completedAt==auditReport.completedAt)aiAnalysis=answer},
       onFailure={aiAnalysis="CARPE couldn't get an AI interpretation: ${it.message ?: "connection failed"}"}
      )
      aiAnalyzing=false
     }
    }
   },enabled=!isAuditing&&!aiAnalyzing){Text(if(aiAnalyzing)"Analyzing…" else "Ask AI to interpret these counts")}
  }}
  if(aiAnalysis.isNotBlank())Card(colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text("AI interpretation",fontWeight=FontWeight.SemiBold)
   Text(aiAnalysis,color=Ink)
  }}
 }
 if(compareSamples && auditComparison!=null){
  Text("Latest two samples for this label",fontWeight=FontWeight.Bold,fontSize=19.sp,color=Ink)
  Card(colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text(if(auditComparison.latest.sourceLabel.isBlank())"Unlabeled samples" else auditComparison.latest.sourceLabel,fontWeight=FontWeight.SemiBold)
   auditComparison.cueRates().forEach{rate->
    val earlier=rate.earlierPercent
    val latest=rate.latestPercent
    Text(rate.label+": "+(earlier?.let{"$it%"} ?: "no data")+" → "+(latest?.let{"$it%"} ?: "no data"),fontSize=14.sp)
   }
   Text("Percentages are shares of readable screens in two short samples. Differences can reflect what happened to be on screen; they do not show why an app ranked content or represent its full feed.",fontSize=12.sp,color=Muted,lineHeight=17.sp)
  }}
 }
 var permissionRefresh by remember{mutableIntStateOf(0)}
 val settingsLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){permissionRefresh++}
 // Recheck after returning from Android settings without requiring a tab switch.
 val granted=remember(permissionRefresh){u.isGranted()}
 ActionCard("Usage intelligence",if(granted)"Enabled. CARPE can analyze foreground app time locally." else "Optional. Tap to grant Android Usage Access."){if(!granted)settingsLauncher.launch(u.settingsIntent())}
 ActionCard("Notification intelligence","Optional. Android grants broad notification access. CARPE reads only the posting app name and stores daily counts; it does not read or save notification text. Enable it only if this insight is useful."){settingsLauncher.launch(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))}
 ActionCard("Notification controls","Open Android notification settings to silence apps that pull you back."){c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.packageName))}
 ActionCard("Privacy dashboard","Open Android's privacy settings. CARPE cannot revoke other apps' permissions or stop their data collection for you."){try{c.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))}catch(_:Exception){}}
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
  val intentions=remember(apps,ratingsRevision){apps.associate{it.packageName to learning.intentional(it.packageName)}}
  val report=AttentionAnalyzer().analyze(apps,ratings,intentions)
  Text("Attention intelligence",fontWeight=FontWeight.Bold)
  Text(report.totalObservedMinutes.toString()+" foreground minutes observed locally.")
  report.signals.take(8).forEach{sig->
   Card{Column(Modifier.fillMaxWidth().padding(14.dp)){
    Text(sig.packageName.substringAfterLast('.'),fontWeight=FontWeight.Bold)
    Text("Observed: "+sig.minutes+" foreground min in the last 24 hours • "+pressure.today(sig.packageName)+" notifications today")
    Text(sig.assessment.label,fontWeight=FontWeight.SemiBold,color=Orange)
    if(sig.reasons.isNotEmpty()) Text(sig.reasons.joinToString(" • "),color=Color.DarkGray)
    Text("Time and notification counts are observations, not evidence of harm. CARPE does not diagnose addiction; your assessment guides the interpretation.",fontSize=12.sp,color=Muted)
    Text("Your assessment: "+when(ratings[sig.packageName]){in 1..2->"Pulls me away";3->"Mixed";in 4..5->"Helps me";else->"Not rated"},fontSize=12.sp)
    val intention=learning.intentional(sig.packageName)
    Text("Did this time match your intention? "+when(intention){true->"Yes";false->"No";else->"Not rated"},fontSize=12.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
     listOf(true to "Matched",false to "Did not match").forEach{(matched,label)->
      TextButton(onClick={learning.setIntentional(sig.packageName,matched);ratingsRevision++}){Text(label,fontSize=11.sp)}
     }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
     listOf(1 to "Pulls me away",3 to "Mixed",5 to "Helps me").forEach{(rating,label)->
      TextButton(onClick={learning.rate(sig.packageName,rating);ratingsRevision++}){Text(label,fontSize=11.sp)}
     }
     if(intention!=null||ratings[sig.packageName]!=null)TextButton(onClick={learning.clearAssessment(sig.packageName);ratingsRevision++}){Text("Clear feedback",fontSize=11.sp)}
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
 Text("CARPE does not require these permissions. Granting them should add insight, never unlock basic usefulness.",color=Orange)
}
private fun cueSummary(report:ScreenAuditReport,count:Int):String =
 "${count.coerceIn(0,report.sampledScreens)} of ${report.sampledScreens} sampled screens" +
  (report.percentOfSamples(count)?.let{" ($it%)"} ?: "")
@Composable private fun Me(p:PaddingValues,prefs:android.content.SharedPreferences,a:ActionStore,r:Int,onWelcome:()->Unit,changed:()->Unit)=Page(p,"Your life, not a feed","Set what CARPE should optimize for and review what you actually did."){
 val context=androidx.compose.ui.platform.LocalContext.current
 val profile=remember{UserProfileStore(context)}
 var aiProfile by remember{mutableStateOf(profile.enabled())}
 val keyStore=remember{AiStudioKeyStore(context)}
 val directProvider=remember{DirectGeminiProvider(keyStore)}
 var directConfigured by remember{mutableStateOf(keyStore.hasKey())}
 var apiKeyInput by remember{mutableStateOf("")}
 var endpointInput by remember{mutableStateOf(SecureAiGateway.configuredEndpoint(context))}
 var serviceTokenInput by remember{mutableStateOf("")}
 var serviceTokenConfigured by remember{mutableStateOf(SecureAiGateway.hasAccessToken(context,endpointInput))}
 var serviceStatus by remember{mutableStateOf("")}
 var testing by remember{mutableStateOf(false)}
 var showAiDetails by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 OutlinedButton(onClick=onWelcome){Text("Revisit welcome & check-in")}
 Text("Your own goals",fontWeight=FontWeight.Bold,fontSize=20.sp)
 Text("Choose a goal that matters to you. Check-ins stay on this device; CARPE uses no streaks, reminders, or leaderboard.",color=Muted,fontSize=13.sp,lineHeight=18.sp)
 val userGoalStore=remember(context){UserGoalStore(context)}
 var userGoals by remember(r){mutableStateOf(userGoalStore.goals())}
 var goalTitle by remember{mutableStateOf("")}
 var goalTarget by remember{mutableStateOf("3")}
 OutlinedTextField(value=goalTitle,onValueChange={goalTitle=it.take(100)},modifier=Modifier.fillMaxWidth(),label={Text("A goal in your own words")},placeholder={Text("e.g. Walk after dinner")},singleLine=true)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
  OutlinedTextField(value=goalTarget,onValueChange={goalTarget=it.filter(Char::isDigit).take(1)},modifier=Modifier.width(110.dp),label={Text("Times / week")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
  Button(onClick={
   val added=userGoalStore.addGoal(goalTitle,goalTarget.toIntOrNull()?:3)
   if(added!=null){goalTitle="";userGoals=userGoalStore.goals();changed()}
  },enabled=goalTitle.isNotBlank()){Text("Add goal")}
 }
 userGoals.forEach{goal->
  val count=GoalProgress.countWithinDays(userGoalStore.checkIns(goal.id),7,System.currentTimeMillis())
  Card(colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text(goal.title,fontWeight=FontWeight.SemiBold,fontSize=16.sp,color=Ink)
   Text("$count of ${goal.weeklyTarget} planned check-ins in the last 7 days",color=Muted,fontSize=13.sp)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
    Button(onClick={userGoalStore.checkIn(goal.id);changed()}){Text("Log a step")}
    TextButton(onClick={userGoalStore.removeGoal(goal.id);userGoals=userGoalStore.goals();changed()}){Text("Remove")}
   }
  }}
 }
 HorizontalDivider()
 Text("AI & privacy",fontWeight=FontWeight.Bold,fontSize=20.sp)
 Card(colors=CardDefaults.cardColors(containerColor=Peach)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text("AI is optional. Local guidance works without a connection.",fontWeight=FontWeight.SemiBold)
  Text("If you connect Gemini, CARPE sends your request only when you tap Send.",color=Muted,fontSize=13.sp)
  TextButton(onClick={showAiDetails=!showAiDetails}){Text(if(showAiDetails)"Hide data details" else "How AI data is handled")}
  if(showAiDetails)Text("Only the current request and optional saved profile preferences are sent. Earlier chat turns and screen-audit samples stay on this device. Google AI Studio free-tier prompts may be used to improve Google products; avoid sensitive details. Keys stored in a mobile app can still be extracted, so this direct option is for private testing.",color=Muted,fontSize=13.sp,lineHeight=18.sp)
 }}
 OutlinedButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://aistudio.google.com/app/apikey")))}.onFailure{serviceStatus="Couldn't open AI Studio."}}){Text("Get a Gemini API key in AI Studio")}
 OutlinedTextField(value=apiKeyInput,onValueChange={apiKeyInput=it},modifier=Modifier.fillMaxWidth(),label={Text(if(directConfigured)"Replace Gemini API key" else "Gemini API key")},placeholder={Text("Paste your AI Studio key")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Button(onClick={runCatching{keyStore.saveKey(apiKeyInput)}.fold(onSuccess={directConfigured=true;apiKeyInput="";serviceStatus="Gemini key saved on this device."},onFailure={serviceStatus=it.message ?: "Couldn't save Gemini key."})},enabled=apiKeyInput.isNotBlank()){Text("Save Gemini key")}
  if(directConfigured)TextButton(onClick={keyStore.clear();directConfigured=false;serviceStatus="Gemini key cleared from this device."}){Text("Clear key")}
 }
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  OutlinedButton(onClick={
   if(!directConfigured && SecureAiGateway.configuredEndpoint(context).isBlank())serviceStatus="Add a Gemini key or save an optional service URL first."
   else {testing=true;serviceStatus="Contacting AI…";scope.launch{
    val provider=if(directConfigured)directProvider else SecureAiGateway(context)
    provider.ask("Reply with one short sentence confirming CARPE AI is responding.","",emptyList()).fold(
     onSuccess={serviceStatus="AI responding: $it"},
     onFailure={serviceStatus="AI test failed: ${it.message ?: "Unknown error"}"})
    testing=false
   }}
  },enabled=!testing){Text(if(testing)"Testing…" else "Test AI")}
 }
 if(serviceStatus.isNotBlank())Text(serviceStatus,color=Orange)
 Text("Optional: connect a trusted CARPE AI service URL instead. This requires someone to deploy and operate that backend.",color=Color.DarkGray,fontSize=12.sp)
 OutlinedTextField(value=endpointInput,onValueChange={endpointInput=it},modifier=Modifier.fillMaxWidth(),label={Text("Optional CARPE AI service URL")},placeholder={Text("https://…/v1/ask")},singleLine=true)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Button(onClick={
  runCatching{SecureAiGateway.setEndpoint(context,endpointInput)}.fold(
   onSuccess={serviceTokenConfigured=SecureAiGateway.hasAccessToken(context,endpointInput);serviceStatus="Service URL saved. Tap Test AI to check the provider."},
    onFailure={serviceStatus=it.message ?: "Invalid service URL"})
  }){Text("Save URL")}
 if(SecureAiGateway.configuredEndpoint(context).isNotBlank())TextButton(onClick={SecureAiGateway.clearEndpoint(context);endpointInput="";serviceTokenConfigured=false;serviceStatus="Custom URL and its saved service token cleared."}){Text("Clear URL")}
 }
 Text("Private service access token",fontWeight=FontWeight.SemiBold)
 Text("Use the token generated for your trusted CARPE backend. It is encrypted on this device and sent only to the service URL above.",color=Muted,fontSize=12.sp)
 OutlinedTextField(value=serviceTokenInput,onValueChange={serviceTokenInput=it},modifier=Modifier.fillMaxWidth(),label={Text(if(serviceTokenConfigured)"Replace service token" else "Paste service token")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Button(onClick={runCatching{SecureAiGateway.saveAccessToken(context,endpointInput,serviceTokenInput)}.fold(onSuccess={serviceTokenConfigured=true;serviceTokenInput="";serviceStatus="Service access token saved on this device for this URL."},onFailure={serviceStatus=it.message ?: "Couldn't save service token."})},enabled=serviceTokenInput.isNotBlank()){Text("Save token")}
  if(serviceTokenConfigured)TextButton(onClick={SecureAiGateway.clearAccessToken(context);serviceTokenConfigured=false;serviceTokenInput="";serviceStatus="Service access token cleared."}){Text("Clear token")}
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("Build my AI profile");Text("Learn from what I tell CARPE. Stored locally and sent to Google only with my requests.",color=Color.DarkGray,fontSize=12.sp)};Switch(aiProfile,{aiProfile=it;profile.setEnabled(it)})}
 if(aiProfile){var profileText by remember{mutableStateOf(profile.summary())};Text("What CARPE remembers",fontWeight=FontWeight.Bold);Text(profileText,fontSize=13.sp);OutlinedButton(onClick={profile.clear();profileText=profile.summary()}){Text("Clear AI profile")}}
 HorizontalDivider()
 val goals=listOf("More time offline","Fitness & movement","Home cooking","Focused work","Saving money","Less compulsive content","Reduce porn use","Think across political viewpoints")
 goals.forEach{g->val k="goal_"+g.lowercase().replace(" ","_").replace("&","and");var on by remember{mutableStateOf(prefs.getBoolean(k,g=="More time offline"||g=="Focused work"))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(g,Modifier.weight(1f));Switch(on,{on=it;prefs.edit().putBoolean(k,it).apply()})}}
 Text("Sensitive goals are optional. CARPE does not monitor page content or infer what you view; these choices only shape suggestions you request.",color=Muted,fontSize=12.sp,lineHeight=17.sp)
 HorizontalDivider()
 Text("How CARPE learns",fontWeight=FontWeight.Bold,fontSize=20.sp)
 Text("Only feedback you tap on a suggestion is used. It stays on this device, and a few ratings have limited influence.",color=Muted,fontSize=13.sp,lineHeight=18.sp)
 val learning=remember(context){LearningStore(context)}
 var feedbackRevision by remember{mutableIntStateOf(0)}
 val feedbackSummary=remember(feedbackRevision){learning.feedbackSummary()}
 val trackedIntents=listOf(CarpeIntent.COOK to "Cooking",CarpeIntent.FOCUS to "Focus",CarpeIntent.MOVE to "Movement",CarpeIntent.SPEND to "Spending",CarpeIntent.OFFLINE_ACTIVITY to "Offline activity",CarpeIntent.REFLECT to "Reflection")
 var hasRecommendationFeedback=false
 trackedIntents.forEach{(intent,label)->
  val counts=feedbackSummary[intent] ?: IntentFeedback(0,0)
  if(counts.total>0){
   hasRecommendationFeedback=true
   Text("$label: ${counts.helpful} helpful of ${counts.total} ratings",color=Ink,fontSize=14.sp)
  }
 }
 if(!hasRecommendationFeedback)Text("No recommendation feedback saved yet.",color=Muted,fontSize=13.sp)
 if(hasRecommendationFeedback)OutlinedButton(onClick={learning.clearRecommendationFeedback();feedbackRevision++}){Text("Clear recommendation learning")}
 HorizontalDivider();Text(a.todayMinutes().toString()+" minutes invested in deliberate actions",fontSize=22.sp,fontWeight=FontWeight.Bold)
 a.recent(8).forEach{Text("• "+it.title+" — "+it.minutes+" min")}
 var confirmErase by remember{mutableStateOf(false)}
 OutlinedButton(onClick={confirmErase=true}){Text("Erase CARPE's local data")}
 if(confirmErase) AlertDialog(
  onDismissRequest={confirmErase=false},
  title={Text("Erase local data?")},
  text={Text("This removes your goals, ratings, action history, AI profile, service URL, and focus session from this device. Android permissions remain managed in system settings.")},
  confirmButton={TextButton(onClick={
   listOf("carpe","carpe_actions","carpe_ai_profile","carpe_ai_service","carpe_ai_service_access","behavior_history","notification_pressure","carpe_learning","carpe_focus","carpe_screen_audit","carpe_goals").forEach{name->
    context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit()
   }
   keyStore.clear()
   SecureAiGateway.clearAccessToken(context)
   confirmErase=false
   (context as? android.app.Activity)?.recreate()
  }){Text("Erase data")}},
  dismissButton={TextButton(onClick={confirmErase=false}){Text("Cancel")}}
 )
 Text("CARPE v"+BuildConfig.VERSION_NAME+" alpha",color=Orange,fontWeight=FontWeight.Bold)
}


/** The check-in is transient: never persisted, profiled, or sent to an AI provider. */
@Composable private fun WelcomeFlow(onFinish:(String)->Unit){
 var step by rememberSaveable{mutableIntStateOf(0)}
 var answer by remember{mutableStateOf("")}
 BackHandler(enabled=step>0){step--;answer=""}
 Scaffold(containerColor=WarmWhite){padding->
  key(step){
  Page(padding,
   when(step){0->"Welcome to CARPE";1->"A moment for you";else->"Start with what matters"},
   when(step){0->"Technology should serve your life.";1->"There is no right or wrong answer.";else->"One small starting point. You can change direction anytime."}){
   if(step==0){
    Text("CARPE is a counter-algorithm app built around your well-being. Our vision is to help you understand how technology shapes your attention, make deliberate choices, and create more room for the life you want.",fontSize=18.sp,lineHeight=26.sp)
    Card(colors=CardDefaults.cardColors(containerColor=Peach)){
     Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      Text("Your goals come first.",fontWeight=FontWeight.Bold,fontSize=20.sp)
      Text("Keep what helps. Change what gets in the way. You decide what a healthy balance looks like.")
      Text("No ads or data sales. Optional permissions. Useful without connecting AI.")
     }
    }
    Button(onClick={step=1},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Continue")}
   }else if(step==1){
    Text("Do you currently feel your relationship with technology is healthy?",fontSize=24.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
    Text("Think about whether technology supports your time, attention, relationships, and the things that matter to you.",color=Muted,lineHeight=22.sp)
    Text("Your answer is not saved or sent anywhere. It only shapes the message below.",color=Muted,fontSize=13.sp)
    listOf("Yes, it feels healthy","Sometimes — it is a mix","No, I would like it to change","I’m not sure yet").forEach{choice->
     OutlinedButton(onClick={answer=choice},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
      Text(if(answer==choice)"Selected: $choice" else choice)
     }
    }
    if(answer.isNotEmpty()){
     Card(colors=CardDefaults.cardColors(containerColor=Peach)){
      Text(when(answer){
       "Yes, it feels healthy"->"Let’s support what is working. You can use CARPE to protect that balance and pursue the goals you choose."
       "Sometimes — it is a mix"->"You can keep the parts that help and explore one part you would like to change, at your own pace."
       "No, I would like it to change"->"You can start small. CARPE offers focus breaks, local guidance, and goals you choose. You stay in control."
       else->"You do not need to decide now. You can explore CARPE and notice what feels helpful to you."
      },Modifier.padding(18.dp),lineHeight=23.sp)
     }
     Button(onClick={step=2},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Continue")}
    }
    TextButton(onClick={answer="";step=2},modifier=Modifier.fillMaxWidth()){Text("Skip this question")}
    TextButton(onClick={step=0;answer=""}){Text("Back")}
   }else{
    Text("What would you like CARPE to help you with first?",fontSize=24.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
    Text("Choose one, or explore on your own. We will ask for more detail only when it helps with what you are doing.",color=Muted,lineHeight=22.sp)
    ActionCard("Protect my focus","Open a focus timer for something I choose."){onFinish("focus")}
    ActionCard("Understand my technology use","Explore Mirror. Any device access is optional and explained before I choose it."){onFinish("mirror")}
    ActionCard("Make room for life offline","Draft a request for an activity that fits my time and energy. I choose whether to send it."){onFinish("offline")}
    ActionCard("Work toward my own goal","Choose or create a personal goal in Me."){onFinish("goals")}
    TextButton(onClick={onFinish("today")},modifier=Modifier.fillMaxWidth()){Text("Just explore CARPE")}
    TextButton(onClick={step=1;answer=""}){Text("Back")}
   }
  }
  }
 }
}
