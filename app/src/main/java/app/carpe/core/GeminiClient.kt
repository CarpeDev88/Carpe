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
 * Cloud provider endpoint contains no model credential. It may be supplied at build
 * time or set by the user on the device. It must point to a trusted HTTPS proxy
 * (Cloud Run / Firebase Function) which keeps provider credentials server-side.
 */
class SecureAiGateway(private val context:Context):CarpeAiProvider {
 companion object {
  private const val PREFS="carpe_ai_service"
  private const val ENDPOINT="endpoint"
  fun validEndpoint(value:String):Boolean = runCatching {
   val url=URL(value.trim())
   url.protocol=="https" && !url.host.isNullOrBlank() && url.userInfo==null &&
    url.query==null && url.ref==null && url.path=="/v1/ask"
  }.getOrDefault(false)
  fun configuredEndpoint(context:Context):String =
   context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(ENDPOINT,"")
    ?.takeIf(::validEndpoint) ?: app.carpe.BuildConfig.CARPE_AI_ENDPOINT.takeIf(::validEndpoint).orEmpty()
  fun setEndpoint(context:Context,value:String) {
   require(validEndpoint(value)){"Enter an HTTPS service URL ending in /v1/ask."}
   context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(ENDPOINT,value.trim()).apply()
  }
  fun clearEndpoint(context:Context) = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(ENDPOINT).apply()
 }
 override suspend fun ask(message:String, profile:String, history:List<AiTurn>):Result<String> = withContext(Dispatchers.IO) {
  val endpoint=configuredEndpoint(context)
  if(endpoint.isBlank()) return@withContext Result.failure(IllegalStateException(
   "CARPE AI needs a service URL. Open Me → AI & privacy to connect it."
  ))
  runCatching {
   val context=CloudDataPolicy.forUserRequest(message,profile)
   val body=JSONObject().apply {
    put("message",context.userRequest)
    put("profile",context.userChosenPreferences.joinToString("\n").take(2500))
    // Keep conversation history on-device; do not send past turns to cloud AI.
    put("history",org.json.JSONArray())
    put("purpose","Help the user advance their explicitly chosen goals while protecting autonomy, attention, privacy, money and time. Prefer useful real-world action over engagement. Ask when intent is uncertain.")
   }.toString()

   val c=(URL(endpoint).openConnection() as HttpURLConnection).apply {
    requestMethod="POST"; connectTimeout=12_000; readTimeout=30_000
    doOutput=true; setRequestProperty("Content-Type","application/json")
   }
   c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
   val code=c.responseCode
   val raw=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
   if(code !in 200..299) {
    val serviceError=runCatching{JSONObject(raw).optString("error")}.getOrDefault("")
    val explanation=when(code){
     401,403->"The AI service rejected the request. Check its app access settings."
     404->"The AI service URL was not found. Check that it ends in /v1/ask."
     429->"The AI service is busy or has reached its request limit."
     502,503->if(serviceError=="AI provider is not configured") "The service is missing its AI provider key."
      else "The service could not reach its AI provider. Check its provider key and quota."
     else->"AI service returned HTTP $code"
    }
    error(explanation)
   }
   val json=JSONObject(raw)
   json.optString("response").ifBlank{json.optString("text")}.ifBlank{error("AI service returned no response")}
  }
 }
}
