package app.carpe.core

import android.content.Context

/** Keeps a focus session alive when the user switches tabs or reopens the app. */
class FocusSessionStore(context: Context) {
 private val prefs=context.getSharedPreferences("carpe_focus",Context.MODE_PRIVATE)
 private val key="ends_at"

 fun isActive():Boolean=prefs.contains(key)
 fun remainingMillis():Long=(prefs.getLong(key,0L)-System.currentTimeMillis()).coerceAtLeast(0L)
 fun start(minutes:Int){prefs.edit().putLong(key,System.currentTimeMillis()+minutes*60_000L).apply()}
 fun stop(){prefs.edit().remove(key).apply()}

 fun finishIfDue(actions:ActionStore):Boolean {
  if(!isActive()||remainingMillis()>0L)return false
  stop()
  actions.add("focus","Completed focus session",25)
  return true
 }
}
