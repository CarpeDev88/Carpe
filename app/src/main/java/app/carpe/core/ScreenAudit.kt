package app.carpe.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A deliberately small local summary. OCR text and captured images are never retained here. */
data class ScreenAuditReport(
    val sampledScreens: Int,
    val adLabelScreens: Int,
    val recommendationLabelScreens: Int,
    val continuePromptScreens: Int,
    val similarScreens: Int,
    val recurringWords: List<String>,
    val completedAt: Long
)

class ScreenAuditAnalyzer {
    private val wordsByScreen = mutableListOf<Set<String>>()
    private val recurringWordCounts = mutableMapOf<String, Int>()
    private var sampled = 0
    private var adLabels = 0
    private var recommendationLabels = 0
    private var continuePrompts = 0
    private var similarScreens = 0

    fun observe(ocrText: String) {
        val text = ocrText.lowercase().replace(Regex("\\s+"), " ").trim()
        if (text.length < 12) return
        sampled++
        if (AD_LABEL.containsMatchIn(text)) adLabels++
        if (RECOMMENDATION_LABEL.containsMatchIn(text)) recommendationLabels++
        if (CONTINUE_PROMPT.containsMatchIn(text)) continuePrompts++

        val words = WORD.findAll(text).map { it.value }
            .filter { it.length >= 5 && it !in STOP_WORDS }
            .toSet()
        if (wordsByScreen.any { previous -> similarity(words, previous) >= 0.72 }) similarScreens++
        if (words.isNotEmpty()) {
            wordsByScreen.add(words)
            words.forEach { recurringWordCounts[it] = (recurringWordCounts[it] ?: 0) + 1 }
        }
    }

    fun report(completedAt: Long = System.currentTimeMillis()) = ScreenAuditReport(
        sampledScreens = sampled,
        adLabelScreens = adLabels,
        recommendationLabelScreens = recommendationLabels,
        continuePromptScreens = continuePrompts,
        similarScreens = similarScreens,
        recurringWords = recurringWordCounts.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key },
        completedAt = completedAt
    )

    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.union(b).size
    }

    private companion object {
        val WORD = Regex("[a-z][a-z0-9']{3,}")
        val AD_LABEL = Regex("\\b(sponsored|promoted|advertisement|paid partnership)\\b|\\bad\\b")
        val RECOMMENDATION_LABEL = Regex("\\b(suggested for you|recommended for you|because you watched|because you liked|you might like|for you)\\b")
        val CONTINUE_PROMPT = Regex("\\b(up next|watch next|autoplay|keep scrolling|keep watching)\\b")
        val STOP_WORDS = setOf(
            "about", "after", "again", "also", "because", "before", "being", "could", "every", "first", "from", "going", "have", "here", "into", "just", "like", "more", "most", "much", "other", "over", "people", "please", "really", "should", "some", "than", "that", "their", "there", "these", "thing", "think", "this", "those", "through", "today", "under", "until", "very", "want", "watch", "what", "when", "where", "which", "while", "will", "with", "would", "your", "follow", "comment", "share", "search", "profile", "settings", "create", "video", "videos", "home", "music", "more"
        )
    }
}

/** Stores only session state and the user's latest aggregate report on-device. */
class ScreenAuditStore(context: Context) {
    private val prefs = context.getSharedPreferences("carpe_screen_audit", Context.MODE_PRIVATE)

    fun setActive(active: Boolean) = prefs.edit().putBoolean(KEY_ACTIVE, active).apply()
    fun isActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)
    fun setError(error: String?) = prefs.edit().putString(KEY_ERROR, error.orEmpty()).apply()
    fun error(): String = prefs.getString(KEY_ERROR, "").orEmpty()

    fun save(report: ScreenAuditReport) {
        val value = JSONObject().apply {
            put("sampled", report.sampledScreens)
            put("ads", report.adLabelScreens)
            put("recommendations", report.recommendationLabelScreens)
            put("continue", report.continuePromptScreens)
            put("similar", report.similarScreens)
            put("words", JSONArray(report.recurringWords))
            put("completedAt", report.completedAt)
        }
        prefs.edit().putString(KEY_REPORT, value.toString()).apply()
    }

    fun report(): ScreenAuditReport? = runCatching {
        val value = JSONObject(prefs.getString(KEY_REPORT, null) ?: return null)
        val words = value.optJSONArray("words") ?: JSONArray()
        ScreenAuditReport(
            sampledScreens = value.optInt("sampled"),
            adLabelScreens = value.optInt("ads"),
            recommendationLabelScreens = value.optInt("recommendations"),
            continuePromptScreens = value.optInt("continue"),
            similarScreens = value.optInt("similar"),
            recurringWords = (0 until words.length()).mapNotNull { index -> words.optString(index).takeIf { it.isNotBlank() } },
            completedAt = value.optLong("completedAt")
        )
    }.getOrNull()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_ERROR = "error"
        const val KEY_REPORT = "report"
    }
}
