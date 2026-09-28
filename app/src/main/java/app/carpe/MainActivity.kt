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
@Composable private fun Page(p:PaddingValues,title:String,s�v��$z{-���jםlligence",fontWeight=FontWeight.Bold)
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
