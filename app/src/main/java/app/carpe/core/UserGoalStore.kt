package app.carpe.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserGoal(
    val id: String,
    val title: String,
    val weeklyTarget: Int,
    val createdAt: Long
)

data class GoalCheckIn(
    val goalId: String,
    val completedAt: Long
)

object GoalProgress {
    fun countWithinDays(checkIns: List<GoalCheckIn>, days: Int, now: Long): Int {
        val windowMillis = days.coerceAtLeast(1).toLong() * 24L * 60L * 60L * 1000L
        val start = now - windowMillis
        return checkIns.count { it.completedAt in start..now }
    }
}

object GoalStepPlanner {
    fun prompt(goalTitle: String): String {
        val clean = goalTitle.replace('\n', ' ').replace('\r', ' ').trim().take(80)
        return "Help me choose one small, realistic next step toward this goal: $clean. Keep it practical and let me decide whether to do it."
    }
}

/** Stores user-authored goals and check-ins on this device. No streaks, reminders, or cloud sync. */
class UserGoalStore(context: Context) {
    private val prefs = context.getSharedPreferences("carpe_goals", Context.MODE_PRIVATE)

    fun goals(): List<UserGoal> = runCatching {
        val items = JSONArray(prefs.getString(KEY_GOALS, "[]") ?: "[]")
        (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.toGoal()
        }
    }.getOrDefault(emptyList())

    fun checkIns(goalId: String): List<GoalCheckIn> = runCatching {
        val items = JSONArray(prefs.getString(KEY_CHECK_INS, "[]") ?: "[]")
        (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.toCheckIn()
        }.filter { it.goalId == goalId }
    }.getOrDefault(emptyList())

    fun addGoal(title: String, weeklyTarget: Int): UserGoal? {
        val cleanTitle = title.replace('|', ' ').replace('\n', ' ').trim().take(MAX_TITLE_LENGTH)
        if (cleanTitle.isBlank()) return null
        val goal = UserGoal(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            weeklyTarget = weeklyTarget.coerceIn(1, 7),
            createdAt = System.currentTimeMillis()
        )
        val updated = goals() + goal
        prefs.edit().putString(KEY_GOALS, updated.goalsToJson().toString()).apply()
        return goal
    }

    fun checkIn(goalId: String, at: Long = System.currentTimeMillis()) {
        if (goals().none { it.id == goalId }) return
        val updated = (allCheckIns() + GoalCheckIn(goalId, at)).takeLast(MAX_CHECK_INS)
        prefs.edit().putString(KEY_CHECK_INS, updated.checkInsToJson().toString()).apply()
    }

    fun removeGoal(goalId: String) {
        prefs.edit()
            .putString(KEY_GOALS, goals().filterNot { it.id == goalId }.goalsToJson().toString())
            .putString(KEY_CHECK_INS, allCheckIns().filterNot { it.goalId == goalId }.checkInsToJson().toString())
            .apply()
    }

    private fun allCheckIns(): List<GoalCheckIn> = runCatching {
        val items = JSONArray(prefs.getString(KEY_CHECK_INS, "[]") ?: "[]")
        (0 until items.length()).mapNotNull { index -> items.optJSONObject(index)?.toCheckIn() }
    }.getOrDefault(emptyList())

    private fun List<UserGoal>.goalsToJson() = JSONArray().also { array ->
        forEach { goal ->
            array.put(JSONObject().apply {
                put("id", goal.id)
                put("title", goal.title)
                put("weeklyTarget", goal.weeklyTarget)
                put("createdAt", goal.createdAt)
            })
        }
    }

    private fun List<GoalCheckIn>.checkInsToJson() = JSONArray().also { array ->
        forEach { checkIn ->
            array.put(JSONObject().apply {
                put("goalId", checkIn.goalId)
                put("completedAt", checkIn.completedAt)
            })
        }
    }

    private fun JSONObject.toGoal() = UserGoal(
        id = optString("id"),
        title = optString("title"),
        weeklyTarget = optInt("weeklyTarget", 3).coerceIn(1, 7),
        createdAt = optLong("createdAt")
    ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }

    private fun JSONObject.toCheckIn() = GoalCheckIn(
        goalId = optString("goalId"),
        completedAt = optLong("completedAt")
    ).takeIf { it.goalId.isNotBlank() }

    private companion object {
        const val KEY_GOALS = "goals"
        const val KEY_CHECK_INS = "check_ins"
        const val MAX_TITLE_LENGTH = 80
        const val MAX_CHECK_INS = 1000
    }
}
