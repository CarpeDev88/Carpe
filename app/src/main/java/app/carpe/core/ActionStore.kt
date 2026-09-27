package app.carpe.core

import android.content.Context

data class CarpeAction(
    val type: String,
    val title: String,
    val minutes: Int,
    val completedAt: Long = System.currentTimeMillis()
)

class ActionStore(context: Context) {
    private val prefs = context.getSharedPreferences("carpe_actions", Context.MODE_PRIVATE)

    fun add(type: String, title: String, minutes: Int) {
        val old = prefs.getString("log", "") ?: ""
        val safeTitle = title.replace("|", " ")
        val row = listOf(System.currentTimeMillis(), type, safeTitle, minutes).joinToString("|")
        val updated = if (old.isBlank()) row else row + "\n" + old
        prefs.edit().putString("log", updated).apply()
    }

    fun recent(limit: Int = 30): List<CarpeAction> {
        val raw = prefs.getString("log", "") ?: ""
        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 4) {
                    null
                } else {
                    CarpeAction(
                        type = parts[1],
                        title = parts[2],
                        minutes = parts[3].toIntOrNull() ?: 0,
                        completedAt = parts[0].toLongOrNull() ?: 0L
                    )
                }
            }
            .take(limit)
    }

    fun todayMinutes(): Int {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        return recent(100)
            .filter { it.completedAt >= cutoff }
            .sumOf { it.minutes }
    }
}
