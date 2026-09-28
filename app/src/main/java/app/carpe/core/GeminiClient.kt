package app.carpe.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UserProfileStore(private val context: Context) {
 private val p=context.getSharedPreferences("carpe_ai_profile",Context.MODE_PRIVATE)
 fun enabled()=p.getBoolean("enabled",false)
 fun setEnabled(v:Boolean)=p.edit().putBoolean("enabled",v).apply()
 fun summary()=p.getString("summary","The user has not built a profile yet.") ?: ""
 fun learn(message:String){
  if(!enabled()) return
  val clean=message.trim().replace("\n"," ").take(300)
  val durable=listOf("my goal","i want to","i prefer","i like","i don't like","i do not like","remember that","important to me","i'm trying to","i am trying to")
  if(clean.length<8 || durable.none{clean.contains(it,true)}) return
  val old=summary()
  if(old.contains(clean,true)) return
  val items=if(old.startsWith("The user has not")) emptyList() else old.lines().filter{it.startsWith("• ")}
  val next=(items+"• "+clean).takeLast(20).joinToString("\n")
  p.edit().putString("summary","User-chosen goals and preferences:\n"+next).apply()
 }
 fun clear()=p.edit().remove("summary").apply()
}

data class AiTurn(val role:String,val text:String)

interface CarpeAiProvider {
 suspend fun ask(message:String, profile:String, history:List<AiTurn>):Result<String>
}

/**
 * Cloud provider endpoint is intentionally supplied at build time and contains no
 * model credential. The endpoint is expected to be a CARPE-owned HTTPS proxy
 * (Cloud Run / Firebase Function) which keeps provider credentials server-side.
 */
class SecureAiGateway(private val endpoint:String = app.carpe.BuildConfig.CARPE_AI_ENDPOINT):CarpeAiProvider {
 override suspend fun ask(message:String, profile:String, history:List<AiTurn>):Result<String> = withContext(Dispatchers.IO) {
  if(endpoint.isBlank()) return@withContext Result.failure(IllegalStateException(
   "CARPE AI is not configured in this build yet. The app is working, but its secure AI service URL has not been installed."
  ))
  runCatching {
   val body=JSONObject().apply {
    put("message",CloudDataPolicy.sanitize(CloudAiContext(userRequest=message)).userRequest)
    put("profile",profile.take(2500))
    put("history",org.json.JSONArray().apply {
     history.takeLast(10).forEach { put(JSONObject().put("role",it.role).put("text",it.text.take(1500))) }
    })
    put("purpose","Help the user advance their explicitly chosen goals while protecting autonomy, attention, privacy, money and time. Prefer useful real-world action over engagement. Ask when intent is uncertain.")
   }.toString()

   val c=(URL(endpoint).openConnection() as HttpURLConnection).apply {
    requestMethod="POST"; connectTimeout=12_000; readTimeout=30_000
    doOutput=true; setRequestProperty("Content-Type","application/json")
   }
   c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
   val code=c.responseCode
   val raw=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
   if(code !in 200..299) error("AI service returned HTTP $code")
   val json=JSONObject(raw)
   json.optString("response").ifBlank{json.optString("text")}.ifBlank{error("AI service returned no response")}
  }
 }
}
