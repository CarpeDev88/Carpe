package app.carpe.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** A deliberately small local summary. OCR text and captured images are never retained here. */
data class ScreenAuditReport(
    val sampledScreens: Int,
    val adLabelScreens: Int,
    val recommendationLabelScreens: Int,
    val continuePromptScreens: Int,
    val similarScreens: Int,
    val completedAt: Long,
    val sourceLabel: String = ""
) {
    fun percentOfSamples(cueScreens: Int): Int? {
        if (sampledScreens <= 0) return null
        val boundedCount = cueScreens.coerceIn(0, sampledScreens)
        return ((boundedCount * 100.0) / sampledScreens).roundToInt()
    }
}

data class AuditCueRate(val label: String, val earlierPercent: Int?, val latestPercent: Int?)

data class ScreenAuditComparison(val earlier: ScreenAuditReport, val latest: ScreenAuditReport) {
    fun cueRates(): List<AuditCueRate> = listOf(
        AuditCueRate("Ad or sponsored labels", earlier.percentOfSamples(earlier.adLabelScreens), latest.percentOfSamples(latest.adLabelScreens)),
        AuditCueRate("Recommendation labels", earlier.percentOfSamples(earlier.recommendationLabelScreens), latest.percentOfSamples(latest.recommendationLabelScreens)),
        AuditCueRate("Continue or autoplay prompts", earlier.percentOfSamples(earlier.continuePromptScreens), latest.percentOfSamples(latest.continuePromptScreens)),
        AuditCueRate("Similar visible text", earlier.percentOfSamples(earlier.similarScreens), latest.percentOfSamples(latest.similarScreens))
    )

    companion object {
        /** Compares only the two most recent samples with the same user-entered label. */
        fun between(reports: List<ScreenAuditReport>): ScreenAuditComparison? {
            if (reports.size < 2) return null
            val latest = reports.last()
            val earlier = reports.dropLast(1).lastOrNull {
                it.sourceLabel.trim().equals(latest.sourceLabel.trim(), ignoreCase = true)
            } ?: return null
            if (earlier.sampledScreens <= 0 || latest.sampledScreens <= 0) return null
            return ScreenAuditComparison(earlier, latest)
        }
    }
}

class ScreenAuditAnalyzer {
    private val wordsByScreen = mutableListOf<Set<String>>()
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
        if (words.isNotEmpty()) wordsByScreen.add(words)
    }

    fun report(
        completedAt: Long = System.currentTimeMillis(),
        sourceLabel: String = ""
    ) = ScreenAuditReport(
        sampledScreens = sampled,
        adLabelScreens = adLabels,
        recommendationLabelScreens = recommendationLabels,
        continuePromptScreens = continuePrompts,
        similarScreens = similarScreens,
        completedAt = completedAt,
        sourceLabel = sourceLabel.trim().take(60)
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
    fun setSourceLabel(value: String) = prefs.edit().putString(KEY_SOURCE_LABEL, value.trim().take(60)).apply()
    fun sourceLabel(): String = prefs.getString(KEY_SOURCE_LABEL, "").orEmpty()

    fun historyEnabled(): Boolean = prefs.getBoolean(KEY_HISTORY_ENABLED, false)
    fun setHistoryEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_HISTORY_ENABLED, enabled).apply() }

    fun save(report: ScreenAuditReport) {
        prefs.edit().putString(KEY_REPORT, report.toJson().toString()).apply()
    }

    /** Retains only aggregate counts, only after a completed session, and only after opt-in. */
    fun archive(report: ScreenAuditReport) {
        if (!historyEnabled() || report.sampledScreens <= 0) return
        val items = JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]")
        items.put(report.toJson())
        val bounded = JSONArray()
        for (index in (items.length() - MAX_HISTORY).coerceAtLeast(0) until items.length()) {
            bounded.put(items.getJSONObject(index))
        }
        prefs.edit().putString(KEY_HISTORY, bounded.toString()).apply()
    }

    fun history(): List<ScreenAuditReport> = runCatching {
        val items = JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]")
        (0 until items.length()).mapNotNull { index -> items.optJSONObject(index)?.toReport() }
    }.getOrDefault(emptyList())

    fun report(): ScreenAuditReport? = runCatching {
        JSONObject(prefs.getString(KEY_REPORT, null) ?: return null).toReport()
    }.getOrNull()

    fun clearReports() {
        prefs.edit().remove(KEY_REPORT).remove(KEY_HISTORY).apply()
    }

    private fun ScreenAuditReport.toJson() = JSONObject().apply {
        put("sampled", sampledScreens)
        put("ads", adLabelScreens)
        put("recommendations", recommendationLabelScreens)
        put("continue", continuePromptScreens)
        put("similar", similarScreens)
        put("completedAt", completedAt)
        put("sourceLabel", sourceLabel)
    }

    private fun JSONObject.toReport() = ScreenAuditReport(
        sampledScreens = optInt("sampled"),
        adLabelScreens = optInt("ads"),
        recommendationLabelScreens = optInt("recommendations"),
        continuePromptScreens = optInt("continue"),
        similarScreens = optInt("similar"),
        completedAt = optLong("completedAt"),
        sourceLabel = optString("sourceLabel")
    )

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_ERROR = "error"
        const val KEY_REPORT = "report"
        const val KEY_HISTORY = "history"
        const val KEY_HISTORY_ENABLED = "history_enabled"
        const val KEY_SOURCE_LABEL = "source_label"
        const val MAX_HISTORY = 5
    }
}
