package app.carpe.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores a user-supplied AI Studio key encrypted with a device-bound Android Keystore key. */
class AiStudioKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("carpe_direct_ai", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = getKey() != null

    fun saveKey(value: String) {
        val clean = value.trim()
        require(clean.length >= 20) { "Enter a valid Gemini API key." }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        }
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun getKey(): String? = runCatching {
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val data = prefs.getString(KEY_DATA, null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
        }
        String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    fun clear() {
        prefs.edit().clear().apply()
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "carpe-gemini-user-key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_IV = "iv"
        const val KEY_DATA = "ciphertext"
    }
}

/** Direct, no-CARPE-server connection for a user's own AI Studio free-tier key. */
class DirectGeminiProvider(private val keyStore: AiStudioKeyStore) : CarpeAiProvider {
    override suspend fun ask(message: String, profile: String, history: List<AiTurn>): Result<String> =
        withContext(Dispatchers.IO) {
            val apiKey = keyStore.getKey()
                ?: return@withContext Result.failure(IllegalStateException("Save a Gemini API key in Me → AI & privacy first."))
            runCatching {
                val safeMessage = CloudDataPolicy.sanitize(CloudAiContext(userRequest = message)).userRequest
                val body = JSONObject().apply {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
                    put("contents", JSONArray().apply {
                        history.takeLast(10).forEach { turn ->
                            val text = turn.text.take(1500)
                            if (text.isNotBlank()) put(content(turn.role, text))
                        }
                        put(content("user", safeMessage))
                    })
                    put("generationConfig", JSONObject().put("temperature", 0.5).put("maxOutputTokens", 1200))
                }.also { request ->
                    // Keep optional profile context explicit and separate from raw device data.
                    if (profile.isNotBlank()) {
                        val prior = request.getJSONObject("systemInstruction").getJSONArray("parts")
                        prior.put(JSONObject().put("text", "User-enabled preferences:\n${profile.take(2500)}"))
                    }
                }.toString()

                val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", apiKey)
                }
                try {
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText().take(64_000) }.orEmpty()
                    if (status !in 200..299) {
                        val reason = when (status) {
                            400, 401, 403 -> "Google rejected the Gemini key or request. Check the key in AI Studio."
                            429 -> "Google's free-tier rate limit was reached. Wait for it to reset, then try again."
                            in 500..599 -> "Google Gemini is temporarily unavailable."
                            else -> "Google Gemini returned HTTP $status."
                        }
                        error(reason)
                    }
                    val response = JSONObject(raw)
                    val answer = response.optJSONArray("candidates")
                        ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                        ?.let { values -> (0 until values.length()).mapNotNull { values.optJSONObject(it)?.optString("text") }.joinToString("\n") }
                        .orEmpty().trim()
                    if (answer.isBlank()) {
                        val blocked = response.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
                        error(if (blocked.isNotBlank()) "Google blocked this prompt ($blocked). Try rephrasing it." else "Google returned no text. Try again.")
                    }
                    answer
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun content(role: String, text: String) = JSONObject()
        .put("role", if (role == "assistant" || role == "model") "model" else "user")
        .put("parts", JSONArray().put(JSONObject().put("text", text)))

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
        val SYSTEM_PROMPT = """
            You are CARPE, a user-first AI whose success is measured by whether technology helps the person live the life they deliberately choose, not by engagement.
            Protect autonomy, attention, privacy, time, money, relationships, and long-term goals. Prefer practical real-world action.
            Never manufacture urgency, guilt, streak pressure, outrage, compulsive checking, or dependence on CARPE. Do not advertise or optimize for purchases.
            Ask when the user's intent is genuinely ambiguous. For political subjects, be balanced and preserve independent judgment. Be concise, candid, and clear about uncertainty.
        """.trimIndent()
    }
}
