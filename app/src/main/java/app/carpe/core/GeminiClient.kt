package app.carpe.core

import android.content.Context

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

/**
 * CARPE deliberately does not embed or accept a raw Gemini Developer API key.
 * Cloud AI will be re-enabled through Firebase AI Logic + App Check so credentials
 * are not stored in the APK or SharedPreferences.
 */
class SecureAiGateway {
 fun ask(message:String, profile:String):String =
  "Secure cloud AI is being upgraded. CARPE kept your request local. " +
  "You can still use Focus, Shield, goals, and local recommendations."
}
