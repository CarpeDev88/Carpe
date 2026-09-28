package app.carpe

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.carpe.core.*

@Composable
internal fun CoachCustomizationPanel(context: Context, initial: CoachChange, close: () -> Unit, changed: () -> Unit, explore: (Int) -> Unit) {
    val prefs = remember { context.getSharedPreferences("carpe", Context.MODE_PRIVATE) }
    val goals = remember { UserGoalStore(context) }
    var proposal by remember(initial) { mutableStateOf(initial) }
    var status by remember { mutableStateOf("") }
    var undo by remember { mutableStateOf<(() -> Unit)?>(null) }
    var goalTitle by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("3") }
    var minutes by remember { mutableStateOf(prefs.getInt("focus_minutes", 25).toString()) }
    var revision by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Make CARPE yours") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("I can adjust your goals, coaching priorities, and focus timer. These controls work on your device without AI. You approve each change.")
                if (status.isNotBlank()) Text(status)
                if (undo != null) TextButton(onClick = { undo?.invoke(); undo = null; revision++; changed(); status = "Change undone." }) { Text("Undo last change") }
                when (val p = proposal) {
                    CoachChange.Access -> {
                        Text("Usage Access lets Mirror summarize time in apps locally. Notification Access counts notifications locally. Screen sampling starts only when you request it and approve Android's prompt. Device observations are not sent to AI.")
                        Text("You can review or revoke access in Android settings. AI connection and optional profile sharing are separate choices in Me. CARPE remains useful without these permissions.")
                        TextButton(onClick = { close(); explore(3) }) { Text("Review device access in Mirror") }
                        TextButton(onClick = { close(); explore(4) }) { Text("Review AI connection and memory in Me") }
                        TextButton(onClick = { proposal = CoachChange.Help }) { Text("Back to customization") }
                    }
                    CoachChange.Help -> {
                        Text("Try saying: Set my focus to 15 minutes. Or: Add goal Walk after dinner 4 times a week.")
                        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Focus minutes (1–120)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        TextButton(onClick = { proposal = CoachChange.Focus(minutes.toInt()) }, enabled = minutes.toIntOrNull() in 1..120) { Text("Review focus change") }
                        OutlinedTextField(goalTitle, { goalTitle = it.take(80) }, label = { Text("Your goal") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(goalTarget, { goalTarget = it.filter(Char::isDigit).take(1) }, label = { Text("Times per week (1–7)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        TextButton(onClick = { proposal = CoachChange.Goal(goalTitle.trim(), goalTarget.toInt()) }, enabled = goalTitle.isNotBlank() && goalTarget.toIntOrNull() in 1..7) { Text("Review new goal") }
                        Text("Coaching priorities")
                        key(revision) {
                            CoachCustomization.priorities.forEach { title ->
                                val enabled = prefs.getBoolean(CoachCustomization.key(title), title == "More time offline" || title == "Focused work")
                                TextButton(onClick = { proposal = CoachChange.Priority(title, !enabled) }) { Text("${if(enabled) "On" else "Off"}: $title") }
                            }
                        }
                        TextButton(onClick = { proposal = CoachChange.Access }) { Text("Review optional access") }
                        TextButton(onClick = { close(); explore(4) }) { Text("Review or remove saved goals in Me") }
                    }
                    else -> {
                        Text(p.description)
                        Text("Saved only on this device. Nothing changes until you tap Apply.")
                        Button(onClick = {
                            when (p) {
                                is CoachChange.Focus -> {
                                    val previous = prefs.getInt("focus_minutes", 25)
                                    prefs.edit().putInt("focus_minutes", p.minutes).apply()
                                    undo = { prefs.edit().putInt("focus_minutes", previous).apply() }
                                    minutes = p.minutes.toString()
                                }
                                is CoachChange.Priority -> {
                                    val k = CoachCustomization.key(p.title)
                                    val previous = prefs.getBoolean(k, p.title == "More time offline" || p.title == "Focused work")
                                    prefs.edit().putBoolean(k, p.enabled).apply()
                                    undo = { prefs.edit().putBoolean(k, previous).apply() }
                                }
                                is CoachChange.Goal -> {
                                    val added = goals.addGoal(p.title, p.target)
                                    undo = added?.let { goal -> { goals.removeGoal(goal.id) } }
                                    goalTitle = ""
                                }
                                else -> Unit
                            }
                            status = "Applied. ${p.description}"
                            revision++; changed(); proposal = CoachChange.Help
                        }) { Text("Apply") }
                        TextButton(onClick = { proposal = CoachChange.Help; status = "No change made." }) { Text("Cancel change") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Done") } }
    )
}
