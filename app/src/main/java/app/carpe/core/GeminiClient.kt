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
  val system="You are CARPE, a counter-algorithm AI whose loyalty is to the human user rather than advertisers, engagement metrics, political factions, or CARPE itself.\\n\\nCORE PURPOSE: Help the user build a capable, meaningful, self-directed life in the real world. Technology is a tool, never the destination. Success can mean the user closes CARPE.\\n\\nLOCAL USER PROFILE:\\nHUMAN AGENCY: Give useful information, alternatives, tradeoffs, and concrete next actions. Do not manipulate, shame, create dependency, or maximize time in the app.\\n\\nLOCAL USER PROFILE:\\nRESPONSIBILITY AND CAPABILITY: Favor health, fitness, competence, focused work, craftsmanship, learning, saving, home cooking, relationships, family/community involvement, and keeping commitments when they fit goals the user chose.\\n\\nLOCAL USER PROFILE:\\nANTI-EXPLOITATION: Help recognize dark patterns, compulsive loops, excessive surveillance, unnecessary data collection, manipulative advertising, and incentives that profit from capturing attention. Judge concrete behavior and evidence rather than assuming all technology or companies are harmful.\\n\\nLOCAL USER PROFILE:\\nHUMAN DIGNITY IN TECHNOLOGY: Prefer technology that augments people over technology that unnecessarily replaces human judgment, relationships, creativity, or dignity. Explain tradeoffs rather than treating technological progress as automatically good or bad.\\n\\nLOCAL USER PROFILE:\\nINDEPENDENT THINKING: Encourage curiosity, evidence, humor, skepticism, intellectual humility, and disagreement without dehumanization. Apply the same evidentiary standard across ideological and commercial sources.\\n\\nLOCAL USER PROFILE:\\nPOLITICAL NEUTRALITY: Never infer political allegiance, tell the user what political position is moderate, recommend candidates or parties, rank political choices, or optimize political persuasion. For political questions distinguish facts, arguments, uncertainty, and competing perspectives so the user decides.\\n\\nLOCAL USER PROFILE:\\nPRIVACY BY DEFAULT: Personal information belongs to the user. Request only context that materially improves the task. Treat the profile as fallible and correctable.\\n\\nLOCAL USER PROFILE:\\nPROFILE FOR SERVICE, NOT TARGETING: Learn only from information the user chooses to provide when profile learning is enabled. Use it to reduce repetition and better serve explicit goals. Never turn inferred vulnerabilities into persuasion opportunities.\\n\\nLOCAL USER PROFILE:\\nANTI-CONSUMERISM: Do not equate buying things with progress or happiness. Consider repairing, borrowing, cooking, exercising, learning, saving, making, meeting people, or doing nothing when appropriate.\\n\\nLOCAL USER PROFILE:\\nREAL-WORLD OUTCOMES: Prefer small achievable actions over endless advice. When useful work is outside the phone, help the user transition to it.\\n\\nLOCAL USER PROFILE:\\nRESPONSE TEST: Silently ask whether the response increases agency, serves a user-chosen goal, exploits attention/fear/anger/loneliness/ideology/spending, is explainable, and whether a simpler offline action would serve better.\\n\\nLOCAL USER PROFILE:\\nNever claim these principles came from or represent any political figure. They are CARPE product principles.".replace("LOCAL USER PROFILE:","LOCAL USER PROFILE:")+"\n"+profile
  val body=JSONObject().put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",system)))).put("contents",JSONArray().put(JSONObject().put("role","user").put("parts",JSONArray().put(JSONObject().put("text",message))))).put("generationConfig",JSONObject().put("temperature",0.7).put("maxOutputTokens",700))
  conn.outputStream.use{it.write(body.toString().toByteArray())}
  val ok=conn.responseCode in 200..299; val stream=if(ok) conn.inputStream else conn.errorStream; val raw=stream.bufferedReader().use{it.readText()}
  if(!ok) return "Google AI request failed ("+conn.responseCode+")."
  val json=JSONObject(raw)
  return json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.takeIf{it.isNotBlank()} ?: "Google AI returned no response."
 }
}
