package app.carpe.core

sealed class CoachChange(val description: String) {
    data class Focus(val minutes: Int) : CoachChange("Set future focus sessions to $minutes minutes. A running session will not change.")
    data class Priority(val title: String, val enabled: Boolean) : CoachChange("${if (enabled) "Include" else "Remove"} $title in local coaching priorities.")
    data class Goal(val title: String, val target: Int) : CoachChange("Add goal: $title — $target times per week.")
    object Help : CoachChange("Choose what you want to customize.")
    object Access : CoachChange("Review optional access. Nothing is enabled automatically.")
}

/** Only explicit user commands become proposals. AI responses never reach this parser. */
object CoachCustomization {
    val priorities = listOf("More time offline", "Fitness & movement", "Home cooking", "Focused work", "Saving money", "Less compulsive content", "Reduce porn use", "Think across political viewpoints")
    fun key(title: String) = "goal_" + title.lowercase(java.util.Locale.ROOT).replace(" ", "_").replace("&", "and")
    fun parse(input: String): CoachChange? {
        val text = input.trim().removeSuffix(".")
        Regex("(?:set (?:my )?focus(?: timer| sessions?)? to|make (?:my )?focus sessions?) (\\d{1,3}) minutes?", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            val minutes = it.groupValues[1].toInt()
            return if (minutes in 1..120) CoachChange.Focus(minutes) else CoachChange.Help
        }
        Regex("(?:add (?:a )?goal:?|my goal is) (.+?)(?: (\\d+) times? (?:per|a) week)?", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            val title = it.groupValues[1].trim()
            val target = it.groupValues[2].takeIf(String::isNotEmpty)?.toIntOrNull() ?: if(it.groupValues[2].isEmpty()) 3 else 0
            return if (title.length in 1..80 && target in 1..7) CoachChange.Goal(title, target) else CoachChange.Help
        }
        Regex("(enable|disable|turn on|turn off) (.+)", RegexOption.IGNORE_CASE).matchEntire(text)?.let {
            val title = priorities.firstOrNull { p -> p.equals(it.groupValues[2], true) } ?: return CoachChange.Help
            return CoachChange.Priority(title, it.groupValues[1].lowercase(java.util.Locale.ROOT) in listOf("enable", "turn on"))
        }
        if (text.lowercase(java.util.Locale.ROOT) in listOf("review access", "manage permissions", "give you more access", "show permissions")) return CoachChange.Access
        if (Regex(".*\\b(customiz(?:e|ation)|settings|permissions)\\b.*", RegexOption.IGNORE_CASE).matches(text) || text.startsWith("set my focus", true) || text.startsWith("add goal", true)) return CoachChange.Help
        return null
    }
}
