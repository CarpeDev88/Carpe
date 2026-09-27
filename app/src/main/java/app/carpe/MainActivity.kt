package app.carpe

import android.os.Bundle
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFF5F1E8)
private val Ink = Color(0xFF17201B)
private val Green = Color(0xFF355E48)
private val SoftGreen = Color(0xFFDDE7DD)

data class Intention(val title: String, val detail: String, val icon: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarpeApp() }
    }
}

@Composable
fun CarpeApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("carpe", Context.MODE_PRIVATE) }
    var selected by remember { mutableIntStateOf(0) }
    var reclaimed by remember { mutableIntStateOf(prefs.getInt("reclaimed", 0)) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Cream, surface = Color.White)) {
        Scaffold(
            containerColor = Cream,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf("Today" to Icons.Outlined.WbSunny, "Intentions" to Icons.Outlined.CheckCircle, "Shield" to Icons.Outlined.Shield, "Me" to Icons.Outlined.Person).forEachIndexed { i, item ->
                        NavigationBarItem(selected = selected == i, onClick = { selected = i }, icon = { Icon(item.second, item.first) }, label = { Text(item.first) })
                    }
                }
            }
        ) { padding ->
            when (selected) {
                0 -> TodayScreen(padding, reclaimed) { reclaimed += 5; prefs.edit().putInt("reclaimed", reclaimed).apply() }
                1 -> IntentionsScreen(padding, prefs)
                2 -> ShieldScreen(padding, context)
                else -> MeScreen(padding, reclaimed)
            }
        }
    }
}

@Composable
private fun Page(padding: PaddingValues, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("CARPE", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text(title, color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Ink.copy(alpha=.65f), fontSize = 17.sp, lineHeight = 24.sp)
        content()
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun TodayScreen(p: PaddingValues, reclaimed: Int, add: () -> Unit) = Page(p, "Own your attention.", "Technology should help you build a life you want to live — then get out of the way.") {
    Card(colors = CardDefaults.cardColors(containerColor = Green), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(22.dp)) {
            Text("TIME RECLAIMED TODAY", color = Color.White.copy(alpha=.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$reclaimed min", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text("Small departures from the loop add up.", color = Color.White.copy(alpha=.8f))
        }
    }
    Text("Choose what happens next", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    listOf(
        Intention("Cook something", "Trade scrolling for making a meal.", "cook"),
        Intention("Move your body", "Walk, train, stretch — your choice.", "move"),
        Intention("Do meaningful work", "Protect 25 minutes for something that matters.", "work"),
        Intention("Spend deliberately", "Pause before an impulse purchase.", "save")
    ).forEach { item ->
        ElevatedCard(onClick = add, colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(when(item.icon){"cook"->Icons.Outlined.Restaurant;"move"->Icons.Outlined.DirectionsWalk;"work"->Icons.Outlined.Work;else->Icons.Outlined.Savings}, null, tint=Green)
                Spacer(Modifier.width(15.dp))
                Column { Text(item.title, fontWeight=FontWeight.Bold); Text(item.detail, color=Ink.copy(alpha=.6f), fontSize=14.sp) }
            }
        }
    }
    Button(onClick = add, modifier=Modifier.fillMaxWidth().height(54.dp)) { Text("I’m leaving the loop") }
}

@Composable
private fun IntentionsScreen(p: PaddingValues, prefs: android.content.SharedPreferences) = Page(p, "Your intentions", "Carpe optimizes for what you choose, not what keeps you engaged.") {
    listOf("More time offline", "Fitness & movement", "Home cooking", "Focused work", "Saving money", "Less compulsive content").forEach {
        val key = "goal_" + it.lowercase().replace(" ", "_").replace("&", "and")
        var on by remember { mutableStateOf(prefs.getBoolean(key, it == "More time offline" || it == "Focused work")) }
        Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment=Alignment.CenterVertically) {
                Text(it, Modifier.weight(1f), fontWeight=FontWeight.Medium)
                Switch(checked=on, onCheckedChange={ value -> on=value; prefs.edit().putBoolean(key, value).apply() })
            }
        }
    }
    Text("These settings stay on this device in this prototype.", color=Ink.copy(alpha=.55f), fontSize=13.sp)
}

@Composable
private fun ShieldScreen(p: PaddingValues, context: Context) = Page(p, "Algorithm shield", "Reduce the signals that make attention-harvesting systems effective. You decide what Carpe can access.") {
    Card(colors=CardDefaults.cardColors(containerColor=SoftGreen), shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Icon(Icons.Outlined.Lock, null, tint=Green)
            Spacer(Modifier.height(10.dp))
            Text("Minimum permission by default", fontWeight=FontWeight.Bold, fontSize=19.sp)
            Text("Carpe remains useful without optional access. Extra capabilities unlock only when you explicitly choose them.", color=Ink.copy(alpha=.7f))
        }
    }
    val usage = remember { app.carpe.core.UsageAccess(context) }
    var granted by remember { mutableStateOf(usage.isGranted()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = usage.isGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Card(onClick = { if (!granted) context.startActivity(usage.settingsIntent()) }, colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Usage insights", Modifier.weight(1f), fontWeight=FontWeight.Bold)
                Text(if (granted) "On" else "Off — tap to enable", color=Green)
            }
            Text(if (granted) "Carpe can analyze recent app usage locally." else "Optional. Android will ask you to approve Usage Access.", color=Ink.copy(alpha=.55f), fontSize=14.sp)
            if (granted) {
                Spacer(Modifier.height(10.dp))
                val top = usage.last24Hours().take(5)
                top.forEach { Text("• ${it.packageName.substringAfterLast('.')} — ${it.foregroundMinutes} min", fontSize=13.sp) }
            }
        }
    }
    PermissionRow("Notification filtering", "Off", "Can reduce attention traps")
    PermissionRow("Website protection", "Off", "Future opt-in content controls")
    PermissionRow("Health & activity", "Off", "Future opt-in wellbeing context")
    Text("No permission is required to use the core app.", fontWeight=FontWeight.Bold, color=Green)
}

@Composable
private fun PermissionRow(name:String, status:String, detail:String) {
    Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth()) { Text(name, Modifier.weight(1f), fontWeight=FontWeight.Bold); Text(status, color=Green) }
            Text(detail, color=Ink.copy(alpha=.55f), fontSize=14.sp)
        }
    }
}

@Composable
private fun MeScreen(p: PaddingValues, reclaimed:Int) = Page(p, "Your life, not a feed", "Progress is measured by time and attention returned to you — not engagement with Carpe.") {
    Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(22.dp)) {
            Text("TODAY", color=Green, fontWeight=FontWeight.Bold, fontSize=12.sp)
            Text("$reclaimed minutes reclaimed", fontSize=24.sp, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress={ (reclaimed/60f).coerceAtMost(1f) }, modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Daily intention: 60 minutes", color=Ink.copy(alpha=.6f))
        }
    }
    Text("Carpe has no infinite feed, no streak punishment, and no ads.", color=Ink.copy(alpha=.7f))
}
