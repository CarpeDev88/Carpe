package app.carpe.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UserProfileStore(private val context: Context) {
 private val p=context.getSharedPreferences("carpe_ai_profile",Context.MODE_PRIVATE)
 fun enabled()=p.getBoolean("enabled",false)
 fun setEnabled(v:Boolean)=p.edit().putBoolean("enabled",v).apply()
 fun summary()=p.getString("summary","The user has not built a profile yet.") ?: ""
 fun learn(message:String){ if(!enabled()) return; val old=summary(); val clean=message.trim().take(500); if(clean.isNotBlank()&&!old.contains(clean,true)){ val next=if(old.startsWith("The user has not")) "User statements and intentions:\n• "+clean else old+"\n• "+clean; p.edit().putString("summary",next.takeLast(6000)).apply() } }
 fun clear()=p.edit().remove("summary").apply()
}

class GeminiClient(private val apiKey:String) {
 fun ask(message:String, profile:String):String {
  if(apiKey.isBlank()) return "Google AI is not configured yet. Add a Gemini API key in CARPE settings."
  val conn=(URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent").openConnection() as HttpURLConnection).apply{
   requestMethod="POST";doOutput=true;connectTimeout=15000;readTimeout=30000
   setRequestProperty("Content-Type","application/json");setRequestProperty("x-goog-api-key",apiKey)
  }
  val system="You are CARPE, a user-first counter-algorithm assistant. Optimize for the user's explicitly stated goals, autonomy, wellbeing, meaningful relationships, health, focused work, financial prudence, and deliberate technology use—not engagement with CARPE. Never invent facts about the user. Treat the profile as fallible context that the user can correct. Prefer useful actions that help the user leave the app when appropriate.\n\nLOCAL USER PROFILE:\n"+profile
  val body=JSONObject().put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",system)))).put("contents",JSONArray().put(JSONObject().put("role","user").put("parts",JSONArray().put(JSONObject().put("text",message))))).put("generationConfig",JSONObject().put("temperature",0.7).put("maxOutputTokens",700))
  conn.outputStream.use{it.write(body.toString().toByteArray())}
  val ok=conn.responseCode in 200..299; val stream=if(ok) conn.inputStream else conn.errorStream; val raw=stream.bufferedReader().use{it.readText()}
  if(!ok) return "Google AI request failed ("+conn.responseCode+")."
  val json=JSONObject(raw)
  return json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.takeIf{it.isNotBlank()} ?: "Google AI returned no response."
 }
}
